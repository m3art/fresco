/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

import fresco.swing.CWorkerDialogFactory;
import image.converters.CBufferedImageToDoubleArray;
import image.converters.Crgb2hsv;
import image.statistics.CHistogram;
import info.clearthought.layout.TableLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class enhance the input image contrast. Image is splited to regions of
 * size region size and for each is histogram counted, this histogram is
 * normalized and all pixels between regions centers are bilineary aproximated
 * by the normalized histograms.
 *
 * Works well...
 *
 * @author Gimli
 */
public class CAdaptiveHistogramEnhancing extends CCorrectionWorker {

	/** Range specifies distance between maximal and minimal output value (in one contrast segment) */
	public int histCut = 3, s_max = 5;
	/** Size of reference region size */
	private int regionSize = 150;
	private int[][] grayScaleImage;
	/** Image width and height */
	private int width, height;
	/** Enhanced colour band - intensity in HSV model */
	private int band = 2;
	BufferedImage image;
	private double[][][] pixels;
	private static final Logger logger = Logger.getLogger(CAdaptiveHistogramEnhancing.class.getName());
	boolean rgb;
	private int HISTOGRAM_BINS = 256;

	public CAdaptiveHistogramEnhancing(BufferedImage image) {
		this.image = image;
		rgb = true;
	}

	/**
	 * Constructor
	 * @param image defines input image
	 * @param band defines color to improve the contrast
	 * @param regionSize defines size of referencing region
	 */
	public CAdaptiveHistogramEnhancing(double[][][] image, int region_size) {
		pixels = image;
		this.regionSize = region_size;
	}

	private void init() {
		long start = System.currentTimeMillis();
		if (rgb) {
			pixels = Crgb2hsv.convertImageToDouble(image);
			logger.log(Level.FINE, "Image to double array conversion: {0}", (System.currentTimeMillis()-start));
			start = System.currentTimeMillis();
		} else {
			pixels = CBufferedImageToDoubleArray.convert(image);
		}

		width = image.getWidth();
		height = image.getHeight();

		grayScaleImage = new int[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				grayScaleImage[x][y] = (int) (pixels[x][y][band]);
			}
		}
		logger.log(Level.FINE, "Intensity matrix: {0}", (System.currentTimeMillis()-start));
	}

	/**
	 * Main class to give result for the image
	 * @return changed image
	 */
	public double[][][] AHE() {
		double[][] out = new double[width][height];
		int k = 0, // index for reference histograms ordering
				neighs;
		// current position in image
		Point pos = new Point(-regionSize, -regionSize);
		/** Four buffered histograms necessary for value interpolation */
		int[][] histogram = {null, null, null, null};
		/** Region contains set of pixels which will be interpolated by the same set of histograms */
		int[] region,
				currentOrder,
				verse = {0, 3, 2, 1}, // order of histograms used (even line)
				reverse = {0, 1, 2, 3}; // order of histograms used (odd line)

		while (pos.x < width || pos.y < height) {
			for (int i = 0; i < 2; i++) {
				region = getRegion(pos.x, pos.y);        // overwrite region
				k = setHistNumber(pos.x, pos.y);        // set changed region number
				histogram[k] = developHistogram(region);       // create first histogram
				pos.y += regionSize;
			}
			pos.x += regionSize;

			// from is set at the las pixel of read regions
			for (int x = Math.max(pos.x - 3 * regionSize / 2, 0); x < Math.min(pos.x - regionSize / 2, width); x++) {
				for (int y = Math.max(pos.y - 3 * regionSize / 2, 0); y < Math.min(pos.y - regionSize / 2, height); y++) {
					out[x][y] = 0;
					neighs = 0;
					if (k % 2 == 0) {
						currentOrder = verse;
					} else {
						currentOrder = reverse;
					}
					if (histogram[(k + currentOrder[1]) % 4] != null) {
						out[x][y] += (pos.x - regionSize / 2 - x) * (pos.y - regionSize / 2 - y)
								* histogram[(k + currentOrder[1]) % 4][grayScaleImage[x][y]];
						neighs += (pos.x - regionSize / 2 - x) * (pos.y - regionSize / 2 - y);
					}
					if (histogram[(k + currentOrder[2]) % 4] != null) {
						out[x][y] += (pos.x - regionSize / 2 - x) * (y - pos.y + 3 * regionSize / 2)
								* histogram[(k + currentOrder[2]) % 4][grayScaleImage[x][y]];
						neighs += (pos.x - regionSize / 2 - x) * (y - pos.y + 3 * regionSize / 2);
					}
					if (histogram[(k + currentOrder[3]) % 4] != null) {
						out[x][y] += (x - pos.x + 3 * regionSize / 2) * (pos.y - regionSize / 2 - y)
								* histogram[(k + currentOrder[3]) % 4][grayScaleImage[x][y]];
						neighs += (x - pos.x + 3 * regionSize / 2) * (pos.y - regionSize / 2 - y);
					}
					if (histogram[(k + currentOrder[0]) % 4] != null) {
						out[x][y] += (x - pos.x + 3 * regionSize / 2) * (y - pos.y + 3 * regionSize / 2)
								* histogram[(k + currentOrder[0]) % 4][grayScaleImage[x][y]];
						neighs += (x - pos.x + 3 * regionSize / 2) * (y - pos.y + 3 * regionSize / 2);
					}
					if (neighs != 0) {
						pixels[x][y][band] = out[x][y] / neighs;
					} else {
						pixels[x][y][band] = 0;
					}
				}
			}
			if (pos.x < width + regionSize / 2) {
				pos.y -= 2 * regionSize;
			} else if (pos.y < height + regionSize / 2) {
				pos.x = -regionSize;
				pos.y -= regionSize;
				histogram[0] = histogram[1] = histogram[2] = histogram[3] = null;
			}
		}
		return pixels;
	}

	/**
	 * Creates array of values from gray scale image
	 * @param minX left begin index in gray scale image
	 * @param minY top begin index in gray scale image
	 * @return copied values in linear array
	 */
	private int[] getRegion(int minX, int minY) {
		int maxX, maxY;
		int[] outRegion;
		// check if it is necessary to set the region
		if (minX >= width || minY >= height || minX < 0 || minY < 0) {
			logger.finer("Region out of bounds skipped");
			return null;
		} else {
			// set region boundaries
			maxX = Math.min(minX + regionSize, width);
			maxY = Math.min(minY + regionSize, height);
			// overwrite the region
			outRegion = new int[(maxX - minX) * (maxY - minY)];
			for (int x = minX; x < maxX; x++) {
				for (int y = minY; y < maxY; y++) {
					outRegion[(x - minX) * (maxY - minY) + (y - minY)] = grayScaleImage[x][y];
				}
			}
		}
		return outRegion;
	}

	private int setHistNumber(int from_x, int from_y) {
		int k = 0;
		// define one of four used histograms which will be overwritten - k is his number
		if (Math.abs((from_x / regionSize) % 2) == 1) {
			k += 2;
		}
		if (Math.abs((from_y / regionSize) % 2) == 1) {
			k += 1;
		}

		return k;
	}

	private int[] developHistogram(int[] region) {
		int[] hist;
		hist = CHistogram.oneBandHistogram(region, HISTOGRAM_BINS);
		hist = normalizeHistogram(hist);

		return hist;
	}

	private int[] normalizeHistogram(int[] hist) {
		if (hist == null) {
			return null;
		}
		int i, sum = 0, min = 0, residuum = 0;

		for (i = 0; i < hist.length; i++) {
			if (hist[i] > 0 && min == 0) {
				min = hist[i];
			}
			if (histCut > hist[i]) {
				sum += hist[i];
			} else {
				sum += histCut;
				residuum += hist[i] - histCut;
			}
			hist[i] = sum;
		}

		logger.log(Level.FINE, "Residuum: {0}", residuum/hist.length);


		int max = hist[HISTOGRAM_BINS-1] + residuum;

		for (i = 0; i < HISTOGRAM_BINS; i++) {
			if (hist[i] != 0) {
				hist[i] = (int) Math.round(hist[i] - min + residuum * i / HISTOGRAM_BINS);
			} else {
				hist[i] = residuum * i / HISTOGRAM_BINS;
			}
			hist[i] *= 255;
			hist[i] /= max;
		}



		return hist;
	}

	@Override
	public boolean hasDialog() {
		return true;
	}

	JTextField regSize;
	JSlider effect = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);

	/**
	 * Evaluate values set by user. Default Worker does not support user input.
	 * In this case is call of this method illegal and
	 * @throws UnsupportedOperationException is thrown.
	 */
	@Override
	public boolean confirmDialog() {
		try {
			this.regionSize = Integer.valueOf(regSize.getText());
			this.histCut = image.getWidth() * image.getHeight() * effect.getValue() / 100 / HISTOGRAM_BINS;
			return true;
		} catch (NumberFormatException nfe) {
			logger.warning("Input value for region size is not a valid integer.");
			return false;
		}
	}

	/**
	 * Default user interface to any user. No dialog is necessary (nothing is
	 * shown). If you want user input in your worker rewrite this method.
	 * @return null
	 */
	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();
		JPanel inputs = new JPanel();

		JLabel regSizeLabel = new JLabel("Region size:", SwingConstants.RIGHT);
		regSize = new JTextField(Integer.toString((int)(Math.sqrt(image.getWidth()*image.getHeight())/4)),6);
		JLabel effectLabel = new JLabel("Effect:", SwingConstants.RIGHT);

		effect.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				effect.setToolTipText(effect.getValue()+"%");
			}
		});

		TableLayout layout = new TableLayout(new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5},new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5});

		inputs.setLayout(layout);
		inputs.add(regSizeLabel, "1, 1");
		inputs.add(regSize, "3, 1");
		inputs.add(effectLabel, "1, 3");
		inputs.add(effect, "3, 3");
		inputs.setPreferredSize(new Dimension(300, 50));

		content.setLayout(new BorderLayout(5,5));
		content.add(new JLabel("<html><body><b>Params for adaptive histogram enhancing:</b><p align=\"justify\"> Small region generates higher contrast on small <br />area, big regions suppress noise much better.</p></body></html>"), BorderLayout.NORTH);
		content.add(inputs, BorderLayout.CENTER);

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}

	@Override
	public String getWorkerName() {
		return "Adaptive histogram enhancement";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		long start = System.currentTimeMillis();
		init();
		logger.log(Level.FINE, "Initialization done: {0}ms", (System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		double[][][] output = AHE();
		logger.log(Level.FINE, "Adaptive histogram enhancment done: {0}ms", (System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		if (rgb) {
			output = Crgb2hsv.inverse(output);
			logger.log(Level.FINE, "HSV -> RGB conversion: {0}ms", (System.currentTimeMillis()-start));
			start = System.currentTimeMillis();
		}

		BufferedImage outImage = CBufferedImageToDoubleArray.inverse(output);
		logger.log(Level.FINE, "ImageCreation: {0}ms", (System.currentTimeMillis()-start));

		return outImage;
	}
}
