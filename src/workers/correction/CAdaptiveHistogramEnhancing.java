/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

import fresco.swing.CContrastEnhancementDialog;
import image.converters.CBufferedImageToDoubleArray;
import java.awt.image.BufferedImage;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;
import javax.swing.JFrame;
import image.statiscics.CHistogram;

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
	public int range = 255, s_max = 5;
	/** Size of reference region size */
	private int region_size = 150;
	private int[][] gray_scale;
	private int width, height, band;
	BufferedImage image;
	private double[][][] pixels;
	private static final Logger logger = Logger.getLogger(CAdaptiveHistogramEnhancing.class.getName());
	double alpha = 3;
	double beta = (range * (1 + alpha / 100 * (s_max - 1)) / (region_size * region_size));

	public CAdaptiveHistogramEnhancing(BufferedImage image) {
		CContrastEnhancementDialog dialog = new CContrastEnhancementDialog(new JFrame());
		dialog.setVisible(true);
		CAHEParams params = dialog.get();

		if (params == null) {
			return;
		}

		this.image = image;

		this.region_size = params.region_size;
		band = 0; //FIXME: should be in menu, fix algorithm for range value
		this.range = params.effect * 255 / 100;
	}

	/**
	 * Constructor
	 * @param image defines input image
	 * @param band defines color to improve the contrast
	 * @param region_size defines size of referencing region
	 */
	public CAdaptiveHistogramEnhancing(double[][][] image, int band, int region_size) {
		int x, y;
		this.band = band;
		gray_scale = new int[width][height];
		this.region_size = region_size;
		beta = (range * (1 + alpha / 100 * (s_max - 1)) / (region_size * region_size));
		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				gray_scale[x][y] = (int) (range * image[x][y][band]);
			}
		}
	}

	private void init() {
		int x, y;
		pixels = CBufferedImageToDoubleArray.convert(image);

		width = image.getWidth();
		height = image.getHeight();
		gray_scale = new int[width][height];
		beta = (range * (1 + alpha / 100 * (s_max - 1)) / (region_size * region_size));
		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				gray_scale[x][y] = (int) (pixels[x][y][band]);
			}
		}
	}

	/**
	 * Main class to give result for the image
	 * @return changed image
	 */
	public double[][][] AHE() {
		double[][] out = new double[width][height];
		int k = 0, // index for reference histograms ordering
				x, y, // position of just overwriten pixel
				from_x = -region_size,
				from_y = -region_size,
				neighs;
		/** Four buffered histograms necessary for value interpolation */
		int[][] hist = {null, null, null, null};
		/** Region contains set of pixels which will be interpolated by the same set of histograms */
		int[] region,
				currentOrder,
				verse = {0, 3, 2, 1}, // order of histograms used (even line)
				reverse = {0, 1, 2, 3}; // order of histograms used (odd line)

		while (from_x < width || from_y < height) {
			for (int i = 0; i < 2; i++) {
				region = setNextRegion(from_x, from_y);        // overwrite region
				k = setHistNumber(from_x, from_y);        // set changed region number
				hist[k] = developHistogram(region);       // create first histogram
				from_y += region_size;
			}
			from_x += region_size;

			// from is set at the las pixel of read regions
			for (x = Math.max(from_x - 3 * region_size / 2, 0); x < Math.min(from_x - region_size / 2, width); x++) {
				for (y = Math.max(from_y - 3 * region_size / 2, 0); y < Math.min(from_y - region_size / 2, height); y++) {
					out[x][y] = 0;
					neighs = 0;
					if (k % 2 == 0) {
						currentOrder = verse;
					} else {
						currentOrder = reverse;
					}
					if (hist[(k + currentOrder[1]) % 4] != null) {
						out[x][y] += (from_x - region_size / 2 - x) * (from_y - region_size / 2 - y)
								* hist[(k + currentOrder[1]) % 4][gray_scale[x][y]];
						neighs += (from_x - region_size / 2 - x) * (from_y - region_size / 2 - y);
					}
					if (hist[(k + currentOrder[2]) % 4] != null) {
						out[x][y] += (from_x - region_size / 2 - x) * (y - from_y + 3 * region_size / 2)
								* hist[(k + currentOrder[2]) % 4][gray_scale[x][y]];
						neighs += (from_x - region_size / 2 - x) * (y - from_y + 3 * region_size / 2);
					}
					if (hist[(k + currentOrder[3]) % 4] != null) {
						out[x][y] += (x - from_x + 3 * region_size / 2) * (from_y - region_size / 2 - y)
								* hist[(k + currentOrder[3]) % 4][gray_scale[x][y]];
						neighs += (x - from_x + 3 * region_size / 2) * (from_y - region_size / 2 - y);
					}
					if (hist[(k + currentOrder[0]) % 4] != null) {
						out[x][y] += (x - from_x + 3 * region_size / 2) * (y - from_y + 3 * region_size / 2)
								* hist[(k + currentOrder[0]) % 4][gray_scale[x][y]];
						neighs += (x - from_x + 3 * region_size / 2) * (y - from_y + 3 * region_size / 2);
					}
					if (neighs != 0) {
						pixels[x][y][band] = out[x][y] / (neighs * range);
					} else {
						pixels[x][y][band] = 0;
					}
					//System.out.println(out[x][y]);
				}
			}
			if (from_x < width + region_size / 2) {
				from_y -= 2 * region_size;
			} else if (from_y < height + region_size / 2) {
				from_x = -region_size;
				from_y -= region_size;
				hist[0] = hist[1] = hist[2] = hist[3] = null;
			}
			//...
		}
		return pixels;
	}

	private int[] setNextRegion(int from_x, int from_y) {
		int x, y, to_x, to_y;
		int[] region;
		// check if it is necessary to set the region
		if (from_x >= width || from_y >= height || from_x < 0 || from_y < 0) {
			return null;
		} else {
			// set region boundaries
			to_x = Math.min(from_x + region_size, width);
			to_y = Math.min(from_y + region_size, height);
			region = new int[(to_x - from_x) * (to_y - from_y)];
			// overwrite the region
			for (x = from_x; x < to_x; x++) {
				for (y = from_y; y < to_y; y++) {
					region[(x - from_x) * (to_y - from_y) + (y - from_y)] = gray_scale[x][y];
				}
			}
		}
		return region;
	}

	private int setHistNumber(int from_x, int from_y) {
		int k = 0;
		// define one of four used histograms which will be overwritten - k is his number
		if (Math.abs((from_x / region_size) % 2) == 1) {
			k += 2;
		}
		if (Math.abs((from_y / region_size) % 2) == 1) {
			k += 1;
		}

		return k;
	}

	private int[] developHistogram(int[] region) {
		int[] hist;
		hist = CHistogram.oneBandHistogram(region, 256);
		hist = normalizeHistogram(hist);

		return hist;
	}

	private int[] normalizeHistogram(int[] hist) {
		if (hist == null) {
			return null;
		}
		int i, sum = 0, min = 0;

		for (i = 0; i < hist.length; i++) {
			if (hist[i] > 0 && min == 0) {
				min = hist[i];
			}
			sum += hist[i];
			hist[i] = sum;
		}
		if (sum - min != 0) {
			for (i = 0; i < hist.length; i++) {
				hist[i] = (int) Math.round((hist[i] - min) * beta * (region_size * region_size) / sum);
			}
		}


		return hist;
	}

	@Override
	public String getWorkerName() {
		return "Adaptive histogram enhancement";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (image == null) {
			throw new CancellationException("Dialog was cancelled.");
		}

		init();
		return CBufferedImageToDoubleArray.inverse(AHE());
	}
}
