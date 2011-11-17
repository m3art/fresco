/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

/**
 * @author Honza Blazek
 */
import image.converters.IConstants;
import utils.vector.CBasic;
import java.awt.image.*;

public class CSmoothWorker extends CCorrectionWorker {

	int matrixSize = 3, bounds;
	double[] smooth_matrix;
	BufferedImage original, image;

	public CSmoothWorker(BufferedImage orig, int range) {
		int i, j;
		bounds = range;
		matrixSize = range * 2 + 1;
		smooth_matrix = new double[matrixSize * matrixSize];

		for (i = -bounds; i < bounds; i++) {
			for (j = -bounds; j < bounds; j++) {
				smooth_matrix[(i + bounds) * matrixSize + j + bounds] = 1 / Math.sqrt(i * i + j * j + 1);
			}
		}
		original = orig;
	}

	/**
	 * Weighted sum of pixel neibourghs are stored as pixel value
	 * @return transformed whole image
	 */
	public BufferedImage matrixSmooth() {
		double matrix_weight;
		double[][] pixel_matrix = new double[matrixSize * matrixSize][IConstants.rgb_bands];
		double[] pixel = new double[IConstants.rgb_bands];
		int i, j, x, y;
		boolean[] neighs = new boolean[matrixSize * matrixSize];
		Raster raster = original.getData();
		image = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
		WritableRaster output = image.getRaster();

		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				// Add new column (matrixSize pixels :o)
				for (i = -bounds; i < bounds + 1; i++) {
					for (j = -bounds; j < bounds + 1; j++) {
						if (x + i < 0 || y + j < 0 || x + i >= raster.getWidth() || y + j >= raster.getHeight()) {
							neighs[(i + bounds) * matrixSize + (j + bounds)] = false;
							continue;
						}
						raster.getPixel(x + i, y + j, pixel_matrix[(i + bounds) * matrixSize + (j + bounds)]);
						neighs[(i + bounds) * matrixSize + (j + bounds)] = true;
					}
				}
				// create new pixel
				for (j = 0; j < IConstants.rgb_bands; j++) {
					pixel[j] = 0;
				}
				matrix_weight = 0;
				for (i = 0; i < matrixSize * matrixSize; i++) {
					if (neighs[i]) {
						pixel = CBasic.sum(pixel, CBasic.scalar(smooth_matrix[i], pixel_matrix[i]));
						matrix_weight += smooth_matrix[i];
					}
				}
				output.setPixel(x, y, CBasic.scalar(1.0 / matrix_weight, pixel));
				setProgress((x * original.getHeight() + y) * 100
						/ (original.getWidth() * original.getHeight()));
			}
		}
		image.setData(output);
		return image;
	}

	@Override
	protected BufferedImage doInBackground() {
		return matrixSmooth();
	}

	@Override
	public String getWorkerName() {
		return "Smooth";
	}
}
