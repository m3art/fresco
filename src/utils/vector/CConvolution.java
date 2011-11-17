/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.vector;

/**
 *
 * @author gimli
 */
public class CConvolution {

	public static enum Type {

		ones, zeros, I
	};

	/**
	 * Convolution with basic type of matrix filter
	 * @param matrix input matrix
	 * @param width size of filter
	 * @param height size of filter
	 * @param type @see CConvolution.Type
	 * @return result of convolution
	 */
	public static double[][] convolution(double[][] matrix, int width, int height, Type type) {
		double[][] filter = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (type == Type.ones) {
					filter[x][y] = 1;
				} else if (type == Type.zeros) {
					filter[x][y] = 0;
				} else if (x == y) {
					filter[x][y] = 1;
				} else {
					filter[x][y] = 0;
				}
			}
		}

		return convolution(matrix, filter);
	}

	/**
	 * Basic matrix convolution
	 * @param matrix input matrix
	 * @param filter convolution matrix filter
	 * @return convolved input matrix with filter
	 */
	public static double[][] convolution(double[][] matrix, double[][] filter) {
		double[][] out = new double[matrix.length][matrix[0].length];
		int x, y, i, j;
		int size = (filter.length - 1) / 2;

		for (x = 0; x < matrix.length; x++) {
			for (y = 0; y < matrix[0].length; y++) {
				for (i = -size; i <= size; i++) {
					for (j = -size; j <= size; j++) {
						if (x + i >= 0 && y + j >= 0 && x + i < matrix.length && y + j < matrix[0].length) {
							out[x][y] += matrix[x + i][y + j] * filter[size + i][size + j];
						}
					}
				}
			}
		}

		return out;
	}

	/**
	 * Basic matrix convolution
	 * @param matrix input matrix
	 * @param filter convolution matrix filter
	 * @return convolved input matrix with filter
	 */
	public static int[][] convolution(int[][] matrix, int[][] filter) {
		int[][] out = new int[matrix.length][matrix[0].length];
		int x, y, i, j;
		int size = (filter.length - 1) / 2;

		for (x = 0; x < matrix.length; x++) {
			for (y = 0; y < matrix[0].length; y++) {
				for (i = -size; i <= size; i++) {
					for (j = -size; j <= size; j++) {
						if (x + i >= 0 && y + j >= 0 && x + i < matrix.length && y + j < matrix[0].length) {
							out[x][y] += matrix[x + i][y + j] * filter[size + i][size + j];
						}
					}
				}
			}
		}

		return out;
	}
}
