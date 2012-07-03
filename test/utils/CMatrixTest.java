/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author gimli
 */
public class CMatrixTest {

	/**
	 * Test of GEM method, of class CMatrix.
	 */
	@Test
	public void testGEM() {
		System.out.println("GEM");
		double[][] input = {{3,1,1},{6,1,0},{0,2,2}};
		double[][] expResult = {{3,0,0},{0,-1,0},{0,0,-2}};
		double[][] result = CMatrix.GEM(input);

		assertArrayEquals(expResult[0], result[0], 0.001);
		assertArrayEquals(expResult[1], result[1], 0.001);
		assertArrayEquals(expResult[2], result[2], 0.001);

		double[][] input2 = {{2,1,1,0},{6,1,3,0},{4,2,2,0},{3,1,4,0}};
		double[][] expResult2 = {{2,0,0,0},{0,-2,0,0},{0,0,2.5,0},{0,0,0,0}};
		double[][] result2 = CMatrix.GEM(input2);

		assertArrayEquals(expResult2[0], result2[0], 0.001);
		assertArrayEquals(expResult2[1], result2[1], 0.001);
		assertArrayEquals(expResult2[2], result2[2], 0.001);
		assertArrayEquals(expResult2[3], result2[3], 0.001);
	}

	@Test
	public void testEigenvalueDecomposition() {
		double[][] input = {{2,0,0},{2,2,1},{1,1,2}};
		Matrix matrix = new Matrix(input);

		EigenvalueDecomposition dec = new EigenvalueDecomposition(matrix);

		double[] result = dec.getRealEigenvalues();
		double[] expResult = {1,2,3};

		Arrays.sort(result);
		assertArrayEquals(expResult, result, 0.001);
	}

	@Test
	public void testReshape() {
		System.out.println("reshape");
		double[][] A = {{1.0, 2.0, 3.0},{4.0, 5.0, 6.0},{7.0, 8.0, 9.0},{10.0, 11.0, 12.0}};
		int m = 2;
		int n = 6;
		double[][] expResult = {{1.0, 3.0, 5.0, 7.0, 9.0, 11.0},{2.0, 4.0, 6.0, 8.0, 10.0, 12.0}};
		double[][] result = CMatrix.reshape(A, m, n);
		assertArrayEquals(expResult, result);

		m = 3;
		n = 4;
		expResult = new double[][]{{1.0, 4.0, 7.0, 10.0},{2.0, 5.0, 8.0, 11.0}, {3.0, 6.0, 9.0, 12.0}};
		result = CMatrix.reshape(A, m, n);
		assertArrayEquals(expResult, result);
	}
}
