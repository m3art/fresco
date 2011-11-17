/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * @author gimli
 * @version 21.6.2009
 */
public class Crgb2OneBand implements IConstants {

	public static int[] convert(int[] rgb, int band) {
		int[] out = {rgb[band], rgb[band], rgb[band]};
		return out;
	}

	public static BufferedImage convert(BufferedImage img, int band) {
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Raster input = img.getData();
		WritableRaster output = out.getRaster();
		int x, y;
		int[] pixel = new int[rgb_bands];

		for (x = 0; x < img.getWidth(); x++) {
			for (y = 0; y < img.getHeight(); y++) {
				input.getPixel(x, y, pixel);
				output.setPixel(x, y, convert(pixel, band));
			}
		}
		out.setData(output);
		return out;
	}
}
