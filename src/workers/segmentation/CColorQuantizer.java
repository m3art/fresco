/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import java.util.LinkedList;
import utils.vector.CBasic;
import java.awt.image.*;
import java.awt.*;
import java.util.Arrays;

/**
 * ColorQuantizer reduce number of colors used in picture.
 * @author Honza Blazek
 */
public class CColorQuantizer extends CSegmentationWorker {

	BufferedImage original, image, graphImage;
	CColorMean[] color;
	Point size;
	int iters, Neigh, means, bands;
	//int[][] map;
	//LinkedList<CSegment> segments;
	CSegmentMap map;

	/** Creates new instance of CColorQuantizer
	 * @param pat image to process
	 */
	public CColorQuantizer(BufferedImage pat, int iterations, int neiboroughs, int colors) {
		original = pat;
		bands = original.getSampleModel().getNumBands();
		size = new Point(original.getWidth(), original.getHeight());
		iters = iterations;
		Neigh = neiboroughs;
		image = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
		means = colors;
	}

	/**
	 * K_Mean reduces colors to values of k vectors
	 */
	@Override
	public CSegmentMap doInBackground() {

		double[] pixel = new double[bands];
		Raster raster = original.getData();
		//WritableRaster out = image.getRaster();
		int i, j, x, y, nearest;
		double err = 0;
		CVectorMetric metric = new CVectorMetric(pixel);

		color = new CColorMean[means];
		map = new CSegmentMap(size.x, size.y, 0);

		// means initialization
		for (i = 0; i < means; i++) {
			color[i] = new CColorMean(bands);
			x = (int) ((size.x - 1) * Math.random());
			y = (int) ((size.y - 1) * Math.random());
			raster.getPixel(x, y, color[i].color);
		}

		// recounting means
		for (i = 0; i < iters; i++) {
			setProgress(i * 100 / iters);
			x = (int) ((size.x - 1) * Math.random());
			y = (int) ((size.y - 1) * Math.random());
			raster.getPixel(x, y, pixel); // pixel is randomly choosed
			metric.setRef(pixel);
			Arrays.sort(color, metric);
			for (j = 0; j < Neigh; j++) {
				color[j].color = CBasic.sum(color[j].color,
						CBasic.scalar(1.0 / (j + 1) / (1 + color[j].weight),
						CBasic.diff(pixel, color[j].color)));
				color[j].weight++;
			}
			setProgress(100 * i / iters);
		}
		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {
				raster.getPixel(x, y, pixel);
				//pixel = CVectorWorker.normalize(pixel);
				nearest = getNearestMean(pixel, color);
				map.setNumberAt(x, y, -nearest - 1);
				err += CBasic.norm(CBasic.diff(pixel, color[nearest].color));
				//out.setPixel(x, y, color[nearest].color);
			}
		}
		//image.setData(out);
		System.out.println("Error: " + err / (size.x * size.y));
		setGraph();

		return getSegmentMap();
	}

	/** Function returns the nearest mean to the color of pixel, form array of means color
	 * @param pixel double array of colors for pixel
	 * @param color double array of color means
	 */
	private int getNearestMean(double[] pixel, CColorMean[] color) {
		int i, nearest = 0;
		double error, min_error = 255 * 255 + 255 * 255 + 255 * 255;

		for (i = 0; i < color.length; i++) {
			error = CVectorMetric.getEukleid(pixel, color[i].color);
			if (error < min_error) {
				nearest = i;
				min_error = error;
			}
		}
		return nearest;
	}

	public void setGraph() {
		final int width = 380, height = 380;
		final int cross = 5;

		graphImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D graph = graphImage.createGraphics();
		Raster raster = original.getRaster();
		double[] pixel = new double[bands];
		int i, j;
		int x, y;

		graph.setBackground(Color.white);
		graph.clearRect(0, 0, width, height);

		graph.setPaint(Color.red);
		for (i = 0; i < original.getWidth(); i++) {
			for (j = 0; j < original.getHeight(); j++) {
				raster.getPixel(i, j, pixel);

				x = (int) (pixel[0] + 0.5 * pixel[2]);
				y = (int) (pixel[1] + 0.5 * pixel[2]);

				graph.drawLine(x - cross / 2, y, x + cross / 2, y);
				graph.drawLine(x, y - cross / 2, x, y + cross / 2);
			}
		}
		graph.setPaint(Color.black);
		for (i = 0; i < color.length; i++) {
			x = (int) (color[i].color[0] + 0.5 * color[i].color[2]);
			y = (int) (color[i].color[1] + 0.5 * color[i].color[2]);

			graph.drawLine(x - cross, y, x + cross, y);
			graph.drawLine(x, y - cross, x, y + cross);
		}
	}

	public BufferedImage getGraph() {
		return graphImage;
	}

	private void createMap() {
		int x, y, segment_size;
		int look_for, minX, maxX, minY, maxY;
		LinkedList<Point> new_segment = new LinkedList<Point>();
		Point checked;
		int[] segment_color;

		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				if ((look_for = map.getNumberAt(x, y)) < 0) { // which are marked and not associated
					segment_color = color[-map.getNumberAt(x, y) - 1].getColor();
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
