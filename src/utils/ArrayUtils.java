/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils;

/**
 *
 * @author gimli
 */
public class ArrayUtils {

	public static double[] toPrimitive(Double[] array) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return new double[0];
		}
		final double[] result = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			result[i] = array[i].doubleValue();
		}
		return result;
	}
}
