/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import fresco.swing.CWorkerDialogFactory;
import image.CBinaryImage;
import image.converters.Crgb2grey;
import image.statiscics.CHistogram;
import info.clearthought.layout.TableLayout;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import support.CSupportWorker;
import workers.analyse.CCannyEdgeDetector;
import workers.registration.CPointPairs;

/**
 * Multimodal genetic algorithms-based algorithm for automatic point correspondence
 * authors Konstantinos Delibasis, Pantelis A. Asvestas, George K. Matsopoulos
 *
 * For details see article mentioned above.
 *
 * In a nut shell:
 * <ol>
 * <li>From image I<sub>r</sub> randomly select RefPoints and rigid transformation
 * parameters. (x, y, d<sub>x</sub>, d<sub>y</sub>, theta, scale)</li>
 * <li>Use GA for searching best individuals. Condition is cross-corelation of
 * nearest neighborhood</li>
 * </ol>
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CRefPointMarker extends CSupportWorker<CPointPairs, CPointAndTransformation[]> {

	private static final Logger logger = Logger.getLogger(CRefPointMarker.class.getName());
	/** Number of evolution generations. In each generation one offspring is generated. */
	private static int GENERATIONS_DEFAULT = 10000;
	/** Evolution is stopped when this part of population succeed on threshold fitness */
	private static double FITNESS_THRESHOLD_DEFAULT = 0.95;
	/** Ranges for initial values */
	/** centered around 0 */
	static final double ANGLE_RANGE = Math.PI / 10;
	/** centered around 0 */
	static final double DXY_RANGE = 40;
	/** centered around 1 */
	static final double SCALE_RANGE = 0.1;
	/** mutation factor (experimentally set) */
	static final double MUTATION_FACTOR = 0.125;
	/** subpopulation size for competition */
	static final double RTS_SIZE = 0.9;
	/** Correlation of point pairs. Radius define range of counted correlation around center pixel */
	static final int RADIUS_DEFAULT = 25;
	/** Percentage of edge pixels in image (top boundary) */
	static final int EDGE_PIXELS_PERCENT = 4;
	/** size of population of marks for genetic algorithm */
	private final int populationSize;
	/** Set of points and their rigid transformation params */
	private CPointAndTransformation[] population;
	/** Best points (cross variation higher than POP_FRACTION_THRESHOLD) */
	private LinkedList<CPointAndTransformation> bestOf = new LinkedList<CPointAndTransformation>();
	/** Best fit - highest cross variation value */
	private double bestFit, averageFitness;
	/** Images being searched */
	private final BufferedImage source, reference;
	/** Nearest edge for each pixel stored here */
	private CNearestEdgeMatrix nem;
	/** Bounds of source */
	private final int width, height;
	/** Evolution generations */
	private int generations = GENERATIONS_DEFAULT;
	/** Fitness threshold. If an offspring reaches value above this threshold than will be published in output population */
	private double fitnessThreshold = FITNESS_THRESHOLD_DEFAULT;
	/** Similarity measure radius */
	private int radius = RADIUS_DEFAULT;

	public CRefPointMarker(BufferedImage source, BufferedImage reference, int populationSize) {
		this.source = source;
		width = source.getWidth();
		height = source.getHeight();
		this.reference = reference;
		this.populationSize = populationSize;
		population = new CPointAndTransformation[populationSize];
	}

	@Override
	public String getWorkerName() {
		return "Automatic search for correspondig points";
	}

	@Override
	protected CPointPairs doInBackground() throws Exception {
		// create referring picture map (Canny detector and thresholding)
		long start = System.currentTimeMillis();
		CBinaryImage bw = createBinaryImage();
		logger.log(Level.FINE, "Binary image created: {0}ms", (System.currentTimeMillis()-start));
		// compute nearest edge matrix
		start = System.currentTimeMillis();
		nem = new CNearestEdgeMatrix(bw);
		logger.log(Level.FINE, "Nearest edge matrix done: {0}ms", (System.currentTimeMillis()-start));
		// initialize points
		createPopulation();
		logger.log(Level.FINE, "Population created: {0}ms", (System.currentTimeMillis()-start));
		//dump(population);

		// initialize evolution
		CPointEvolution evolution = new CPointEvolution(generations, population, nem);

		// evolve individuals
		while (evolution.oneGeneration()) {
			logger.log(Level.FINEST, "Generation {0}", evolution.getGenerations());
			if (evolution.getGenerations() % 5000 == 0) {
				dump(population);
			}
			storeBetterThanThreshold();
			if (bestOf.size() > population.length) {
				population = bestOf.toArray(population);
				publish(population);
				setProgress(100);
				break;
			}
			publish(population);
			setProgress(100 / 6 + 500 * evolution.getGenerations() / generations / 6);
		}

		if (bestOf.size() < populationSize && bestOf.size() > 4) {
			population = new CPointAndTransformation[bestOf.size()];
			population = bestOf.toArray(population);
		}

		return removeDuplicites();
	}

	/**
	 * Algorithm for development of matrix of nearest edge pixels
	 * @return binary image with edges of original image
	 * @throws InterruptedException it is a SwingWorker (thread exceptions)
	 * @throws ExecutionException
	 */
	private CBinaryImage createBinaryImage() throws InterruptedException, ExecutionException {
		CCannyEdgeDetector ced = new CCannyEdgeDetector(source);

		ced.execute();

		synchronized (this) {
			while (ced.getState() != StateValue.DONE) {
				setProgress(ced.getProgress() / 6);
				wait(100);
			}
		}

		WritableRaster cannyResult = ced.get().getRaster();

		return new CBinaryImage(cannyResult, countThreshold(cannyResult, EDGE_PIXELS_PERCENT));
	}

	private int countThreshold(Raster raster, int percentOfEdgePixels) {
		double[][] greyImage = Crgb2grey.convertImage(raster);
		int[] greyHist = CHistogram.getMonochromeHistogram(greyImage, 256);
		int sum = 0;
		int size = greyImage.length * greyImage[0].length;

		for(int i=greyHist.length - 1; i>=0; i--) {
			sum += greyHist[i];
			if (sum > size * percentOfEdgePixels / 100)
				return i;
		}

		return 0;
	}


	/** Defines initial position of each point in population. This version uses
	 * square mesh with Gaussian noise. Transformation parameters are generated
	 * as identity with Gaussian noise.
	 */
	private void createPopulation() {
		CCrossCorrelationFitness fitnesEvaluator = new CCrossCorrelationFitness(source, reference, radius);


		for (int i = 0; i < populationSize; i++) {
			population[i] = CPointAndTransformationFactory.createArticleBased(width, height, fitnesEvaluator);
			population[i].alignToEdge(nem);
		}
	}

	private CPointPairs removeDuplicites() {
		CPointPairs pairs = new CPointPairs();

		for (int i = 0; i < population.length; i++) {
			if (!pairs.contains(population[i].getPosition(), null)) {
				pairs.addPointPair(population[i].getPosition(), population[i].getProjection());
			}
		}

		return pairs;
	}

	private void dump(CPointAndTransformation[] data) {
		StringBuilder sb = new StringBuilder("Point coords: ");
		for (int i = 0; i < data.length; i++) {
			sb.append("[");
			sb.append(data[i].getPosition().x);
			sb.append(", ");
			sb.append(data[i].getPosition().y);
			sb.append(']');
		}
		logger.info(sb.toString());
		logger.log(Level.INFO, "Best of size: {0} Best fit: {1}. Average: {2}", new Object[]{bestOf.size(), bestFit, averageFitness});
	}

	private void storeBetterThanThreshold() {
		averageFitness = 0;

		for (int i = 0; i < population.length; i++) {
			if (population[i].getFitness() > fitnessThreshold && !bestOf.contains(population[i])) {

				bestOf.add(population[i]);
			}
			bestFit = Math.max(population[i].getFitness(), bestFit);
			averageFitness += population[i].getFitness();
		}

		averageFitness /= population.length;
	}


	/**
	 * Text input of number of generations (evolved by algorithm). Accepted
	 * values are integers greater than zero. Useful values are up to 10^6
	 */
	JTextField gensInput;
	/**
	 * Similarity measure is defined only on small area. This area can vary due
	 * to resolution of input image. For high resolution images I recommend
	 * under sampling.
	 */
	JTextField measuredAreaInput;
	/**
	 * Different size of compared areas can cause variation of absolute value
	 * of similarity measure. This should be handled here.
	 * TODO: For some measures can be precomputed and automatically set
	 */
	JTextField fitnessThresholdInput;

	@Override
	public boolean confirmDialog() {
		try {
			generations = Integer.valueOf(gensInput.getText());
			if (generations < 1) {
				logger.log(Level.WARNING, "Value is lower than zero. Using default value: {0}", GENERATIONS_DEFAULT);
				generations = GENERATIONS_DEFAULT;
			}
		} catch (NumberFormatException nfe) {
			logger.log(Level.WARNING, "Value generations is not a number! Using default value: {0}", GENERATIONS_DEFAULT);
		}

		try {
			radius = Integer.valueOf(measuredAreaInput.getText());
			if (radius < 1) {
				logger.log(Level.WARNING, "Value is lower than zero. Using default value: {0}", RADIUS_DEFAULT);
				radius = RADIUS_DEFAULT;
			}
		} catch (NumberFormatException nfe) {
			logger.log(Level.WARNING, "Value for measured area input is not a integer number! Using default value: {0}", RADIUS_DEFAULT);
		}

		try {
			fitnessThreshold = Double.valueOf(fitnessThresholdInput.getText());
		} catch (NumberFormatException nfe) {
			logger.log(Level.WARNING, "Value for fitness threshold is not a real number! Using default value: {0}", FITNESS_THRESHOLD_DEFAULT);
		}

		return true;
	}

	@Override
	public JDialog getParamSettingDialog() {
		logger.info("No params are necessary.");

		JPanel content = new JPanel();
		TableLayout layout = new TableLayout (new double[]{0.6, 0.4},
				new double[]{TableLayout.FILL, TableLayout.FILL, TableLayout.FILL});


		layout.setHGap(5);
		layout.setVGap(5);
		content.setLayout(layout);

		// number of generations
		JLabel generationsLab = new JLabel("Number of generations: ");
		content.add(generationsLab, "0, 0");
		if (gensInput == null) {
			gensInput = new JTextField(""+generations);
		}
		content.add(gensInput, "1, 0");

		// size of compared area
		JLabel similarityRadius = new JLabel("Radius of compared area: ");
		content.add(similarityRadius, "0, 1");
		if (measuredAreaInput == null) {
			measuredAreaInput = new JTextField(""+radius);
		}
		content.add(measuredAreaInput, "1, 1");

		// threashold of similarity
		JLabel similarityThreshold = new JLabel("Fitness threshold: ");
		content.add(similarityThreshold, "0, 2");
		if (fitnessThresholdInput == null) {
			fitnessThresholdInput = new JTextField(""+fitnessThreshold);
		}
		content.add(fitnessThresholdInput, "1, 2");

		JDialog dialog = CWorkerDialogFactory.createOkCancelDialog(this, content);

		return dialog;
	}
}
