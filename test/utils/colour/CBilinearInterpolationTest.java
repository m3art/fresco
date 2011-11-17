/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.colour;

import image.colour.CBilinearInterpolation;
import java.awt.geom.Point2D.Double;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CBilinearInterpolationTest {

	@Test
	public void testGetValue() {
		System.out.println("getValue");
		Double position = new Double(50.75, 32.25);
		double leftTop = 200.0;
		double rightTop = 100.0;
		double leftBottom = 0.0;
		double rightBottom = 300.0;
		double expResult = 150;
		double result = CBilinearInterpolation.getValue(position, leftTop, rightTop, leftBottom, rightBottom);
		assertEquals(expResult, result, 0.0);
	}
}
