/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import utils.vector.CBasic;

/**
 * This static image converter normalizes pixel values in image.
 * Which means that average value of pixels is set to 128 and the
 * standard deviation is set to 32
 * @author gimli
 */
public class CNormalization {

	public static BufferedImage normalize(final BufferedImage input, double meanVal, double varVal) {
		BufferedImage out = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
		WritableRaster outRaster = out.getRaster();
		Raster inRaster = input.getData();
		int[] pixel = new int[inRaster.getNumBands()];
		long[] mean = new long[inRaster.getNumBands()],
				var = new long[inRaster.getNumBands()];
		int b, x, y;

		// vynuluj
		for (b = 0; b < inRaster.getNumBands(); b++) {
			var[b] = mean[b] = 0;
		}

		// spocti potrebnou statistiku
		for (x = 0; x < input.getWidth(); x++) {
			for (y = 0; y < input.getHeight(); y++) {
				inRaster.getPixel(x, y, pixel);
				mean = CBasic.sum(mean, pixel);
				var = CBasic.sum(var, CBasic.vector(pixel, pixel));
			}
		}
		mean = CBasic.divide(input.getWidth() * input.getHeight(), mean);
		var = CBasic.diff(CBasic.divide(input.getWidth() * input.getHeight(), var), CBasic.vector(mean, mean));

		// vyrob normalizovany vystup
		for (x = 0; x < input.getWidth(); x++) {
			for (y = 0; y < input.getHeight(); y++) {
				inRaster.getPixel(x, y, pixel);
				for (b = 0; b < inRaster.getNumBands(); b++) {
					pixel[b] = (int) ((pixel[b] - mean[b]) * varVal / Math.sqrt(var[b]) + meanVal);
					pixel[b] = Math.min(Math.max(0, pixel[b]), 255);
				}
				outRaster.setPixel(x, y, pixel);
			}
		}

		out.setData(outRaster);
		return out;
	}
}
