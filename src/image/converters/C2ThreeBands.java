/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

/**
 *
 * @author Gimli
 */
public class C2ThreeBands implements IConstants {

	public static int[] convert(int[] input) {
		int length = input.length;
		int i;
		int[] rgb = new int[rgb_bands];

		switch (length) {
			case 1:
				for (i = 0; i < rgb_bands; i++) {
					rgb[i] = input[0];
				}
		}

		return rgb;
	}
}
