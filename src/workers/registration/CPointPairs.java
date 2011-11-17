/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author gimli
 */
public class CPointPairs {

	LinkedList<Point> origins = new LinkedList<Point>();
	LinkedList<Point> projected = new LinkedList<Point>();

	public LinkedList<Point> getOrigins() {
		return origins;
	}

	public LinkedList<Point> getProjected() {
		return projected;
	}

	public void addPointPair(Point origin, Point projection) {
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
	public boolean contains(Point origin, Point projected) {
		Iterator<Point> itProj = this.projected.iterator();
		Iterator<Point> itOrig = origins.iterator();
		while (itOrig.hasNext()) {
			Point orig = itOrig.next();
			Point proj = itProj.next();

			if (orig.x == origin.x && orig.y == origin.y && projected == null) {
				return true;
			} else if (orig.x == origin.x && orig.y == origin.y && projected.x == proj.x && projected.y == proj.y) {
				return true;
			}
		}
		return false;
	}
}
