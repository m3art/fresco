/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.vector;

import image.CBinaryImage;
import image.converters.Crgb2gray;
import java.awt.image.WritableRaster;

/**
 * Utility for thresholding intensities of pixels
 *
 * @author gimli
 */
public class CTreshold {

	/**
	 * In this method is used conversion to gray values from @see Crgb2grey class
	 * @param raster input image
	 * @param threshold value in gray scale (0-255)
	 * @return new instance of CBinaryImage
	 */
	public static CBinaryImage rgbToBinary(WritableRaster raster, double threshold) {

		double[] pixel = new double[raster.getSampleModel().getNumBands()];
		double value;
		CBinaryImage bi = new CBinaryImage(raster.getWidth(), raster.getHeight());

		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				raster.getPixel(x, y, pixel);
				value = Crgb2gray.convertToOneValue(pixel);

				if (value < threshold) {
					bi.setValue(x, y, false);
				} else {
					bi.setValue(x, y, true);
				}
			}
		}

		return bi;
	}
}
