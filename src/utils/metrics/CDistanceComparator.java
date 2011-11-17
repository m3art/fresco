/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import java.util.Comparator;

/**
 * @author gimli
 * @version 16.6.2009
 */
public class CDistanceComparator implements Comparator<Double> {

	double center;

	public CDistanceComparator(double center) {
		this.center = center;
	}

	public int compare(Double o1, Double o2) {
		double a = Math.abs(o1.doubleValue() - center),
				b = Math.abs(o2.doubleValue() - center);

		if (a > b) {
			return 1;
		} else if (a == b) {
			return 0;
		} else {
			return -1;
		}
	}

	public void setCenter(double o) {
		center = o;
	}
}
