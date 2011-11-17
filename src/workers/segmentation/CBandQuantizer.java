/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.LinkedList;
import utils.metrics.CDistanceComparator;

/**
 * @author gimli
 * @version 15.6.2009
 */
public class CBandQuantizer extends CSegmentationWorker {

	BufferedImage original, image, graphImage;
	Point size;
	int iters, Neigh, means, bands;
	Double[][] color;
	CSegmentMap map;

	/**
	 * Creates new instance of CBandQuantizer
	 * @param pat image to process
	 */
	public CBandQuantizer(BufferedImage pat, int iterations, int neiboroughs, int colors) {
		original = pat;
		bands = original.getSampleModel().getNumBands();
		size = new Point(original.getWidth(), original.getHeight());
		iters = iterations;
		Neigh = neiboroughs;
		image = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
		means = colors;
		color = new Double[bands][means];
		for (int b = 0; b < bands; b++) {
			for (int m = 0; m < means; m++) {
				color[b][m] = 0.0;
			}
		}
	}

	@Override
	protected CSegmentMap doInBackground() {
		double[] pixel = new double[bands];
		Raster raster = original.getData();
		int b, i, j, x, y, nearest;
		int[][] weight = new int[bands][means];
		CDistanceComparator metric = new CDistanceComparator(0);

		map = new CSegmentMap(size.x, size.y, 0);

		// means initialization
		for (i = 0; i < means; i++) {
			x = (int) ((size.x - 1) * Math.random());
			y = (int) ((size.y - 1) * Math.random());
			raster.getPixel(x, y, pixel);
			for (b = 0; b < bands; b++) {
				color[b][i] = pixel[b];
			}
		}

		// recounting means
		for (i = 0; i < iters; i++) {
			setProgress(i * 100 / iters);
			x = (int) ((size.x - 1) * Math.random());
			y = (int) ((size.y - 1) * Math.random());
			raster.getPixel(x, y, pixel); // pixel is randomly choosed

			for (b = 0; b < bands; b++) {
				metric.setCenter(pixel[b]);
				Arrays.sort(color[b], metric);
				for (j = 0; j < Neigh; j++) {
					color[b][j] += 1.0 / (j + 1) / (1 + Math.pow(weight[b][j], 0.4)) * (pixel[b] - color[b][j].doubleValue());
					weight[b][j]++;
				}
			}
			setProgress(100 * i / iters);
		}
		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {
				raster.getPixel(x, y, pixel);
				nearest = getNearestMean(pixel, color);
				map.setNumberAt(x, y, -nearest - 1);
			}
		}

		return getSegmentMap();
	}

	/** Function returns the nearest mean to the color of pixel, form array of means color
	 * @param pixel double array of colors for pixel
	 * @param color double array of color means
	 */
	private int getNearestMean(double[] pixel, Double[][] color) {
		int[] nearest = new int[bands];
		int out = 0;
		double error, min_error = 255 * 255 + 255 * 255 + 255 * 255;
		double[] me = {min_error, min_error, min_error};


		for (int i = 0; i < means; i++) {
			for (int b = 0; b < bands; b++) {
				error = Math.abs(pixel[b] - color[b][i]);
				if (error < me[b]) {
					nearest[b] = i;
					me[b] = error;
				}
			}
		}
		for (int b = 0; b < bands; b++) {
			out = (out << 8) + nearest[b];
		}
		return out;
	}

	private void createMap() {
		int x, y, segment_size, b, col;
		int look_for, minX, maxX, minY, maxY;
		LinkedList<Point> new_segment = new LinkedList<Point>();
		Point checked;
		int[] segment_color = new int[bands];

		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				if ((look_for = map.getNumberAt(x, y)) < 0) { // which are marked and not associated
					col = -look_for - 1;
					for (b = 0; b < bands; b++) {
						segment_color[b] = (int) (color[b][col >> ((bands - b - 1) * 8)].doubleValue());
						col = (col << ((b + (4 - bands) + 1) * 8)) >> ((b + (4 - bands) + 1) * 8);
					}
					map.setNumberAt(x, y, map.getNumSegments());

					new_segment.add(new Point(x, y)); // first point to check
					segment_size = 1;
					maxX = maxY = -1;
					minY = minX = Math.max(size.x, size.y);
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
					map.addSegment(new CSegment(minX, minY, maxX + 1, maxY + 1, map.getNumSegments(), segment_size, segment_color));
//					System.out.println("p"+(map.getNumSegments()-1)+": ["+map.getSegmentByNumber(map.getNumSegments()-1).getColor()[0]+","+
//									 map.getSegmentByNumber(map.getNumSegments()-1).getColor()[1]+", "+
//									 map.getSegmentByNumber(map.getNumSegments()-1).getColor()[2]+"]");
				}
			}
		}
		System.out.println("Segments: " + map.getNumSegments());
	}

	@Override
	public int[][] getMap() {
		if (map.getNumberAt(0, 0) < 0) {
			createMap();
		}
		return map.getSegmentMask();
	}

	@Override
	public LinkedList<CSegment> getSegments() {
		if (map.getNumberAt(0, 0) < 0) {
			createMap();
		}
		return map.getSegments();
	}

	@Override
	public CSegmentMap getSegmentMap() {
		if (map.getNumberAt(0, 0) < 0) {
			createMap();
		}
		return map;
	}

	@Override
	public String getWorkerName() {
		return "Color quantization";
	}
}
