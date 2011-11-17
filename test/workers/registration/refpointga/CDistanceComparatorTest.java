/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CDistanceComparatorTest {

	/**
	 * Test of compare method, of class CDistanceComparator.
	 */
	@Test
	public void testCompare() {
		System.out.println("compare");
		CPointAndTransformation center = new CPointAndTransformation(null);
		center.setValues(0, 0, 0, 0, 0, 0);

		CPointAndTransformation a = new CPointAndTransformation(null);
		CPointAndTransformation b = new CPointAndTransformation(null);
		CDistanceComparator instance = new CDistanceComparator(center);

		int expResult = 0;
		a.setValues(2, -2, 0, 0, 0, 0);
		b.setValues(2, 2, 0, 0, 0, 0);
		int result = instance.compare(a, b);
		assertEquals(expResult, result);

		expResult = -1;
		a.setValues(1, 1, 0, 0, 0, 0);
		b.setValues(2, 2, 0, 0, 0, 0);
		result = instance.compare(a, b);
		assertEquals(expResult, result);

		expResult = 0;
		a.setValues(5, 12, 0, 0, 0, 0);
		b.setValues(13, 0, 0, 0, 0, 0);
		result = instance.compare(a, b);
		assertEquals(expResult, result);

		expResult = 1;
		a.setValues(5, 12, 0, 0, 0, 0);
		b.setValues(12, 0, 0, 0, 0, 0);
		result = instance.compare(a, b);
		assertEquals(expResult, result);
	}
}
