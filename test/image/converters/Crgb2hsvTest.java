package image.converters;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author gimli
 */
public class Crgb2hsvTest {

	@Test
	public void testConversion() {
		System.out.println("inverse");
		double[] rgb = {0x80, 0x12, 0xea};
		double[] hsv = {270.556, 235.385, 234.00};
		double[] expResult = rgb;
		double[] hsvResult = Crgb2hsv.convert(rgb);
		double[] result = Crgb2hsv.inverse(hsv);

		System.out.println(hsvResult[0]+", "+hsvResult[1]+", "+hsvResult[2]);
		System.out.println(result[0]+", "+result[1]+", "+result[2]);

		assertArrayEquals(hsv, hsvResult, 0.1);
		assertArrayEquals(expResult, result, 0.1);
	}
}
