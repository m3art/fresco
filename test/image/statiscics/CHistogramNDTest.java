/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.statiscics;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CHistogramNDTest {

	@Test
	public void testGetBinNumber() {
		System.out.println("getBinNumber");
		int[] binNumbers = new int[]{3,5,9};
		int[] bins = new int[]{10,10,10};
		int expResult = 953;
		int result = CHistogramND.getBinNumber(binNumbers, bins);
		assertEquals(expResult, result);

		binNumbers = new int[]{3,5,11};
		bins = new int[]{10,10,10};
		try {
			CHistogramND.getBinNumber(binNumbers, bins);
			fail("Number of bin out of bounds, but values accepted.");
		} catch (AssertionError ae) {
			// OK
		}
	}

	@Test
	public void testCreateHistogram() {
		System.out.println("createHistogram");
		double[][] inputValues = new double[][] {
			{1,1,1,1,2,2,2,2,3,3,3,2,2,2,2,2,2,3,1,2},
			{1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,3,3},
		};
		int[] bins = new int[]{3,3};
		CHistogramND result = CHistogramND.createHistogram(inputValues, bins);

		assertEquals(2, result.dimensions);
		assertEquals(20, result.values);
		assertArrayEquals(new double[]{1,1}, result.min, CHistogramND.EPSILON);
		assertArrayEquals(new double[]{3,3}, result.max, CHistogramND.EPSILON);
		assertArrayEquals(new double[]{2.0/3,2.0/3}, result.binSize, CHistogramND.EPSILON);
		assertArrayEquals(new int[]{4,4,3,0,6,1,1,1,0}, result.binContent);
		assertEquals(-2*4*4.0/20*Math.log(4.0/20) - 3*3.0/20*Math.log(3.0/20) - 6*6.0/20*Math.log(6.0/20) - 3*1.0/20*Math.log(1.0/20), result.entropy, CHistogramND.EPSILON);
	}
}
