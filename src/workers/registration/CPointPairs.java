/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author gimli
 */
public class CPointPairs {

	LinkedList<Point2D.Double> origins = new LinkedList<Point2D.Double>();
	LinkedList<Point2D.Double> projected = new LinkedList<Point2D.Double>();

	public LinkedList<Point2D.Double> getOrigins() {
		return origins;
	}

	public LinkedList<Point2D.Double> getProjected() {
		return projected;
	}

	public void addPointPair(Point2D.Double origin, Point2D.Double projection) {
		origins.add(origin);
		projected.add(projection);
	}

	/**
	 * Test for content of PointPairs. If projected is not set (null value) only
	 * origin is searched, otherwise projection of origin must correspond to
	 * projected.
	 *
	 * @param origin searched point in point pairs
	 * @param projected optional corresponding point
	 * @return true if origin/pair is in point set
	 */
	public boolean contains(Point2D.Double origin, Point2D.Double projected) {
		Iterator<Point2D.Double> itProj = this.projected.iterator();
		Iterator<Point2D.Double> itOrig = origins.iterator();
		while (itOrig.hasNext()) {
			Point2D.Double orig = itOrig.next();
			Point2D.Double proj = itProj.next();

			if (orig.x == origin.x && orig.y == origin.y && projected == null) {
				return true;
			} else if (orig.x == origin.x && orig.y == origin.y && projected.x == proj.x && projected.y == proj.y) {
				return true;
			}
		}
		return false;
	}
}
