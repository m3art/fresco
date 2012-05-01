/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.vector;

import image.statiscics.CHistogram;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import workers.segmentation.CSegment;
import workers.segmentation.CSegmentMap;

/**
 *
 * @author gimli
 */
public class CHistogramTest {

	final int width = 3, height = 4;
	BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
	WritableRaster pattern;
	int[] pixels = {128, 128, 128,
		32, 45, 200,
		27, 50, 177,
		60, 70, 210,
		80, 80, 200,
		12, 25, 160,
		59, 32, 130,
		70, 60, 111,
		32, 70, 120,
		57, 62, 90,
		34, 38, 139,
		32, 45, 71
	};

	public CHistogramTest() {
		pattern = img.getRaster();

		pattern.setPixels(0, 0, width, height, pixels);

	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	/**
	 * Test of createHistogram method, of class CHistogram.
	 */
	@Test
	public void testGetHistogram_3args() {
		System.out.println("getHistogram");
		int range = 129;
		int band = 0;
		int[] expResult = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 1, 0, 0, 0,
			0, 3, 0, 1, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 1, 0, 1, 1,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 1};
		int[] result = CHistogram.getHistogram(pattern, range, band);

		System.out.println(result.length);

		assertArrayEquals(expResult, result);
	}

	/**
	 * Test of get2DHistogram method, of class CHistogram.
	 */
	@Test
	public void testGet2DHistogram() throws Exception {
		System.out.println("get2DHistogram");
		Raster in1 = null;
		Raster in2 = null;
		int range = 0;
		int band = 0;
		int band2 = 0;
		int[][] expResult = null;
		int[][] result = CHistogram.get2DHistogram(in1, in2, range, band, band2);
		assertEquals(expResult, result);
		fail("The test case is a prototype.");
	}

	/**
	 * Test of oneBandHistogram method, of class CHistogram.
	 */
	@Test
	public void testOneBandHistogram() {
		System.out.println("oneBandHistogram");
		int[] values = {1, 2, 3, 4, 5,
			5, 6, 10, 2, 20,
			12, 23, 13, 17, 1,
			18, 14, 10, 17, 2,
			15, 11, 10, 21, 4};
		int range = 26;
		int[] expResult = {0, 2, 3, 1, 2, 2, 1, 0, 0, 0, 3,
			1, 1, 1, 1, 1, 0, 2, 1, 0, 1,
			1, 0, 1, 0, 0};

		int[] result = CHistogram.oneBandHistogram(values, range);
		assertArrayEquals(expResult, result);
	}

	/**
	 * Test of createHistogram method, of class CHistogram.
	 */
	@Test
	public void testGetHistogram_4args() {
		System.out.println("getHistogram");
		CSegment segment = null;
		CSegmentMap map = null;
		int range = 0;
		int[][][] data = null;
		int[][] expResult = null;
		int[][] result = CHistogram.getHistogram(segment, map, range, data);
		assertEquals(expResult, result);
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getSubHistogram method, of class CHistogram.
	 */
	@Test
	public void testGetSubHistogram() {
		System.out.println("getSubHistogram");
		Raster raster = null;
		Rectangle rect = null;
		int range = 0;
		int band = 0;
		int[] expResult = null;
		int[] result = CHistogram.getSubHistogram(raster, rect, range, band);
		assertEquals(expResult, result);
		fail("The test case is a prototype.");
	}

	/**
	 * Test of get2DSubHistogram method, of class CHistogram.
	 */
	@Test
	public void testGet2DSubHistogram() {
		System.out.println("get2DSubHistogram");
		Raster in1 = null;
		Raster in2 = null;
		Rectangle rect1 = null;
		Rectangle rect2 = null;
		int range = 0;
		int band1 = 0;
		int band2 = 0;
		int[][] expResult = null;
		int[][] result = CHistogram.get2DSubHistogram(in1, in2, rect1, rect2, range, band1, band2);
		assertEquals(expResult, result);
		fail("The test case is a prototype.");
	}

	/**
	 * Test of getMean method, of class CHistogram.
	 */
	@Test
	public void testGetMean() {
		System.out.println("getMean");

		double[] expResult = {51.92, 58.75, 144.67};
		double[] result = CHistogram.getMean(pattern);

		assertArrayEquals(expResult, result, 0.01);
	}

	/**
	 * Test of getVar method, of class CHistogram.
	 */
	@Test
	public void testGetVar() {
		System.out.println("getVar");
		double[] mean = CHistogram.getMean(pattern);;
		double[] expResult = {897.58, 687.69, 1862.89};
		double[] result = CHistogram.getVar(pattern, mean);

		System.out.println(result[0] + " " + result[1] + " " + result[2]);
		assertArrayEquals(expResult, result, 0.01);
	}
}
