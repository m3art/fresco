/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.Raster;

/**
 * @author gimli
 * @version 21.6.2009
 */
public class Crgb2gray extends CImageConverter {

	public static double[][] convertImage(Raster rgbRaster) {
		double[][] out = new double[rgbRaster.getWidth()][rgbRaster.getHeight()];
		double[] pixel = null;

		for (int x = 0; x < rgbRaster.getWidth(); x++) {
			for (int y = 0; y < rgbRaster.getHeight(); y++) {
				out[x][y] = convertToOneValue(rgbRaster.getPixel(x, y, pixel));
			}
		}

		return out;
	}

	public static int[] convert(int[] rgb) {
		int value = convertToOneValue(rgb);

		return new int[]{value, value, value};
	}

	public static int convertToOneValue(int[] rgb) {
		return (int) Math.round(0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2]);
	}

	public static double convertToOneValue(double[] rgb) {
		return 0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2];
	}

	public static int[] inverse(int gray) {
		int[] rgb = new int[rgb_bands];

		for (int b = 0; b < rgb_bands; b++) {
			rgb[b] = gray;
		}
		return rgb;
	}

	protected int[] convertPixel(int[] rgb) {
		return convert(rgb);
	}
}
