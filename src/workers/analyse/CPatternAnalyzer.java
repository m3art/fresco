/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import fresco.CImageContainer;
import image.converters.Crgb2grey;
import image.converters.IConstants;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * This algorithm compares patterns in different images. For this purpose uses
 * small samples (neural networks) which adapts on pattern in the image. Trained
 * NN are then compared and difference is shown as output.
 * @author Honza Blazek
 */
public class CPatternAnalyzer extends CAnalysisWorker {

	private static double MAX = 1.0, MIN = 0.0;
	private static int GRAY_LEVELS = 256;
	/** Number of samples - small NN */
	private final int sampleNo;
	/** Width and height of one sample in pixels - should be odd number */
	private final int sampleSize;
	/** Learning iterations - for NN training */
	private final int iterations;
	/** Progress abs value*/
	private int progress;
	/** Matching set of samples */
	protected Sample[] sample;
	/** Learning gradient */
	protected double alpha;
	/** Image inputs */
	private final BufferedImage inputA, inputB;
	/** Size of images */
	private Dimension size;
	/** Double values for input images */
	private double[][] meshA, meshB, outputMesh, sampleErr;
	/** Support pattern - optimization */
	double[] pattern;
	/** logger */
	private static final Logger logger = Logger.getLogger(CPatternAnalyzer.class.getName());

	public CPatternAnalyzer(CImageContainer inputA, CImageContainer inputB) {
		this.sampleNo = 15;
		this.sampleSize = 7;
		this.iterations = 1000;
		this.inputA = inputA.getImage();
		this.inputB = inputB.getImage();
	}

	public CPatternAnalyzer(CImageContainer inputA, CImageContainer inputB, int sampleNo, int sampleSize, int iters) {
		this.sampleNo = sampleNo;
		this.sampleSize = sampleSize;
		this.iterations = iters;
		this.inputA = inputA.getImage();
		this.inputB = inputB.getImage();
	}

	public CPatternAnalyzer(BufferedImage inputA, BufferedImage inputB, int sampleNo, int sampleSize, int iters) {
		this.sampleNo = sampleNo;
		this.sampleSize = sampleSize;
		this.iterations = iters;
		this.inputA = inputA;
		this.inputB = inputB;
	}

	protected boolean init() {
		if (inputA.getWidth() != inputB.getWidth() || inputA.getHeight() != inputB.getHeight()) {
			return false;
		}

		size = new Dimension(inputA.getWidth(), inputB.getHeight());
		progress = size.width * size.height + iterations;

		// convert images into double mesh
		meshA = convertImageToMesh(inputA);
		meshB = convertImageToMesh(inputB);

		// alloc space for output matrix
		outputMesh = new double[inputA.getWidth()][inputB.getHeight()];
		pattern = new double[sampleSize * sampleSize];
		sampleErr = new double[sampleNo][sampleNo];

		for (int x = 0; x < sampleNo; x++) {
			for (int y = 0; y < sampleNo; y++) {
				if (x != y) {
					sampleErr[x][y] = -1;
				}
			}
		}

		// initialize samples
		alpha = 1.0;
		sample = new Sample[sampleNo];
		for (int s = 0; s < sampleNo; s++) {
			sample[s] = new Sample(sampleSize);
			randomPattern(meshA);
			sample[s].learn(pattern);
		}

		logger.info("Pattern analyzer inited");
		return true;
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (!init()) {
			JOptionPane.showMessageDialog(new JFrame(), "Input images must have the same size!\n"
					+ "Possible solution:\n"
					+ "1) Resize images in external software\n"
					+ "2) Use Transform > Perspective transformation for correct transform.", "Image difference stopped", JOptionPane.WARNING_MESSAGE);
			return null;
		}

		// learn samples
		logger.info("Learning samples ...");
		for (int i = 0; i < iterations; i++) {
			alpha = 0.3 / i;
			randomPattern(meshA);
			sample[selectBestSample(pattern)].learn(pattern);
			setProgress(iterations * 100 / progress);
		}

		int sA, sB;
		double maxErr = 0;

		logger.info("Counting output values ...");
		for (int x = 0; x < size.width - sampleSize; x++) {
			for (int y = 0; y < size.height - sampleSize; y++) {
				// select best representation of pattern from imageA
				setSampleAtIntoPattern(meshA, x, y);
				sA = selectBestSample(pattern);
				// select best representation of pattern from imageB
				setSampleAtIntoPattern(meshB, x, y);
				sB = selectBestSample(pattern);
				// count error of two samples if is not stored yet and store it into outputMash for concrete pixel
				if (sampleErr[sA][sB] != -1) {
					outputMesh[x][y] = sampleErr[sA][sB];
				} else {
					outputMesh[x][y] = sampleErr[sA][sB] = sample[sA].getValue(sample[sB].value);
					if (maxErr < sampleErr[sA][sB]) {
						maxErr = sampleErr[sA][sB];
					}
				}
				setProgress((iterations + x * size.height + y) * 100 / progress);
			}
		}

		for (int x = 0; x < sampleNo; x++) {
			for (int y = 0; y < sampleNo; y++) {
				System.out.print(sampleErr[x][y] + " ");
			}
			System.out.println();
		}

		logger.info("Converting result to image");
		return convertMeshToImage(outputMesh, 0, maxErr);
	}

	/**
	 * From simple double[][] mash creates gray image which scales values in
	 * mash (interval [MIN, MAX]) in GRAY_LEVELS values
	 * @param mash input double matrix, values should be between [MIN,MAX]
	 * @return created image based on input matrix
	 */
	protected BufferedImage convertMeshToImage(double[][] mash, double min, double max) {
		BufferedImage out = new BufferedImage(size.width, size.height, BufferedImage.TYPE_3BYTE_BGR);
		int x, y, rgb;
		int gray;

		for (x = 0; x < size.width; x++) {
			for (y = 0; y < size.height; y++) {
				gray = 0xff & (int) ((mash[x][y] - min) / max * GRAY_LEVELS);
				rgb = (((gray << 8) | gray) << 8) | gray;
				out.setRGB(x, y, rgb);
			}
		}

		return out;
	}

	/**
	 * From RGB image creates double mesh with values between [MIN, MAX]
	 * @param input input image to convert
	 * @return double mash with converted values
	 */
	protected static double[][] convertImageToMesh(BufferedImage input) {
		double[][] out = new double[input.getWidth()][input.getHeight()];

		Raster raster = input.getData();
		int[] gray = new int[IConstants.rgb_bands], rgb = new int[IConstants.rgb_bands];

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				raster.getPixel(x, y, rgb);
				gray = Crgb2grey.convert(rgb);

				out[x][y] = ((double) (gray[0])) / ((double) GRAY_LEVELS * MAX + MIN);
			}
		}

		return out;
	}

	/**
	 * From input @param mesh select randomly squere of size sampleSize and
	 * put it into pattern field
	 */
	protected void randomPattern(double[][] mesh) {
		int x = (int) (Math.random() * (size.width - sampleSize)),
				y = (int) (Math.random() * (size.height - sampleSize));

		setSampleAtIntoPattern(mesh, x, y);
	}

	/**
	 * From specified coords put squere (left top corner at [x,y]) into pattern
	 * field
	 * @param mesh input mesh of values
	 * @param x width coord
	 * @param y height coord
	 */
	protected void setSampleAtIntoPattern(double[][] mesh, int x, int y) {
		for (int i = 0; i < sampleSize; i++) {
			for (int j = 0; j < sampleSize; j++) {
				pattern[sampleSize * i + j] = mesh[x + i][y + j];
			}
		}
	}

	/**
	 * For given pattern returns sample with lowest error
	 * @param pattern input pattern
	 * @return index of best sample
	 */
	protected int selectBestSample(double[] pattern) {
		int best = 0;
		double bestErr = Double.MAX_VALUE, err;
		for (int s = 0; s < sampleNo; s++) {
			err = sample[s].getValue(pattern);
			if (err < bestErr) {
				best = s;
				bestErr = err;
			}
		}
		return best;
	}

	/** Simple NN */
	protected class Sample {

		/** Intensity values pixel by pixel, converted in 1D array */
		private double[] value;

		/** random initialization of sample */
		public Sample(int sampleSize) {
			value = new double[sampleSize * sampleSize];
			for (int i = 0; i < value.length; i++) {
				value[i] = (Math.random() * (MAX - MIN)) + MIN;
			}
		}

		/**
		 * Sample is learned by patterns on which has the best response.
		 * Learning shift value vector near to pattern vector
		 * @param pattern learned vector
		 */
		public void learn(double[] pattern) {
			for (int i = 0; i < value.length; i++) {
				value[i] += alpha * (pattern[i] - value[i]);
			}
		}

		/** Returns response for input pattern. Input pattern represent squere
		 * of size sampleSize x sampleSize resampled to 1D
		 * @param pattern values converted to interval [MIN, MAX]
		 * @return difference of pattern and sample in sense of squere error
		 */
		public double getValue(double[] pattern) {
			double out = 0;
			for (int i = 0; i < value.length; i++) {
				out += Math.pow(pattern[i] - value[i], 2);
			}

			return out;
		}
	}

	@Override
	public String getWorkerName() {
		return "Pattern analyzer";
	}
}
