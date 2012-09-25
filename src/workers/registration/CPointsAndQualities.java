/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

/**
 *
 * @author Jakub
 */

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;

public class CPointsAndQualities {
  

	LinkedList<Point2D.Double> points = new LinkedList<Point2D.Double>();
	LinkedList<Double> qualities = new LinkedList<Double>();

	public CPointsAndQualities() {
		
	}

	public CPointsAndQualities(LinkedList<Point2D.Double> pts, LinkedList<Double> qs) {
		assert (points.size() == qs.size());
		this.points = pts;
		this.qualities = qs;
	}

	public LinkedList<Point2D.Double> getPoints() {
		return points;
	}

	public LinkedList<Double> getQualities() {
		return qualities;
	}
  
  public Point2D.Double getPoint(int i) {
		return points.get(i);
	}
	public Double getQuality(int i) {
		return qualities.get(i);
	}
  
  public void removePtq(int i) {
    if (i > size()) i = size();
    points.remove(i);
    qualities.remove(i);
  }


	public void addPoint(Point2D.Double pt, Double q) {
		points.add(pt);
		qualities.add(q);
	}

	
	public boolean contains(Point2D.Double input) {
		Iterator<Point2D.Double> itPts = this.points.iterator();
		while (itPts.hasNext()) {
			Point2D.Double orig = itPts.next();
			if ((orig.x == input.x) && (orig.y == input.y)) {
				return true;
      }
    }
    return false;
	}

	public int size() {
		return points.size();
	}
}

