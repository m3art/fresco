/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.geom.Point2D;

/**
 *
 * @author gimli
 */
public class CPointAndTransformation {
	/** Position of point in original image */
	protected Point2D.Double position;
	/** Transformation parameters, translation, angle and scale */
	protected double dx, dy, angle, scale;

	Point2D.Double getProjection() {
		return CAffineTransformation.getProjected(position.x, position.y, dx, dy, angle, scale, scale);
	}

	/**
	 * Projection of this center point applied on another pixel
	 * @param x coordinate of projected pixel
	 * @param y coordinate of projected pixel
	 * @return projected point
	 */
	public Point2D.Double getProjection(double x, double y) {
		return CAffineTransformation.getProjected(x, y, dx, dy, angle, scale, scale);
	}

	public Point2D.Double getPosition() {
		return position;
	}

	public double getShiftX() {
		return dx;
	}

	public double getShiftY() {
		return dy;
	}

	public double getAngle() {
		return angle;
	}

	public double getScale() {
		return scale;
	}
}
