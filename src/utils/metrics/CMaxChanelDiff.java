/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import workers.segmentation.CSegment;

/**
 * @author gimli
 * @version 26.5.2009
 */
public class CMaxChanelDiff extends CSegmentMetrics {

	@Override
	public double getDiff(CSegment A, CSegment B) {
		if (A.getColor() == null || B.getColor() == null) {
			throw new UnsupportedOperationException("Not supported yet.");
		} else {
			int i, diff, max = -1;
			for (i = 0; i < A.getColor().length; i++) {
				diff = Math.abs(A.getColor()[i] - B.getColor()[i]);
				if (diff > max) {
					max = diff;
				}
			}
			return max;
		}

	}

	@Override
	public String getMetricName() {
		return "Maximal chanel matric";
	}

	@Override
	public String getMetricDescription() {
		return "It computes maximum difference between red, green and blue color."
				+ "i.e. for colors [20, 30, 50] and [45, 80, 101] returns 51";
	}
}
