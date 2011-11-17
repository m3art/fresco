/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.vector;

import utils.vector.CConvolution;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CMatrixTest {

	public CMatrixTest() {
	}

	/**
	 * Test of convolution method, of class CConvolution.
	 */
	@Test
	public void testConvolution() {
		System.out.println("convolution");
		double[][] matrix = {{1, 1, 1, 1, 1}, {1, 1, 1, 1, 1}, {1, 1, 1, 1, 1}, {1, 1, 1, 1, 1}};
		double[][] filter = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
		double[][] expResult = {{4, 6, 6, 6, 4}, {6, 9, 9, 9, 6}, {6, 9, 9, 9, 6}, {4, 6, 6, 6, 4}};
		double[][] result = CConvolution.convolution(matrix, filter);
		assertEquals(expResult, result);
	}
}
