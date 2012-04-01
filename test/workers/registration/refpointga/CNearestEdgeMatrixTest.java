/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import java.awt.geom.Point2D;
import image.CBinaryImage;
import java.awt.Point;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CNearestEdgeMatrixTest {

	static CNearestEdgeMatrix nem;

	@BeforeClass
	public static void setUpClass() throws Exception {
		CBinaryImage bw = new CBinaryImage(10, 10);

		bw.setValue(3, 0, true);
		bw.setValue(3, 1, true);
		bw.setValue(4, 2, true);
		bw.setValue(4, 3, true);
		bw.setValue(4, 4, true);
		bw.setValue(5, 5, true);
		bw.setValue(6, 5, true);
		bw.setValue(7, 5, true);
		bw.setValue(7, 4, true);
		bw.setValue(8, 3, true);

		nem = new CNearestEdgeMatrix(bw);
	}

	/**
	 * Test of getNearest method, of class CNearestEdgeMatrix.
	 */
	@Test
	public void testGetNearest_Point() {
		System.out.println("getNearest");
		Point2D.Double p = new Point2D.Double(0, 0);
		Point2D.Double expResult = new Point2D.Double(3, 0);
		Point2D.Double result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 9;
		p.y = 9;
		expResult = new Point2D.Double(7, 5);
		result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 4;
		p.y = 7;
		expResult = new Point2D.Double(5, 5);
		result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 2;
		p.y = 4;
		expResult = new Point2D.Double(4, 4);
		result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 7;
		p.y = 3;
		expResult = new Point2D.Double(7, 4);
		result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 7;
		p.y = 0;
		expResult = new Point2D.Double(8, 3);
		result = nem.getNearest(p);
		assertEquals(expResult, result);

		p.x = 3;
		p.y = 1;
		expResult = new Point2D.Double(3, 1);
		result = nem.getNearest(p);
		assertEquals(expResult, result);
	}

	/**
	 * Test of getNearest method, of class CNearestEdgeMatrix.
	 */
	@Test
	public void testGetNearest_int_int() {
		System.out.println("getNearest");
		int x = 0;
		int y = 0;
		Point2D.Double expResult = new Point2D.Double(3, 0);
		Point2D.Double result = nem.getNearest(x, y);
		assertEquals(expResult, result);

		x = 2;
		y = 9;
		expResult = new Point2D.Double(5, 5);
		result = nem.getNearest(2, 9);
		assertEquals(expResult, result);

		x = 7;
		y = 7;
		expResult = new Point2D.Double(7, 5);
		result = nem.getNearest(x, y);
		assertEquals(expResult, result);

		x = 5;
		y = 3;
		expResult = new Point2D.Double(4, 3);
		result = nem.getNearest(x, y);
		assertEquals(expResult, result);

		x = 8;
		y = 1;
		expResult = new Point2D.Double(8, 3);
		result = nem.getNearest(x, y);
		assertEquals(expResult, result);

		x = 1;
		y = 0;
		expResult = new Point2D.Double(3, 0);
		result = nem.getNearest(x, y);
		assertEquals(expResult, result);

		x = 5;
		y = 4;
		expResult = new Point2D.Double(4, 4);
		result = nem.getNearest(x, y);
		assertEquals(expResult, result);
	}
}
