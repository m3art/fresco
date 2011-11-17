/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.geom.Point2D.Double;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CAffineTransformationTest {

	/**
	 * Test of getProjected method, of class CAffineTransformation.
	 */
	@Test
	public void testGetProjected_Point2DDouble() {
		System.out.println("rotation 90 degrees");
		Double origin = new Double(1.0, 0.0);
		CAffineTransformation instance = new CAffineTransformation();
		instance.setParameters(0, 0, Math.PI / 2, 1, 1);

		Double expResult = new Double(0.0, 1.0);
		Double result = instance.getProjected(origin);
		assertEquals(expResult.x, result.x, 0.01);
		assertEquals(expResult.y, result.y, 0.01);
	}

	/**
	 * Test of getProjected method, of class CAffineTransformation.
	 */
	@Test
	public void testGetProjected_double_double() {
		System.out.println("rotation 90 degrees, scale 2");
		double x = 1.0;
		double y = 0.5;
		CAffineTransformation instance = new CAffineTransformation();
		instance.setParameters(0, 0, Math.PI / 2, 2, 2);

		Double expResult = new Double(-1, 2);
		Double result = instance.getProjected(x, y);
		assertEquals(expResult.x, result.x, 0.01);
		assertEquals(expResult.y, result.y, 0.01);
	}

	/**
	 * Test of getProjected method, of class CAffineTransformation.
	 */
	@Test
	public void testGetProjected_7args() {
		System.out.println("rotation 180 degrees, shift [2,5]");
		double x = 1.0;
		double y = 0.0;
		double shiftX = 2.0;
		double shiftY = 5.0;
		double angle = Math.PI;
		double scaleX = 1.0;
		double scaleY = 1.0;
		Double expResult = new Double(1, 5);
		Double result = CAffineTransformation.getProjected(x, y, shiftX, shiftY, angle, scaleX, scaleY);
		assertEquals(expResult.x, result.x, 0.01);
		assertEquals(expResult.y, result.y, 0.01);

		System.out.println("rotation 270 degrees, shift [-2,1], scale [1.5, 3]");
		x = 1.0;
		y = 0.0;
		shiftX = -2.0;
		shiftY = 1.0;
		angle = Math.PI * 3 / 2;
		scaleX = 1.5;
		scaleY = 3.0;
		expResult = new Double(-2, -2);
		result = CAffineTransformation.getProjected(x, y, shiftX, shiftY, angle, scaleX, scaleY);
		assertEquals(expResult.x, result.x, 0.01);
		assertEquals(expResult.y, result.y, 0.01);
	}

	/**
	 * Test of getInversion method, of class CAffineTransformation.
	 */
	@Test
	public void testGetInversion_double_double() {
//		System.out.println("getInversion");
//		double x = 0.0;
//		double y = 0.0;
//		CAffineTransformation instance = new CAffineTransformation();
//		Double expResult = null;
//		Double result = instance.getInversion(x, y);
//		assertEquals(expResult, result);
		fail("Not yet implemented.");
	}

	/**
	 * Test of getInversion method, of class CAffineTransformation.
	 */
	@Test
	public void testGetInversion_Point2DDouble() {
//		System.out.println("getInversion");
//		Double projection = null;
//		CAffineTransformation instance = new CAffineTransformation();
//		Double expResult = null;
//		Double result = instance.getInversion(projection);
//		assertEquals(expResult, result);
		fail("Not yet implemented.");
	}
}
