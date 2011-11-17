/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.geom.Point2D;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public interface ITransformation2D {

	public Point2D.Double getProjected(Point2D.Double origin);

	public Point2D.Double getProjected(double x, double y);

	public Point2D.Double getInversion(Point2D.Double projection);
}
