/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gimli
 * @version 21.6.2009
 */
public class Crgb2grey extends CImageConverter {

	private static final Logger logger = Logger.getLogger(Crgb2grey.class.getName());

	public static BufferedImage oneBandImage(BufferedImage img) {
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Raster input = img.getData();
		WritableRaster output = out.getRaster();
		int x, y;
		int[] pixel = new int[rgb_bands];

		logger.fine("Converting RGB image to grey colour");
		long startTime = System.currentTimeMillis();

		for (x = 0; x < img.getWidth(); x++) {
			for (y = 0; y < img.getHeight(); y++) {
				input.getPixel(x, y, pixel);
				output.setPixel(x, y, convert(pixel));
			}
		}
		out.setData(output);

		logger.log(Level.FINE, "Grey conversion done: {0}ms", (System.currentTimeMillis()-startTime));
		return out;
	}

	public static double[][] convertImage(Raster rgbRaster) {
		double[][] out = new double[rgbRaster.getWidth()][rgbRaster.getHeight()];
		double[] pixels = new double[rgbRaster.getWidth() * rgbRaster.getHeight() * rgbRaster.getNumBands()];

		rgbRaster.getPixels(0, 0, rgbRaster.getWidth(), rgbRaster.getHeight(), pixels);

		for (int x = 0; x < rgbRaster.getWidth(); x++) {
			for (int y = 0; y < rgbRaster.getHeight(); y++) {
				out[x][y] = convertToOneValue(pixels[x*rgbRaster.getHeight()+y], pixels[x*rgbRaster.getHeight()+y+1], pixels[x*rgbRaster.getHeight()+y+2]);
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

	public static double convertToOneValue(double red, double green, double blue) {
		return 0.3 * red + 0.59 * green + 0.11 * blue;
	}

	/** converts RGB value into grayscale  range of intensity is the same */
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

	@Override
	protected int[] convertPixel(int[] rgb) {
		return convert(rgb);
	}
}
