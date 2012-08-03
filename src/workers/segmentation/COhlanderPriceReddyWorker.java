/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import utils.vector.CBasic;
import image.converters.Crgb2hsl;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Iterator;
import java.util.LinkedList;
import image.statistics.CHistogram;

/**
 * Class for recursive segmentation method based on spliting
 * (by Ohlader-Price-Reddy)
 */
public class COhlanderPriceReddyWorker extends CSegmentationWorker {

	static int max_value = 360;
	Point size;
	Raster original;
	int[][][] data;
	//LinkedList<CSegment> segments;
	//int[][] map;
	CSegmentMap map;
	int bands;

	public COhlanderPriceReddyWorker(BufferedImage orig) {
		//create image in memory
		size = new Point(orig.getWidth(), orig.getHeight());
		original = orig.getData();
		bands = original.getNumBands() + 3;
		data = new int[size.x][size.y][bands];
		loadSpectralData();
		//create segment map
		map = new CSegmentMap(size.x, size.y, 0);
		//create first segment
		int[] color = new int[bands];
		CSegment segment = new CSegment(0, 0, size.x, size.y, 0, size.x * size.y, color);
		map.addSegment(segment);
	}

	private void loadSpectralData() {
		int[][][] hsl = getHSLImage();
		int[] pixel = new int[original.getNumBands()];
		int x, y, b;

		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {
				original.getPixel(x, y, pixel);
				for (b = 0; b < original.getNumBands(); b++) {
					data[x][y][b] = pixel[b];
				}
				for (b = original.getNumBands(); b < bands; b++) {
					data[x][y][b] = hsl[x][y][b - original.getNumBands()];
				}
			}
		}
	}

	@Override
	protected CSegmentMap doInBackground() {
		LinkedList<int[]> peaks;
		CSegment segment;
		int k = 0, splits = 0;
		int[][] histogram;
		int[] peak;

		// for each segment
		while (map.getNumSegments() - k != 0) {
			segment = map.getSegmentByNumber(k);
			// count histogram
			histogram = CHistogram.getHistogram(segment, map, max_value + 1, data);
			// find peaks
			peaks = findPeaks(balanceHistogram(histogram));
			// choose the best
//            System.out.println("Segment: "+segment.getLowX()+", "+segment.getLowY()+", "+segment.getHighX()+", "+segment.getHighY()+" num:"+segment.getNumber());
			peak = selectBestPeak(peaks, histogram, segment);
			// split segment if necessary
			if (peak != null) {
				splitSegmentByPeak(segment, peak);
				// mark new segments in map and add them to list
				markNewSegments(segment);
				splits++;
			}
			k++;
		}

		setProgress(100);
		System.out.println("segments: " + map.getNumSegments() + " splits: " + splits);
//        return printHistogramAndPeaks(histogram[3],peaks,3);
		return getSegmentMap();
	}

	@Override
	public int[][] getMap() {
		return map.getSegmentMask();
	}

	@Override
	public LinkedList<CSegment> getSegments() {
		return map.getSegments();
	}

	@Override
	public CSegmentMap getSegmentMap() {
		return map;
	}

	private BufferedImage getSegmentedImage() {
		BufferedImage output = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
		WritableRaster out = output.getRaster();
		int i, j;
		int[][] pixel = new int[map.getNumSegments()][bands];
		int[] black = {128, 255, 50};

		// count average color TODO: count pattern
		for (i = map.getNumSegments() - 1; i >= 0; i--) {
			getAvgColor(map.getSegmentByNumber(i), pixel[i]);
		}

		// for each segment set the right color
		for (i = 0; i < size.x; i++) {
			for (j = 0; j < size.y; j++) {
				if (map.getNumberAt(i, j) > -1) {
					out.setPixel(i, j, pixel[map.getNumberAt(i, j)]);
				} else {
					out.setPixel(i, j, black); // if not checked ... bug
				}
			}
		}
		return output;
	}

	private void getAvgColor(CSegment segment, int[] out) {
		int x, y, b, range = 0;
		int[] pixel = new int[bands - original.getNumBands()];
		if (out == null) {
			out = new int[bands];
		}
		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map.getNumberAt(x, y) == segment.getNumber()) {
					original.getPixel(x, y, pixel);
					for (b = 0; b < bands - original.getNumBands(); b++) {
						out[b] += pixel[b];
					}
					range++;
				}
			}
		}
		if (range != 0) {
			for (b = 0; b < bands - original.getNumBands(); b++) {
				out[b] = out[b] / range;
			}
		}
	}

	private BufferedImage printHistogramAndPeaks(int[] histogram, LinkedList<int[]> peaks, int band) {
		int i, j, max = CBasic.max(histogram);
		final int out_size = 300, my = out_size - 1;
		int[] peak;
		int[] black = {255, 255, 255}, red = {255, 0, 0}, green = {0, 255, 0}, blue = {0, 0, 255};
		BufferedImage output = new BufferedImage(360, out_size, BufferedImage.TYPE_INT_RGB);
		WritableRaster out = output.getRaster();
		Iterator<int[]> iter = peaks.listIterator();

		// scale the histogram
		for (i = 0; i < 360; i++) {
			out.setPixel(i, histogram[i] * my / max, black);
		}

		while (iter.hasNext()) {
			peak = iter.next();
			if (peak[0] == band) {
				for (j = -2; j < 2; j++) { // mark peak as crosses
					if (peak[1] + j < out.getWidth() && peak[1] + j > 0) {
						out.setPixel(peak[1] + j, histogram[peak[1]] * my / max, red);
					}
					if (histogram[peak[1]] * my / max + j < out.getHeight() && histogram[peak[1]] * my / max + j > 0) {
						out.setPixel(peak[1], histogram[peak[1]] * my / max + j, red);
					}
					if (peak[2] + j < out.getWidth() && peak[2] + j > 0) {
						out.setPixel(peak[2] + j, histogram[peak[2]] * my / max, green);
					}
					if (histogram[peak[2]] * my / max + j < out.getHeight() && histogram[peak[2]] * my / max + j > 0) {
						out.setPixel(peak[2], histogram[peak[2]] * my / max + j, green);
					}
					if (peak[3] + j < out.getWidth() && peak[3] + j > 0) {
						out.setPixel(peak[3] + j, histogram[peak[3]] * my / max, blue);
					}
					if (histogram[peak[3]] * my / max + j < out.getHeight() && histogram[peak[3]] * my / max + j > 0) {
						out.setPixel(peak[3], histogram[peak[3]] * my / max + j, blue);
					}
				}
			}
		}
		output.setData(out);
		return output;
	}

	private int[][] balanceHistogram(int[][] histogram) {
		int b, i;
		int[][] hist = new int[bands][max_value + 1];
		// smoothing of histogram
		for (b = 0; b < bands; b++) {
			hist[b][0] = (histogram[b][1] + 2 * histogram[b][0]) / 3;
		}
		for (i = 1; i < max_value; i++) {
			for (b = 0; b < bands; b++) {
				hist[b][i] = (histogram[b][i - 1] + histogram[b][i] + histogram[b][i + 1]) / 3;
			}
		}
		for (b = 0; b < bands; b++) {
			hist[b][max_value] = (histogram[b][max_value - 1] + 2 * histogram[b][max_value]) / 3;
		}
		// entropy count

		return hist;
	}

	private int findPoint(int[] hist, boolean rising, int fromX, int toX, int min_size, int min_width, double ratio) {
		int best = hist[fromX], step, lastX = fromX;

		// chceck the side
		step = (fromX > toX) ? -1 : 1;
		// while are you in range and don't find the point
		for (; fromX * step <= toX * step; fromX += step) {
			if ((rising && hist[fromX] > hist[lastX]) || // not local maximum
					(!rising && hist[fromX] < hist[lastX])) {
				lastX = fromX; // not local minimum
			} else {
				if (Math.abs(hist[fromX] - hist[lastX]) > min_size && // difference is greater than minimal sufficient
						Math.max(hist[fromX], hist[lastX]) / Math.max(1, Math.min(hist[fromX], hist[lastX])) > ratio && // ratio is greater than minimal sufficient
						Math.abs(fromX - best) > min_width) {
					return lastX; // width of range is enough
				} else {
					best = rising ? Math.min(hist[fromX], best) : Math.max(hist[fromX], best); // better value than last
				}
			}
			fromX += step;
		}
		if ((Math.abs(best - hist[lastX]) > min_size) || !rising) {
			return lastX; // good result for local minimum and possibly for peak
		} else {
			return -1;
		}
	}

	/**
	 * finds all possible peaks in histogram
	 * @param histogram
	 * @return peaks values - range, peak value and subband
	 */
	private LinkedList<int[]> findPeaks(int[][] histogram) {
		int b;
		final double ratio = 1.4;
		int peak_size, min_color_value, max_color_value, peak_width, peak;
		LinkedList<int[]> peaks_values = new LinkedList<int[]>();
		int[] peak_value = new int[4]; // band, value of color, min_color, max_color

		for (b = 0; b < bands; b++) { // look for peaks in all bands
			peak_size = CBasic.max(histogram[b]) / 15; // limit value for distance between colors (for peak)

			min_color_value = 0;
			max_color_value = max_value;
			while (min_color_value < max_color_value + 1 && histogram[b][min_color_value] == 0) {
				min_color_value++;
			}
			while (min_color_value < max_color_value + 1 && histogram[b][max_color_value] == 0) {
				max_color_value--;
			}
			if (min_color_value > max_color_value) {
				return peaks_values;
			}

			peak_width = (max_color_value - min_color_value) / 50; // limit value for minimum of peak range
			do {
				// finds first peak in range
				peak = findPoint(histogram[b], true, min_color_value, max_color_value, peak_size, peak_width, ratio);
				if (peak != -1) {
					peak_value = new int[4];
					peak_value[0] = b;
					peak_value[1] = peak;
					// find local minimum before peak but not necessary last
					peak_value[2] = findPoint(histogram[b], false, peak, min_color_value, peak_size, peak_width, ratio);
					// find local minimum after peak
					peak_value[3] = findPoint(histogram[b], false, peak, max_color_value, peak_size, peak_width, ratio);
					// set new border for looking for peaks
					min_color_value = peak_value[3] + 1;
					// save values about peak
					peaks_values.add(peak_value);
				}
			} while (peak != -1 && min_color_value < max_value);
		}

		return reduceBorderesOfPeaks(peaks_values, histogram);
	}

	/**
	 * Because the local minimum before and after the peak is not a good border this procedure reduce its range
	 * @param peaks_values local minima and peak value
	 * @return new peaks_values
	 */
	private LinkedList<int[]> reduceBorderesOfPeaks(LinkedList<int[]> peaks_values, int[][] hist) {
		Iterator<int[]> iter = peaks_values.iterator();
		int derivation_avg, i, j, k, min_value;
		int[] temp = new int[max_value + 1];
		int[] peak;

		while (iter.hasNext()) {
			peak = iter.next();

			// left side
			if (peak[2] != peak[1]) {
				min_value = hist[peak[0]][peak[1]];
				derivation_avg = hist[peak[0]][peak[1]] / (2 * (peak[1] - peak[2]));
				for (i = peak[1]; i > peak[2]; i--) {
					min_value = temp[i] = Math.min(hist[peak[0]][i], min_value);
				}

				for (i = peak[2]; i < peak[1]; i++) {
					k = 0;
					if (temp[i + 1] - temp[i] > derivation_avg) {
						for (j = i + 1; j < peak[1] && j < i + 6; j++) {
							if (temp[j + 1] - temp[j] > derivation_avg) {
								k++;
							}
						}
					}
					if ((double) k / (Math.min(6, peak[1] - i)) > 0.8) {
						break;
					}
				}
				if (i != peak[1]) {
					peak[2] = i;
				}
			}

			// right side
			if (peak[3] != peak[1]) {
				min_value = hist[peak[0]][peak[1]];
				// deleni nulou
				derivation_avg = hist[peak[0]][peak[1]] / (2 * (peak[3] - peak[1]));
				for (i = peak[1]; i < peak[3]; i++) {
					min_value = temp[i] = Math.min(hist[peak[0]][i], min_value);
				}

				for (i = peak[3]; i > peak[1]; i--) {
					k = 0;
					if (temp[i - 1] - temp[i] > derivation_avg) {
						for (j = i - 1; j > peak[1] && j > i - 6; j--) {
							if (temp[j - 1] - temp[j] > derivation_avg) {
								k++;
							}
						}
					}
					if ((double) k / (Math.min(6, i - peak[1])) > 0.8) {
						break;
					}
				}
				if (i != peak[1]) {
					peak[3] = i;
				}
			}
		}

		return peaks_values;
	}

	/**
	 * determine the best peak of all possibilities
	 * @param peaks linked list of possibilities
	 * @param hist histogram of image
	 * @return best peak or null
	 */
	private int[] selectBestPeak(LinkedList<int[]> peaks, int[][] histogram, CSegment segment) {
		Iterator<int[]> iter = peaks.listIterator();
		int[] peak, best = null;
		double ent = 0.4 + (size.x * size.y / 50) / segment.size(), entropy, in, out;
		int i;

		while (iter.hasNext()) {
			peak = iter.next();

			in = out = 0;
			for (i = 0; i < max_value + 1; i++) {
				if (i < peak[2] || i > peak[3]) {
					out += histogram[peak[0]][i];
				} else {
					in += histogram[peak[0]][i];
				}
			}
			if (in != 0 && out != 0) {
				entropy = -in / (in + out) * Math.log(in / (in + out)) - out / (in + out) * Math.log(out / (in + out));
			} else {
				continue;
			}

			if (ent < entropy) {
				ent = entropy;
				best = peak;
			}
		}
		return best;
	}

	/**
	 * Function getHSVImage return original image in [hue,saturation,value] vector
	 */
	private int[][][] getHSLImage() {
		final int hsl_size = 3;
		int x, y;
		int[] colors = new int[bands - original.getNumBands()];
		int[][][] out = new int[size.x][size.y][hsl_size];

		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {
				original.getPixel(x, y, colors);
				out[x][y] = Crgb2hsl.convert(colors);
			}
		}
		return out;
	}

	/**
	 * Function creates two groups of pixels (from segment) along peak
	 * @param segment splited segment
	 * @param peak spliting rule
	 */
	private void splitSegmentByPeak(CSegment segment, int[] peak) {
		int x, y, in = 0, out = 0;

		// split segment by the peak
		// the part in peak region gets mark of -2 and the other -1
		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) { // check whole segment
				if (map.getNumberAt(x, y) == segment.getNumber()) {
					// make a difference
					if (data[x][y][peak[0]] < peak[2] || data[x][y][peak[0]] > peak[3]) {
						map.setNumberAt(x, y, -1); // out of peak
						out++;
					} else {
						map.setNumberAt(x, y, -2); // peak
						in++;
					}
				}
			}
		}
	}

	/**
	 * In the splited segment function marks continual shapes
	 * @param segment marked segment
	 */
	private void markNewSegments(CSegment segment) {
		int x, y, look_for, segment_size;
		int minX, maxX, minY, maxY;
		Point checked;
		CSegment created;
		int[] color = null;
		LinkedList<Point> new_segment = new LinkedList<Point>();

		for (x = segment.getLowX(); x < segment.getHighX(); x++) // look at all pixels
		{
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if ((look_for = map.getNumberAt(x, y)) < 0) { // which are marked and not associated
					map.setNumberAt(x, y, map.getNumSegments());
					new_segment.add(new Point(x, y)); // first point to check
					segment_size = 1;
					maxX = maxY = -1;
					minY = minX = Math.max(segment.getHighY(), segment.getHighX());
					while (!new_segment.isEmpty()) {
						checked = new_segment.removeFirst();
						// change border
						if (checked.x > maxX) {
							maxX = checked.x;
						}
						if (checked.y > maxY) {
							maxY = checked.y;
						}
						if (checked.x < minX) {
							minX = checked.x;
						}
						if (checked.y < minY) {
							minY = checked.y;
						}
						//if (maxX >= size.x)
						// System.out.println(checked.x+", "+checked.y);
						// add all neibouroughs
						if (checked.x > 0 && map.getNumberAt(checked.x - 1, checked.y) == look_for) {
							map.setNumberAt(checked.x - 1, checked.y, map.getNumSegments());
							new_segment.add(new Point(checked.x - 1, checked.y));
						}
						if (checked.y + 1 < size.y && map.getNumberAt(checked.x, checked.y + 1) == look_for) {
							map.setNumberAt(checked.x, checked.y + 1, map.getNumSegments());
							new_segment.add(new Point(checked.x, checked.y + 1));
						}
						if (checked.y > 0 && map.getNumberAt(checked.x, checked.y - 1) == look_for) {
							map.setNumberAt(checked.x, checked.y - 1, map.getNumSegments());
							new_segment.add(new Point(checked.x, checked.y - 1));
						}
						if (checked.x + 1 < size.x && map.getNumberAt(checked.x + 1, checked.y) == look_for) {
							map.setNumberAt(checked.x + 1, checked.y, map.getNumSegments());
							new_segment.add(new Point(checked.x + 1, checked.y));
						}
						segment_size++;
					}
					created = new CSegment(minX, minY, maxX + 1, maxY + 1, map.getNumSegments(), segment_size, null);
					color = new int[original.getNumBands()];
					getAvgColor(created, color);
					created.setColor(color);
					map.addSegment(created);
				}
			}
		}
	}

	@Override
	public String getWorkerName() {
		return "Ohlander Priece Reddy";
	}
}
