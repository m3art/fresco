/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.LinkedList;
import java.util.logging.Level;
import utils.vector.CBasic;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * ColorQuantizer reduce number of colors used in picture. Used algorithm is
 * k-means
 * @author Honza Blazek
 */
public class CColorQuantizer extends CSegmentationWorker {

	/** Default value for k. Output image will have this number of colours. */
	public static int MEANS_DEFAULT = 100;
	/** This number of nearest neighbors is updated other means keeps its places*/
	public static int EVALUATED_NEIGHBORS_DEFAULT = 3;
	/** In each iteration one pixel is evaluated */
	public static double ITERATIONS_PER_PIXEL_DEFAULT = 0.33;

	private BufferedImage original, graphImage;
	/** Floating values of colour means */
	private CColorMean[] color;
	/** Input image size */
	private final Dimension size;
	private int iters, evaluatedNeighbors, means, bands;
	/** Output map of segments */
	CSegmentMap map;
	/** Logging tool */
	private static final Logger logger = Logger.getLogger(CColorQuantizer.class.getName());

	/** Creates new instance of CColorQuantizer
	 * @param pat image to process
	 */
	public CColorQuantizer(BufferedImage pat) {
		original = pat;
		bands = original.getSampleModel().getNumBands();
		size = new Dimension(original.getWidth(), original.getHeight());
		iters = (int)(size.width * size.height * ITERATIONS_PER_PIXEL_DEFAULT);
		evaluatedNeighbors = EVALUATED_NEIGHBORS_DEFAULT;
		means = MEANS_DEFAULT;
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
		map = new CSegmentMap(size.width, size.height, 0);

		// means initialization
		for (i = 0; i < means; i++) {
			color[i] = new CColorMean(bands);
			x = (int) ((size.width - 1) * Math.random());
			y = (int) ((size.height - 1) * Math.random());
			raster.getPixel(x, y, color[i].color);
		}

		// recounting means
		for (i = 0; i < iters; i++) {
			setProgress(i * 100 / iters);
			x = (int) ((size.width - 1) * Math.random());
			y = (int) ((size.height - 1) * Math.random());
			raster.getPixel(x, y, pixel); // pixel is randomly choosed
			metric.setRef(pixel);
			Arrays.sort(color, metric);
			for (j = 0; j < evaluatedNeighbors; j++) {
				color[j].color = CBasic.sum(color[j].color,
						CBasic.scalar(1.0 / (j + 1) / (1 + color[j].weight),
						CBasic.diff(pixel, color[j].color)));
				color[j].weight++;
			}
			setProgress(100 * i / iters);
		}
		for (x = 0; x < size.width; x++) {
			for (y = 0; y < size.height; y++) {
				raster.getPixel(x, y, pixel);
				//pixel = CVectorWorker.normalize(pixel);
				nearest = getNearestMean(pixel, color);
				map.setNumberAt(x, y, -nearest - 1);
				err += CBasic.norm(CBasic.diff(pixel, color[nearest].color));
				//out.setPixel(x, y, color[nearest].color);
			}
		}
		//image.setData(out);
		System.out.println("Error: " + err / (size.width * size.height));
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

	/**
	 * Mean graph shows how precise is approximation by means. Set of colours
	 * contains few highlighted points (means).
	 *
	 * TODO: For usage 3D must be mapped.
	 */
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

	/**
	 * Segment map is generated from means here. For more
	 * @see workers.segmentation.CSegmentMap
	 */
	private void createMap() {
		int segmentSizePixels;
		int segmentId, minX, maxX, minY, maxY;
		LinkedList<Point> segmentsFound = new LinkedList<Point>();
		int[] segmentColor;

		for (int x = 0; x < original.getWidth(); x++) {
			for (int y = 0; y < original.getHeight(); y++) {
				if ((segmentId = map.getNumberAt(x, y)) < 0) { // which are marked and not associated
					segmentColor = color[-map.getNumberAt(x, y) - 1].getColor();
					map.setNumberAt(x, y, map.getNumSegments());

					segmentsFound.add(new Point(x, y)); // first point to check
					segmentSizePixels = 1;
					maxX = maxY = -1;
					minY = minX = Math.max(size.width, size.height);
					while (!segmentsFound.isEmpty()) {
						Point checked = segmentsFound.removeFirst();
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
						if (checked.x > 0 && map.getNumberAt(checked.x - 1, checked.y) == segmentId) {
							map.setNumberAt(checked.x - 1, checked.y, map.getNumSegments());
							segmentsFound.add(new Point(checked.x - 1, checked.y));
						}
						if (checked.y + 1 < size.height && map.getNumberAt(checked.x, checked.y + 1) == segmentId) {
							map.setNumberAt(checked.x, checked.y + 1, map.getNumSegments());
							segmentsFound.add(new Point(checked.x, checked.y + 1));
						}
						if (checked.y > 0 && map.getNumberAt(checked.x, checked.y - 1) == segmentId) {
							map.setNumberAt(checked.x, checked.y - 1, map.getNumSegments());
							segmentsFound.add(new Point(checked.x, checked.y - 1));
						}
						if (checked.x + 1 < size.width && map.getNumberAt(checked.x + 1, checked.y) == segmentId) {
							map.setNumberAt(checked.x + 1, checked.y, map.getNumSegments());
							segmentsFound.add(new Point(checked.x + 1, checked.y));
						}
						segmentSizePixels++;
					}
					map.addSegment(new CSegment(minX, minY, maxX + 1, maxY + 1, map.getNumSegments(), segmentSizePixels, segmentColor));
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

	@Override
	public boolean confirmDialog() {
		try {
			means = Integer.valueOf(numOfColorsInput.getText());
			if (means <= 0 || means > 65535) {
				logger.log(Level.WARNING, "Unsupported number of means. Value must be between: 0-65535. Using default value: {0}", MEANS_DEFAULT);
				means = MEANS_DEFAULT;
			}
		} catch (NumberFormatException nfe) {
			logger.log(Level.WARNING, "Value set by user is not a number. Used default number of colour means: {0}", MEANS_DEFAULT);
		}

		return true;
	}

	JTextField numOfColorsInput = new JTextField(MEANS_DEFAULT);

	/**
	 * Creates inputs for number of colour means. TODO: number of evaluated neighbors and number of iterations
	 * @return created dialog for user
	 */
	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();

		TableLayout layout = new TableLayout(new double[]{200,100}, new double[]{TableLayout.FILL});
		content.setLayout(layout);
		content.add(new JLabel("Set number of output colours:"),"0, 0");
		content.add(numOfColorsInput,"1, 0");

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}
}
