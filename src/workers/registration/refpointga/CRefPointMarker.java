/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import image.CBinaryImage;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	/** Number of generations is mentioned in article, experimental result */
	private static int GENERATIONS = 100000;
	/** Evolution is stopped when this part of population succeed on threshold fitness */
	private static double FITNESS_THRESHOLD = 0.95;
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
	/** Correlation of point pairs. Radius define range of counted correlation */
	static final int RADIUS = 25;
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
		CBinaryImage bw = createBinaryImage();
		logger.fine("Binary image created");
		// compute nearest edge matrix
		nem = new CNearestEdgeMatrix(bw);
		logger.fine("Nearest edge matrix done");
		// initialize points
		createPopulation();
		logger.fine("Population created");
		dump(population);

		// initialize evolution
		CPointEvolution evolution = new CPointEvolution(GENERATIONS, population, nem);

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
			setProgress(100 / 6 + 500 * evolution.getGenerations() / GENERATIONS / 6);
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


		return new CBinaryImage(cannyResult, 180);
	}

	/** Defines initial position of each point in population. This version uses
	 * square mesh with Gaussian noise. Transformation parameters are generated
	 * as identity with Gaussian noise.
	 */
	private void createPopulation() {
		CCrossCorrelationFitness fitnesEvaluator = new CCrossCorrelationFitness(source, reference, RADIUS);


		for (int i = 0; i < populationSize; i++) {
			population[i] = CPointAndTransformationFactory.createArticleBased(width, height, fitnesEvaluator);
			population[i].alignToEdge(nem);
		}
	}

	private CPointPairs removeDuplicites() {
		CPointPairs pairs = new CPointPairs();

		for (int i = 0; i < population.length; i++) {
			if (!pairs.contains(population[i].getPosition(), null)) {
				pairs.addPointPair(population[i].getPosition(), population[i].getIntProjection());
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
			if (population[i].getFitness() > FITNESS_THRESHOLD && !bestOf.contains(population[i])) {

				bestOf.add(population[i]);
			}
			bestFit = Math.max(population[i].getFitness(), bestFit);
			averageFitness += population[i].getFitness();
		}

		averageFitness /= population.length;
	}
}
