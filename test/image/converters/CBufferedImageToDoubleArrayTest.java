/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CBufferedImageToDoubleArrayTest {

	/**
	 * Test of convert method, of class CBufferedImageToDoubleArray.
	 */
	@Test
	public void testConvert() {
		System.out.println("convert");
		BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_3BYTE_BGR);

		WritableRaster raster = image.getRaster();

		double[][][] expResult = {new double[][]{new double[]{1, 1, 1},
				new double[]{2, 2, 2},
				new double[]{3, 3, 3}},
			new double[][]{new double[]{4, 4, 4},
				new double[]{5, 5, 5},
				new double[]{6, 6, 6}},
			new double[][]{new double[]{7, 7, 7},
				new double[]{8, 8, 8},
				new double[]{9, 9, 9}}};

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				raster.setPixel(x, y, expResult[x][y]);
			}
		}

		double[][][] result = CBufferedImageToDoubleArray.convert(image);

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int b = 0; b < 3; b++) {
					assertEquals(expResult[x][y][b], result[x][y][b], 0);
				}
			}
		}
	}

	/**
	 * Test of inverse method, of class CBufferedImageToDoubleArray.
	 */
	@Test
	public void testInverse() {
		System.out.println("inverse");
		double[][][] data = {new double[][]{new double[]{1, 1, 1},
				new double[]{2, 2, 2},
				new double[]{3, 3, 3}},
			new double[][]{new double[]{4, 4, 4},
				new double[]{5, 5, 5},
				new double[]{6, 6, 6}},
			new double[][]{new double[]{7, 7, 7},
				new double[]{8, 8, 8},
				new double[]{9, 9, 9}}};

		BufferedImage expResult = new BufferedImage(3, 3, BufferedImage.TYPE_3BYTE_BGR);

		WritableRaster raster = expResult.getRaster();

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				raster.setPixel(x, y, data[x][y]);
			}
		}

		BufferedImage result = CBufferedImageToDoubleArray.inverse(data);
		assertEquals(expResult, result);
	}
}