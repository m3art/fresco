/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import java.awt.Point;
import workers.segmentation.CSegment;
import utils.vector.CBasic;

/**
 * @author gimli
 * @version 26.5.2009
 */
public class CEuclidMetrics extends CSegmentMetrics {

	public static double distance(Point a, Point b) {
		return Math.sqrt(Math.pow((a.x - b.x), 2) + Math.pow((a.y - b.y), 2));
	}

	@Override
	public double getDiff(CSegment A, CSegment B) {
		if (A.getColor() == null || B.getColor() == null) {
			throw new UnsupportedOperationException("Not supported yet.");
		} else {
			return CBasic.norm(CBasic.diff(A.getColor(), B.getColor()));
		}
	}

	@Override
	public String getMetricName() {
		return "Euclidean matric.";
	}

	@Override
	public String getMetricDescription() {
		return "Euclidean matric of color means. "
				+ "It counts difference as Diff = r*r + g*g + b*b";
	}
}
