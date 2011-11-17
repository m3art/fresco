/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.vector;

/**
 *
 * @author gimli
 */
public class CQuantization {

	public static int[] uniform(int low, int high, int origLow, int origHigh, int[] values) {
		for (int i = 0; i < values.length; i++) {
			// shift to zero value
			values[i] -= -origLow;
			// rescale
			values[i] = values[i] * (high - low) / (origHigh - origLow);
			// shift to new value
			values[i] += low;
		}
		return values;
	}
}
