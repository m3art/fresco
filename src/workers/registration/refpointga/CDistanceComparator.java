/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import java.util.Comparator;
import utils.metrics.CEuclidMetrics;

/**
 * Comparator based on Euclidian metrics
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CDistanceComparator implements Comparator<CPointAndTransformation> {

	/** Referring point for comparison */
	private final CPointAndTransformation center;

	/**
	 * Comparator order array according to euclidian distance from @param center
	 */
	public CDistanceComparator(CPointAndTransformation center) {
		this.center = center;
	}

	/**
	 * Compares distance from @param center
	 * @param a first competitor
	 * @param b second competitor
	 * @return 1 if a is closer to center, -1 if b is closer and 0 otherwise
	 */
	public int compare(CPointAndTransformation a, CPointAndTransformation b) {
		double aDistance = CEuclidMetrics.distance(center.getPosition(), a.getPosition());
		double bDistance = CEuclidMetrics.distance(center.getPosition(), b.getPosition());

		if (aDistance < bDistance) {
			return -1;
		} else if (aDistance == bDistance) {
			return 0;
		} else {
			return 1;
		}
	}
}
