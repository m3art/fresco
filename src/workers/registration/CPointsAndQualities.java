/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers.registration;

/**
 *
 * @author Jakub
 */

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author gimli
 */
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


	public void addPoint(Point2D.Double pt, Double q) {
		points.add(pt);
		qualities.add(q);
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

