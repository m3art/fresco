/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 * @author gimli
 */
public class CBufferedImageToDoubleArray {

	public static double[][][] convert(BufferedImage image) {
		Raster raster = image.getData();
		double[][][] original = new double[image.getWidth()][image.getHeight()][raster.getNumBands()];

		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				raster.getPixel(x, y, original[x][y]);
			}
		}
		return original;
	}

	public static BufferedImage inverse(double[][][] data) {
		BufferedImage output = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = output.getRaster();

		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				raster.setPixel(x, y, data[x][y]);
			}
		}
		output.setData(raster);
		return output;
	}
}
