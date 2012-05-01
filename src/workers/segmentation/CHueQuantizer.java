/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import fresco.swing.CWorkerDialogFactory;
import image.converters.Crgb2hsv;
import info.clearthought.layout.TableLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import utils.vector.CBasic;

/**
 *
 * @author gimli
 */
public class CHueQuantizer extends CSegmentationWorker {
/** Default value for k. Output image will have this number of colours. */
	public static int MEANS_DEFAULT = 100;
	/** This number of nearest neighbors is updated other means keeps its places*/
	public static int EVALUATED_NEIGHBORS_DEFAULT = 0;
	/** In each iteration one pixel is evaluated */
	public static double ITERATIONS_PER_PIXEL_DEFAULT = 1;

	private BufferedImage original, graphImage;
	/** Floating values of hue means */
	private CColorMean[] color;
	/** Input image size */
	private final Dimension size;
	private int iters, evaluatedNeighbors, means, bands;
	/** Output map of segments */
	CSegmentMap map;
	/** Logging tool */
	private static final Logger logger = Logger.getLogger(CHueQuantizer.class.getName());

	/** Creates new instance of CColorQuantizer
	 * @param pat image to process
	 */
	public CHueQuantizer(BufferedImage pat) {
		original = pat;
		bands = original.getSampleModel().getNumBands();
		size = new Dimension(original.getWidth(), original.getHeight());
		iters = (int)(size.width * size.height * ITERATIONS_PER_PIXEL_DEFAULT);
		evaluatedNeighbors = EVALUATED_NEIGHBORS_DEFAULT;
		means = MEANS_DEFAULT;
	}

	private class HueComparator implements Comparator<CColorMean> {

		double ref;

		public void setRef(double hue) {
			ref = hue;
		}

		@Override
		public int compare(CColorMean o1, CColorMean o2) {
			if (Math.abs(o1.color[0] - ref)%360 < Math.abs(o2.color[0] - ref)%360)
				return 1;
			else if (Math.abs(o1.color[0] - ref) == Math.abs(o2.color[0] - ref))
				return 0;
			else
				return -1;
		}
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

		color = new CColorMean[means];
		map = new CSegmentMap(size.width, size.height, 0);


		// means initialization
		for (i = 0; i < means; i++) {
			color[i] = new CColorMean(3);
			x = (int) ((size.width - 1) * Math.random());
			y = (int) ((size.height - 1) * Math.random());
			raster.getPixel(x, y, pixel);
			color[i].color[0] = 360 * i / means;
			color[i].color[1] = 128;
			color[i].color[2] = 128;
		}

		HueComparator hueComparator = new HueComparator();

		// recounting means
		for (i = 0; i < iters; i++) {
			setProgress(i * 100 / iters);
			x = (int) ((size.width - 1) * Math.random());
			y = (int) ((size.height - 1) * Math.random());
			double hue = Crgb2hsv.getHue(raster.getPixel(x, y, pixel)); // pixel is randomly choosed
			hueComparator.setRef(hue);
			Arrays.sort(color, hueComparator);
			for (j = 0; j < evaluatedNeighbors; j++) {
				color[j].color[0] += 1/(1+j)/(1+color[j].weight)*(hue - color[j].color[0]);
				color[j].color[0] %= 360;
				color[j].weight++;
			}
			setProgress(100 * i / iters);
		}
		// classify pixels
		for (x = 0; x < size.width; x++) {
			for (y = 0; y < size.height; y++) {
				raster.getPixel(x, y, pixel);
				//pixel = CVectorWorker.normalize(pixel);
				nearest = getNearestMean(Crgb2hsv.getHue(pixel), color);
				map.setNumberAt(x, y, -nearest - 1);
				err += CBasic.norm(CBasic.diff(pixel, color[nearest].color));
				//out.setPixel(x, y, color[nearest].color);
			}
		}
		//image.setData(out);
		System.out.println("Error: " + err / (size.width * size.height));

		return getSegmentMap();
	}

	/** Function returns the nearest mean to the color of pixel, form array of means color
	 * @param pixel double array of colors for pixel
	 * @param color double array of color means
	 */
	private int getNearestMean(double hue, CColorMean[] color) {
		int i, nearest = 0;
		double error, min_error = 255 * 255 + 255 * 255 + 255 * 255;

		for (i = 0; i < color.length; i++) {
			error = Math.abs(hue - color[i].color[0])%360;
			if (error < min_error) {
				nearest = i;
				min_error = error;
			}
		}
		return nearest;
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
					segmentColor = Crgb2hsv.inverse(color[-map.getNumberAt(x, y) - 1].getColor());
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
		content.add(new JLabel("Set number of output hues:"),"0, 0");
		content.add(numOfColorsInput,"1, 0");

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}
}
