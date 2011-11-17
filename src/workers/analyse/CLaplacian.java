/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * @author gimli
 * @version 10.6.2009
 */
public class CLaplacian extends CAnalysisWorker {

	Point size;
	BufferedImage data, image;
	Raster input;
	WritableRaster output;
	int bands, matrix_size;
	public static int[][] M3 = {{1, 1, 1}, {1, -8, 1}, {1, 1, 1}};
	public static int[][] M5 = {{0, 0, 1, 0, 0},
		{0, 1, 2, 1, 0},
		{1, 2, -16, 2, 1},
		{0, 1, 2, 1, 0},
		{0, 0, 1, 0, 0}};

	public CLaplacian(BufferedImage original, int m_size) {
		size = new Point(original.getWidth(), original.getHeight());
		data = original;
		input = data.getData();
		matrix_size = (m_size == 5) ? 5 : 3;
		image = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
		bands = original.getSampleModel().getNumBands();
	}

	@Override
	protected BufferedImage doInBackground() {
		int x, y, b;
		int[] A = null;
		int[] sum = new int[bands];
		WritableRaster raster = image.getRaster();

		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {

				if (x - (matrix_size - 1) / 2 < 0
						|| x + (matrix_size - 1) / 2 > size.x - 1
						|| y - (matrix_size - 1) / 2 < 0
						|| y + (matrix_size - 1) / 2 > size.y - 1) {
					input.getPixel(x, y, sum);
				} else {
					for (b = 0; b < bands; b++) {
						A = input.getSamples(x - (matrix_size - 1) / 2, y - (matrix_size - 1) / 2, matrix_size, matrix_size, b, A);
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
				setProgress(100 * (x * size.y + y) / (size.x * size.y));
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
		int[][] M = (matrix_size == 5) ? M5 : M3;

		for (i = 0; i < matrix_size; i++) {
			for (j = 0; j < matrix_size; j++) {
				out += M[i][j] * A[i + j * matrix_size];
			}
		}

		return out;
	}
}
