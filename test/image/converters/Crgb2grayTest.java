/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.DataBufferDouble;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class Crgb2grayTest {

	@Test
	public void testConvert() {
		System.out.println("convert");
		int[] rgb = {128, 128, 128};
		int[] expResult = {128, 128, 128};
		int[] result = Crgb2grey.convert(rgb);
		assertArrayEquals(expResult, result);

		rgb[0] = rgb[1] = 0;
		rgb[2] = 255;
		result = Crgb2grey.convert(rgb);
		expResult[0] = expResult[1] = expResult[2] = 28;
		assertArrayEquals(expResult, result);

		rgb[0] = 115;
		rgb[1] = 26;
		rgb[2] = 155;
		result = Crgb2grey.convert(rgb);
		expResult[0] = expResult[1] = expResult[2] = 67;
		assertArrayEquals(expResult, result);
	}

	@Test
	public void testInverse() {
		System.out.println("inverse");
		int gray = 0;
		int[] expResult = {0, 0, 0};
		int[] result = Crgb2grey.inverse(gray);
		assertArrayEquals(expResult, result);
	}

	@Test
	public void testConvertPixel() {
		System.out.println("convertPixel");
		int[] rgb = {233, 12, 100};
		Crgb2grey instance = new Crgb2grey();
		int[] expResult = {88, 88, 88};
		int[] result = instance.convertPixel(rgb);
		assertArrayEquals(expResult, result);
	}

	/**
	 * Test of convertImage method, of class Crgb2grey.
	 */
	@Test
	public void testConvertImage() {
		System.out.println("convertImage");
		DataBufferDouble pixels = new DataBufferDouble(9, 3);

		pixels.setElem(0, 0, 100);
		pixels.setElem(1, 0, 150);
		pixels.setElem(2, 0, 80);
		pixels.setElem(0, 1, 75);
		pixels.setElem(1, 1, 49);
		pixels.setElem(2, 1, 12);
		pixels.setElem(0, 2, 10);
		pixels.setElem(1, 2, 90);
		pixels.setElem(2, 2, 190);
		pixels.setElem(0, 3, 134);
		pixels.setElem(1, 3, 220);
		pixels.setElem(2, 3, 255);
		pixels.setElem(0, 4, 101);
		pixels.setElem(1, 4, 159);
		pixels.setElem(2, 4, 230);
		pixels.setElem(0, 5, 20);
		pixels.setElem(1, 5, 103);
		pixels.setElem(2, 5, 34);
		pixels.setElem(0, 6, 120);
		pixels.setElem(1, 6, 143);
		pixels.setElem(2, 6, 211);
		pixels.setElem(0, 7, 50);
		pixels.setElem(1, 7, 63);
		pixels.setElem(2, 7, 91);
		pixels.setElem(0, 8, 17);
		pixels.setElem(1, 8, 77);
		pixels.setElem(2, 8, 177);

		Raster rgbRaster = Raster.createRaster(new BandedSampleModel(DataBuffer.TYPE_DOUBLE, 3, 3, 3), pixels, null);

		double[][] expResult = {{127.3, 198.05, 143.58}, {52.73, 149.41, 62.18}, {77, 70.51, 70}};
		double[][] result = Crgb2grey.convertImage(rgbRaster);
		assertArrayEquals(expResult[0], result[0], 0.01);
		assertArrayEquals(expResult[1], result[1], 0.01);
		assertArrayEquals(expResult[2], result[2], 0.01);
	}

	/**
	 * Test of convertToOneValue method, of class Crgb2grey.
	 */
	@Test
	public void testConvertToOneValue_intArr() {
		System.out.println("convertToOneValue");
		int[] rgb = {10, 20, 30};
		int expResult = 18;
		int result = Crgb2grey.convertToOneValue(rgb);
		assertEquals(expResult, result);
	}

	/**
	 * Test of convertToOneValue method, of class Crgb2grey.
	 */
	@Test
	public void testConvertToOneValue_doubleArr() {
		System.out.println("convertToOneValue");
		double[] rgb = {30.2, 27.1, 255};
		double expResult = 53.099;
		double result = Crgb2grey.convertToOneValue(rgb);
		assertEquals(expResult, result, 0.01);
	}
}