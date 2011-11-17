/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import workers.segmentation.CSegment;
import workers.segmentation.CSegmentMap;

/**
 * Abstract class for comparison of segment statistics
 *
 * @author gimli
 * @version 26.5.2009
 */
public abstract class CSegmentMetrics {

	private CSegmentMap map;
	/** Input image from which we can compute segment statistics */
	private int[][][] original;

	/**
	 * Compares statistics from segment A and B
	 * @param A input segment
	 * @param B input segment
	 * @return difference between segments
	 */
	public abstract double getDiff(CSegment A, CSegment B);

	public abstract String getMetricName();

	public abstract String getMetricDescription();
}
