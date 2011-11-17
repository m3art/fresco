/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.colour;

import java.awt.geom.Point2D;

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
}
