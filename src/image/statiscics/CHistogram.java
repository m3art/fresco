/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.statiscics;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.IOException;
import workers.segmentation.CSegment;
import workers.segmentation.CSegmentMap;

/**
 *
 * @author gimli
 */
public class CHistogram {

	private final static double EPSILON = 0.1;
	/** Number of histogram bins */
	public int bins;
	/** Real size of histogram bin */
	public double binSize;
	/** Minimal and maximal value stored in histogram*/
	public double min, max;
	/** histogram bin values */
	public int[] binContent;
	/** Number of values stored in histogram */
	public int values;
	/** Entropy of stored values */
	public double entropy;

	private CHistogram(int bins) {
		this.bins = bins;
		min = Double.MAX_VALUE;
		max = Double.MIN_VALUE;
		binContent = new int[bins];
		values = 0;
		entropy = 0;
	}

	public static CHistogram createHistogram(double[] values, int bins) {
		CHistogram out = new CHistogram(bins);

		for(double value: values) {
			if (value < out.min) {
				out.min = value;
			}
			if (value > out.max) {
				out.max = value + EPSILON;
			}
		}

		out.binSize = (out.max-out.min)/bins;

		for(double value: values) {
			out.binContent[(int)((value-out.min)/out.binSize)]++;
		}
		out.values = values.length;

		for(int bin: out.binContent) {
			double pBin = ((double)bin)/out.values;
			if (pBin != 0) {
				out.entropy -= pBin * Math.log(pBin) * bin;
			}
		}

		return out;
	}


	public static int[] getMonochromeHistogram(double[][] imageValues, int range) {
		int[] hist = new int[range];

		for (int x = 0; x < imageValues.length; x++) {
			for (int y = 0; y < imageValues[x].length; y++) {
				hist[(int)imageValues[x][y]]++;
			}
		}

		return hist;
	}

	/**
	 * Counts histogram from obtained image and put it into array of size
	 *
	 * @param raster contains pixel values
	 * @param range max colour value in histogram
	 * @param band colour band used for histogram
	 * @return histogram
	 */
	public static int[] getHistogram(Raster raster, int range, int band) {
		int bands = raster.getNumBands();

		int[] hist = new int[range];
		int[] pixel = new int[bands];

		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				raster.getPixel(x, y, pixel);
				hist[pixel[band]]++;
			}
		}

		return hist;
	}

	public static int[][] get2DHistogram(Raster in1, Raster in2, int range, int band, int band2) throws IOException {
		if (in1.getWidth() != in2.getWidth() || in1.getHeight() != in2.getHeight()) {
			throw new IOException("Bad dimension of input data (images dooesn't have same size)");
		}

		Raster rasterA = in1, rasterB = in2;
		int[][] hist = new int[range][range];
		int[] pixelA = new int[rasterA.getNumBands()], pixelB = new int[rasterB.getNumBands()];

		for (int x = 0; x < in1.getWidth(); x++) {
			for (int y = 0; y < in1.getHeight(); y++) {
				rasterA.getPixel(x, y, pixelA);
				rasterB.getPixel(x, y, pixelB);

				hist[pixelA[band]][pixelB[band2]]++;
			}
		}

		return hist;
	}

	/**
	 * Creates histogram from field values
	 * @param values field of checked values
	 * @param range max value in histogram (min is zero)
	 * @return histogram
	 */
	public static int[] oneBandHistogram(int[] values, int range) {
		if (values == null) {
			return null;
		}

		int i;
		int[] histogram = new int[range];
		for (i = 0; i < values.length; i++) {
			histogram[values[i]]++;
		}
		return histogram;
	}

	/**
	 * Counts histogram on only specified segment (requires map of segments)
	 *
	 * @param segment specification of the region
	 * @param map raw data - segment info
	 * @param range defines maximal value in histogram field
	 * @param data pixel color data
	 * @return created histogram for each band separately
	 */
	public static int[][] getHistogram(final CSegment segment, final CSegmentMap map, int range, final int[][][] data) {
		if (data == null) {
			return null;
		}
		int bands = data[0][0].length;
		int b, i, j;
		int[][] histogram = new int[bands][range];

		for (i = segment.getLowX(); i < segment.getHighX(); i++) {
			for (j = segment.getLowY(); j < segment.getHighY(); j++) {
				if (map.getNumberAt(i, j) == segment.getNumber()) {
					for (b = 0; b < bands; b++) {
						histogram[b][data[i][j][b]]++;
					}
				}
			}
		}
		return histogram;
	}

	public static int[] getSubHistogram(Raster raster, Rectangle rect, int range, int band) {
		int[] out = new int[range];
		int[] pixel = new int[raster.getNumBands()];
		for (int x = 0; x < rect.width; x++) {
			for (int y = 0; y < rect.width; y++) {
				raster.getPixel(x, y, pixel);
				out[pixel[band]]++;
			}
		}
		return out;
	}

	public static int[][] get2DSubHistogram(Raster in1, Raster in2, Rectangle rect1, Rectangle rect2, int range, int band1, int band2) {
		int[][] hist = new int[range][range];
		int[] pixelA = new int[in1.getNumBands()],
				pixelB = new int[in2.getNumBands()];

		for (int x = 0; x < rect1.width; x++) {
			for (int y = 0; y < rect1.height; y++) {
				in1.getPixel(x, y, pixelA);
				in2.getPixel(x, y, pixelB);

				hist[pixelA[band1]][pixelB[band2]]++;
			}
		}

		return hist;
	}

	public static double[] getMean(Raster pattern) {
		int bands = pattern.getNumBands();

		double[] out = new double[bands];
		int[] pixels = new int[pattern.getWidth() * pattern.getHeight() * bands];

		pattern.getPixels(0, 0, pattern.getWidth(), pattern.getHeight(), pixels);

		for (int x = 0; x < pattern.getWidth() * pattern.getHeight() * bands; x += bands) {
			for (int b = 0; b < bands; b++) {
				out[b] += pixels[x + b];
			}
		}

		int weight = pattern.getWidth() * pattern.getHeight();

		for (int b = 0; b < bands; b++) {
			out[b] /= weight;
		}

		return out;
	}

	public static double[] getVar(Raster pattern, double[] mean) {
		int bands = pattern.getNumBands();

		double[] out = new double[bands];
		int[] pixels = new int[pattern.getWidth() * pattern.getHeight() * bands];

		pattern.getPixels(0, 0, pattern.getWidth(), pattern.getHeight(), pixels);

		for (int x = 0; x < pattern.getWidth() * pattern.getHeight() * bands; x += bands) {
			for (int b = 0; b < bands; b++) {
				out[b] += (mean[b] - pixels[x + b]) * (mean[b] - pixels[x + b]);
			}
		}

		int weight = pattern.getWidth() * pattern.getHeight();

		for (int b = 0; b < bands; b++) {
			out[b] /= weight;
		}

		return out;
	}
}
