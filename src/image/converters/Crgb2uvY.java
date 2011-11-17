/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

public class Crgb2uvY implements IConstants {

	static double[][] rgb2xyz_matrix = {{0.49, 0.31, 0.2},
		{0.17697, 0.8124, 0.01063},
		{0, 0.01, 0.99}};
	static double[][] xyz2rgb_matrix = {{2.3646, -0.8965, -0.4681},
		{-0.5152, 1.4264, 0.0888},
		{0.0052, -0.0144, 1.0092}};

	/**
	 * Converts triple rgb to uvY colors
	 * @param rgb input triple
	 * @return uvY triple
	 */
	public static double[] convert(double[] rgb) {
		return xyz2uvY(rgb2xyz(rgb));
	}

	/** Converts triple uvY to rgb colors
	 * @param uvY input triple
	 * @return rgb colors
	 */
	public static double[] inverse(double[] uvY) {
		return xyz2rgb(uvY2xyz(uvY));
	}

	public static double[] rgb2xyz(double[] rgb) {
		int i, j;
		double[] out = new double[xyz_bands];

		for (i = 0; i < rgb_bands; i++) {
			for (j = 0; j < rgb_bands; j++) {
				out[i] += rgb[j] * rgb2xyz_matrix[i][j];
			}
		}
		return out;
	}

	public static double[] xyz2rgb(double[] xyz) {
		int i, j;
		double[] out = new double[xyz_bands];

		for (i = 0; i < rgb_bands; i++) {
			for (j = 0; j < rgb_bands; j++) {
				out[i] += xyz[j] * xyz2rgb_matrix[i][j];
			}
		}

		for (j = 0; j < rgb_bands; j++) {
			out[j] = Math.min(out[j], 1.0);
		}
		return out;
	}

	public static double[] xyz2uvY(double[] xyz) {
		double[] uvY = new double[uvY_bands];

		uvY[0] = 4 * xyz[0] / (xyz[0] + 15 * xyz[1] + 3 * xyz[2]);
		uvY[1] = 9 * xyz[1] / (xyz[0] + 15 * xyz[1] + 3 * xyz[2]);
		uvY[2] = xyz[1];

		return uvY;
	}

	public static double[] uvY2xyz(double[] uvY) {
		double[] xyz = new double[xyz_bands];

//        x = 9*uvY[0]/(6*uvY[0]+16*uvY[1]+12);
//        y = 4*uvY[0]/(6*uvY[0]+16*uvY[1]+12);
//        z = 1-x-y;
//
//        xyz[1] = y*(uvY[0]+uvY[1]+uvY[2]);
//        xyz[0] = x*(uvY[0]+uvY[1]+uvY[2]);
//        xyz[2] = z*(uvY[0]+uvY[1]+uvY[2]);


		xyz[0] = (9 * uvY[0] * uvY[2]) / (4 * uvY[1]);
		xyz[1] = uvY[2];
		xyz[2] = -((-12 * uvY[2] + 3 * uvY[0] * uvY[2] + 20 * uvY[1] * uvY[2]) / (4 * uvY[1]));

		return xyz;
	}
}
