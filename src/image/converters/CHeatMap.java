/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * This convertor creates from gray image popular heatmap (highest values red
 * lowest blue
 * @author gimli
 */
public class CHeatMap {

	public static BufferedImage convert(BufferedImage input) {
		BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Raster raster = input.getData();
		WritableRaster output = out.getRaster();
		int[] hsv = new int[3];

		for (int x = 0; x < input.getWidth(); x++) {
			for (int y = 0; y < input.getHeight(); y++) {
				raster.getPixel(x, y, hsv);
				hsv[0] = 256 - hsv[0];
				hsv[1] = hsv[2] = 255;
				output.setPixel(x, y, Crgb2hsv.inverse(hsv));
			}
		}

		out.setData(output);
		return out;
	}
}
