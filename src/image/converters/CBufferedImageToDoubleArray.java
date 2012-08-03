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

	/**
	 * Takes image and converts it into matrix. Each row represents colour band,
	 * each column represents one pixel
	 * @param image converted image
	 * @return matrix of pixels
	 */
	public static double[][] convertToPixelArray(BufferedImage image) {
		int numOfPixels = image.getWidth()*image.getHeight();
		Raster raster = image.getData();
		double[] data = raster.getPixels(0, 0, image.getWidth(), image.getHeight(), new double[image.getWidth()*image.getHeight()*raster.getNumBands()]);
		int bands = data.length/numOfPixels;
		double[][] pixels = new double[numOfPixels][bands];

		for(int i=0; i<data.length; i++) {
			int band = i%bands;
			pixels[i/bands][band] = data[i];
		}
		return pixels;
	}

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

	public static BufferedImage inverseFromPixelArray(double[][] pixels, int width, int height) {
		assert(width*height == pixels.length);

		BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = output.getRaster();

		for(int x=0; x<width; x++)
			for(int y=0; y<height; y++) {
				raster.setPixel(x, y, pixels[x*height+y]);
			}

		return output;
	}
}
