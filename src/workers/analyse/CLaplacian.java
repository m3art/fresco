/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Basic edge detector. Uses Laplacian of Gaussian as a filter. Two different
 * sizes are accepted - matrix 5x5 or 3x3
 *
 * @author gimli
 * @version 10.6.2009
 */
public class CLaplacian extends CAnalysisWorker {

	private final static int FILTER_SIZE_DEFAULT = 3;

	/** size of input image */
	private final Dimension size;
	/** output image - with edges */
	private final BufferedImage image;
	/** input raster */
	private final Raster input;
	/** Number of colour bans */
	private final int bands;
	/** Size of filter (3 or 5)*/
	int filterSize;
	/** Precomputed filters */
	public static int[][] M3 = {{1, 1, 1}, {1, -8, 1}, {1, 1, 1}};
	public static int[][] M5 = {{0, 0, 1, 0, 0},
		{0, 1, 2, 1, 0},
		{1, 2, -16, 2, 1},
		{0, 1, 2, 1, 0},
		{0, 0, 1, 0, 0}};

	public CLaplacian(BufferedImage original) {
		size = new Dimension(original.getWidth(), original.getHeight());
		input = original.getData();
		filterSize = FILTER_SIZE_DEFAULT;
		image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
		bands = original.getSampleModel().getNumBands();
	}

	@Override
	protected BufferedImage doInBackground() {
		int x, y, b;
		int[] A = null;
		int[] sum = new int[bands];
		WritableRaster raster = image.getRaster();

		for (x = 0; x < size.width; x++) {
			for (y = 0; y < size.height; y++) {

				if (x - (filterSize - 1) / 2 < 0
						|| x + (filterSize - 1) / 2 > size.width - 1
						|| y - (filterSize - 1) / 2 < 0
						|| y + (filterSize - 1) / 2 > size.height - 1) {
					input.getPixel(x, y, sum);
				} else {
					for (b = 0; b < bands; b++) {
						A = input.getSamples(x - (filterSize - 1) / 2, y - (filterSize - 1) / 2, filterSize, filterSize, b, A);
						sum[b] = getLoG(A);
					}
					for (b = 0; b < bands; b++) {
						sum[b] = Math.abs(sum[b]);
						if (sum[b] > 255) {
							sum[b] = 255;
						} else if (sum[b] < 0) {
							sum[b] = 0;
						}
					}
				}
				raster.setPixel(x, y, sum);
				setProgress(100 * (x * size.height + y) / (size.width * size.height));
			}
		}

		image.setData(raster);
		return image;
	}

	@Override
	public String getWorkerName() {
		return "Laplacian of Gaussian Edge Detector";
	}

	private int getLoG(int[] A) {
		int i, j;
		int out = 0;
		int[][] M = (filterSize == 5) ? M5 : M3;

		for (i = 0; i < filterSize; i++) {
			for (j = 0; j < filterSize; j++) {
				out += M[i][j] * A[i + j * filterSize];
			}
		}

		return out;
	}

	@Override
	public boolean hasDialog() {
		return true;
	}

	private JComboBox filterSizeInput = new JComboBox();

	@Override
	public boolean confirmDialog() {
		filterSize = (Integer)filterSizeInput.getSelectedItem();
		return true;
	}

	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();

		filterSizeInput.addItem((Integer) 3);
		filterSizeInput.addItem((Integer) 5);

		TableLayout layout = new TableLayout(new double[]{200,100}, new double[]{TableLayout.FILL});
		content.setLayout(layout);

		content.add(new JLabel("Set filter size: "),"0,0");
		content.add(filterSizeInput, "1,0");

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}
}
