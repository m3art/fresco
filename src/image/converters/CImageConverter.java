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
public abstract class CImageConverter implements IConstants {

	public BufferedImage convert(BufferedImage img) {
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Raster input = img.getData();
		WritableRaster output = out.getRaster();

		convert(input, output);

		out.setData(output);
		return out;
	}

	protected void convert(Raster img, WritableRaster output) {
		int x, y;
		int[] pixel = new int[rgb_bands];

		for (x = 0; x < img.getWidth(); x++) {
			for (y = 0; y < img.getHeight(); y++) {
				img.getPixel(x, y, pixel);
				output.setPixel(x, y, convertPixel(pixel));
			}
		}
	}

	protected abstract int[] convertPixel(int[] pixel);
}
