/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.tools;

import org.junit.Test;
import static org.junit.Assert.*;
import workers.tools.CFft.CComplexNumber;

/**
 * @author gimli
 */
public class CFftTest {

	CComplexNumber[] x = {new CComplexNumber(0),
		new CComplexNumber(1, 1),
		new CComplexNumber(2, 3),
		new CComplexNumber(4, -2)};
	CComplexNumber[] y = {new CComplexNumber(7, 2),
		new CComplexNumber(1),
		new CComplexNumber(-3, 4),
		new CComplexNumber(-5, -6)};

	public CFftTest() {
	}

	@Test
	public void testFft() {
		System.out.println("fft");
		CComplexNumber[] expResult = {new CComplexNumber(7, 2),
			new CComplexNumber(1),
			new CComplexNumber(-3, 4),
			new CComplexNumber(-5, -6)};
		CComplexNumber[] result = CFft.fft(x);

		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i].real, expResult[i].real, 0.00000001);
			assertEquals(result[i].imag, expResult[i].imag, 0.00000001);
		}
	}

	@Test
	public void testIfft() {
		System.out.println("ifft");
		CComplexNumber[] expResult = {new CComplexNumber(0),
			new CComplexNumber(1, 1),
			new CComplexNumber(2, 3),
			new CComplexNumber(4, -2)};
		CComplexNumber[] result = CFft.ifft(y);
		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i].real, expResult[i].real, 0.00000001);
			assertEquals(result[i].imag, expResult[i].imag, 0.00000001);
		}
	}

	@Test
	public void testRadialConvolution() {
		System.out.println("convolution");

		CComplexNumber[] expResult = {new CComplexNumber(-13, -14),
			new CComplexNumber(9, 4),
			new CComplexNumber(-23, 12),
			new CComplexNumber(27, -2)};
		CComplexNumber[] result = CFft.radialConvolution(x, y);
		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i].real, expResult[i].real, 0.00000001);
			assertEquals(result[i].imag, expResult[i].imag, 0.00000001);
		}
	}

	@Test
	public void testConvolve() {
		System.out.println("convolve");

		CComplexNumber[] expResult = {new CComplexNumber(0),
			new CComplexNumber(5, 9),
			new CComplexNumber(9, 26),
			new CComplexNumber(27, -2),
			new CComplexNumber(-13, -14),
			new CComplexNumber(4, -5),
			new CComplexNumber(-32, -14),
			new CComplexNumber(0, 0)};
		CComplexNumber[] result = CFft.convolve(x, y);
		for (int i = 0; i < result.length; i++) {
			assertEquals(result[i].real, expResult[i].real, 0.00000001);
			assertEquals(result[i].imag, expResult[i].imag, 0.00000001);
		}
	}
}