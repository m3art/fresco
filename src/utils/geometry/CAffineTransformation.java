/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public class CAffineTransformation implements ITransformation2D {

	private double shiftX, shiftY;
	private double angle;
	private double scaleX, scaleY;

	public Point2D.Double getProjected(Point2D.Double origin) {
		return getProjected(origin.x, origin.y);
	}

	public Point2D.Double getProjected(double x, double y) {
		return getProjected(x, y, shiftX, shiftY, angle, scaleX, scaleY);
	}

	public void setParameters(double shiftX, double shiftY, double angle, double scaleX, double scaleY) {
		this.shiftX = shiftX;
		this.shiftY = shiftY;
		this.angle = angle;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

	public static Point2D.Double getProjected(double x, double y, double shiftX, double shiftY, double angle, double scaleX, double scaleY) {
		Point2D.Double projected = new Point2D.Double();

		projected.x = scaleX * (Math.cos(angle) * x - Math.sin(angle) * y) + shiftX;
		projected.y = scaleY * (Math.sin(angle) * x + Math.cos(angle) * y) + shiftY;

		return projected;
	}

	public Point2D.Double getInversion(double x, double y) {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	public Double getInversion(Double projection) {
		return getInversion(projection.x, projection.y);
	}
}
