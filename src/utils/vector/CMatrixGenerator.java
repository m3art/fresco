/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.vector;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public class CMatrixGenerator {

	public static double[][] create(int width, int height, double value) {
		double[][] out = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				out[x][y] = value;
			}
		}

		return out;
	}
}
