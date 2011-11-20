/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import workers.segmentation.*;
import java.awt.image.*;
import java.awt.*;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Colour implementation of canny edge detector.
 * @author gimli
 */
public class CCannyEdgeDetector extends CAnalysisWorker {

	/** Size of output image */
	private final Dimension size;
	/** Output image */
	private final BufferedImage image;
	/** Input data in raster format */
	private final Raster input;
	/** Number of colours presented in input data.
	 * NOTE: original algorithm works with grayscale images. Here is edge detection
	 * made for each colour band.
	 */
	private final int bands;
	/** Thresholds defined in article by Canny. Low threshold is used for pixels
	 * neighboring with edge pixel thresholded by high threshold.
	 *
	 * It is not necessary to use thresholds, in this case only convolution is
	 * made.
	 */
	private double lowThreshold = 0.3f, highThreshold = 0.7f;

	/**
	 * Basic nonparametric usage of canny edge detector. No thresholding is used.
	 * @param original input image
	 */
	public CCannyEdgeDetector(BufferedImage original) {
		size = new Dimension(original.getWidth(), original.getHeight());
		input = original.getData();
		image = new BufferedImage((int) size.getWidth(), (int) size.getHeight(), BufferedImage.TYPE_INT_RGB);
		bands = original.getSampleModel().getNumBands();

	}

	/**
	 * Extended version. Low threshold is used for edges which lie next to edges
	 * founded by high threshold.
	 *
	 * @param original input image
	 * @param lowThreshold edge threshold
	 * @param highThreshold edge threshold
	 */
	public CCannyEdgeDetector(BufferedImage original, int lowThreshold, int highThreshold) {
		this(original);
		this.lowThreshold = lowThreshold;
		this.highThreshold = highThreshold;
	}

	@Override
	protected BufferedImage doInBackground() {
		int x, y, b;
		int[] A = null;
		int[] sum = new int[bands], sum_x = new int[bands], sum_y = new int[bands];
		WritableRaster raster = image.getRaster();

		for (x = 0; x < size.getWidth(); x++) {
			for (y = 0; y < size.getHeight(); y++) {
				if (y == 0 || y == size.getHeight() - 1 || x == 0 || x == size.getWidth() - 1) {
					for (b = 0; b < bands; b++) {
						sum[b] = 0;
					}
				} else {
					for (b = 0; b < bands; b++) {
						A = input.getSamples(x - 1, y - 1, CSobelOperator.matrix_size, CSobelOperator.matrix_size, b, A);
						sum_x[b] = CSobelOperator.getGx(A);
						sum_y[b] = CSobelOperator.getGy(A);
					}

					for (b = 0; b < bands; b++) {
						sum[b] = Math.abs(sum_x[b]) + Math.abs(sum_y[b]);
					}
				}
				raster.setPixel(x, y, sum);
				setProgress(100 * (x * ((int) size.getHeight()) + y) / ((int) (size.getWidth() * size.getHeight())));
			}
		}
		image.setData(raster);
		return image;
	}

	@Override
	public String getWorkerName() {
		return "Edge detector";
	}

	@Override
	public boolean hasDialog() {
		return true;
	}

	private final int SLIDER_MAX = 100;
	static JSlider lowThresh, highThresh;

	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();
		JPanel inputs = new JPanel();

		TableLayout layout = new TableLayout(new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5},new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5});

		if (lowThresh == null) {
			lowThresh = new JSlider(JSlider.HORIZONTAL, 1, SLIDER_MAX, 30);
			highThresh = new JSlider(JSlider.HORIZONTAL, 1, SLIDER_MAX, 70);
		}

		inputs.setLayout(layout);
		inputs.add(new JLabel("Low threshold: "), "1, 1");
		inputs.add(lowThresh, "3, 1");
		inputs.add(new JLabel("High threshold: "), "1, 3");
		inputs.add(highThresh, "3, 3");

		content.setLayout(new BorderLayout(5,5));
		content.add(new JLabel("<html><body>Set thresholds for edge detection. <br />Low threshold is used for pixels neighboring with edge.</body></html>"), BorderLayout.NORTH);
		content.add(inputs, BorderLayout.CENTER);

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}

	@Override
	public boolean confirmDialog() {
//		detector.setLowThreshold(40 * lowThresh.getValue()/SLIDER_MAX);
//		detector.setHighThreshold(40 * highThresh.getValue()/SLIDER_MAX);
		lowThreshold = 0.3f;
		highThreshold = 0.7f;
		return true;
	}
}
