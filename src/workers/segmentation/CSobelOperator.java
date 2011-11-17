/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

/**
 * Class for image gradient detection
 * @author Honza Blazek
 */
public class CSobelOperator {

	public static int matrix_size = 3;
	public static int[][] Sx = {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}},
			Sy = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};

	/**
	 * Static function for image gradient in x axis for the center of sample matrix A 3x3
	 * @param A sample matrix 3x3
	 * @return value of the gradient on the x axis
	 */
	public static int getGx(int[] A) {
		int i, j;
		int out = 0;

		for (i = 0; i < matrix_size; i++) {
			for (j = 0; j < matrix_size; j++) {
				out += Sx[i][j] * A[i + j * matrix_size];
			}
		}

		return out;
	}

	/**
	 * Static function for image gradient in y axis for the center of sample matrix A 3x3
	 * @param A sample matrix 3x3
	 * @return value of the gradient on the y axis
	 */
	public static int getGy(int[] A) {
		int i, j;
		int out = 0;

		for (i = 0; i < matrix_size; i++) {
			for (j = 0; j < matrix_size; j++) {
				out += Sy[i][j] * A[i + j * matrix_size];
			}
		}

		return out;
	}
}
