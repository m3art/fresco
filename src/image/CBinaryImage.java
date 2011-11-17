/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image;

import image.converters.Crgb2gray;
import it.tidalwave.imageio.util.Logger;
import java.awt.image.Raster;

/**
 * Type of image which allows only two values for pixels [0,1]
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CBinaryImage {

	int edgePixels = 0;
	final int width, height;
	boolean[][] image;
	private final static Logger logger = Logger.getLogger(CBinaryImage.class.getName());

	/**
	 * Use this constructor for manually set values of image pixels. After calling
	 * this, all values will be set to zero.
	 *
	 * @param width size of image
	 * @param height size of image
	 */
	public CBinaryImage(int width, int height) {
		this.width = width;
		this.height = height;
		image = new boolean[width][height];
	}

	/**
	 * Creates binary representation of parameter
	 * @param raster contains original image
	 * @param threshold for pixel intensities
	 */
	public CBinaryImage(Raster raster, double threshold) {
		width = raster.getWidth();
		height = raster.getHeight();

		image = new boolean[width][height];

		double[] pixel = null;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixel = raster.getPixel(x, y, pixel);
				if (threshold < Crgb2gray.convertToOneValue(pixel)) {
					image[x][y] = true;
					edgePixels++;
				}
			}
		}

		logger.info("Edge pixels:" + edgePixels);
	}

	/**
	 * @param x
	 * @param y coordinates of pixel
	 * @return true if position [x,y] contains value above threshold
	 */
	public boolean isOne(int x, int y) {
		return image[x][y];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Manual setting of pixel value
	 * @param x coordinate of pixel
	 * @param y coordinate of pixel
	 * @param value true if one, false otherwise
	 */
	public void setValue(int x, int y, boolean value) {
		image[x][y] = value;
	}
}
