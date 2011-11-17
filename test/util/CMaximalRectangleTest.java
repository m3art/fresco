/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package util;

import utils.geometry.CMaximalRectangle;
import java.awt.Point;
import java.awt.Rectangle;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CMaximalRectangleTest {

	public CMaximalRectangleTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of getRect method, of class CMaximalRectangle.
	 */
	@Test
	public void testGetRect() {
		System.out.println("getRect");
		Point center = new Point(20, 20);
		int range = 0;
		Rectangle boundary = new Rectangle(0, 0, 100, 100);
		Rectangle expResult = new Rectangle(20, 20, 1, 1);
		Rectangle result = CMaximalRectangle.getRect(center, range, boundary);
		assertEquals(expResult, result);

		center = new Point(20, 20);
		range = 1;
		boundary = new Rectangle(0, 0, 100, 100);
		expResult = new Rectangle(19, 19, 3, 3);
		result = CMaximalRectangle.getRect(center, range, boundary);
		assertEquals(expResult, result);
	}
}