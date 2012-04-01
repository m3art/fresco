/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.colour;

import java.awt.geom.Point2D;
import java.awt.image.Raster;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public class CBilinearInterpolation {

	/**
	 * Interpolation of pixel intensity (gray scale)
	 * @param position double coordinates. Interpolation is taken from nearest
	 * four points with integer coordinates
	 * @param leftTop gray intensity in nearest left top pixel
	 * @param rightTop dtto
	 * @param leftBottom dtto
	 * @param rightBottom dtto
	 * @return interpolated value of gray intensity at @param position
	 */
	public static double getValue(Point2D.Double position, double leftTop, double rightTop, double leftBottom, double rightBottom) {

		double alpha = 1 - position.x + (int) position.x,
				beta = 1 - position.y + (int) position.y;

		return beta * (alpha * leftTop + (1 - alpha) * rightTop)
				+ (1 - beta) * (alpha * leftBottom + (1 - alpha) * rightBottom);
	}

	public static double[] getValue(Point2D.Double position, Raster image) {
		double rgba[] = new double[image.getNumBands()];

		if (position.x == (int)position.x && position.y == (int)position.y) {
			return image.getPixel((int)position.x, (int)position.y, rgba);
		}

		for (int i = 0; i < image.getNumBands(); i++) {
			rgba[i] = getValue(position, image.getSampleDouble((int) position.x, (int) position.y, i),
					image.getSampleDouble((int) position.x + 1, (int) position.y, i),
					image.getSampleDouble((int) position.x, (int) position.y + 1, i),
					image.getSampleDouble((int) position.x + 1, (int) position.y + 1, i));
		}


		return rgba;
	}
}
