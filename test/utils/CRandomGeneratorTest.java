/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils;

import java.awt.geom.Point2D.Double;
import org.junit.Test;

/**
 *
 * @author gimli
 */
public class CRandomGeneratorTest {

	final int INTERVALS = 80, RANDOMS = 1000000;

	/**
	 * Test of rndGauss2D method, of class CRandomGenerator.
	 */
	@Test
	public void testRndGauss2D() {
		System.out.println("rndGauss2D");
		Double mean = new Double(0, 0);
		double var = 1.0;
		int[][] hist = new int[INTERVALS][INTERVALS];
		Double point;

		for (int i = 0; i < RANDOMS * 100; i++) {
			point = CRandomGenerator.rndGauss2D(mean, var);
			point.x = (int) Math.round(point.x * 10 + 40);
			point.y = (int) Math.round(point.y * 10 + 40);
			if (point.x < 80 && point.x >= 0 && point.y >= 0 && point.y < 80) {
				hist[(int) point.x][(int) point.y]++;
			}
		}

		for (int i = 0; i < INTERVALS; i++) {
			System.out.print("c("); // export to r type of array
			for (int j = 0; j < INTERVALS; j++) {
				System.out.print(hist[i][j] + ", ");
			}
			System.out.print("), ");
		}
		System.out.println();
	}

	/**
	 * Test of rndGauss method, of class CRandomGenerator.
	 */
	@Test
	public void testRndGauss() {
		System.out.println("rndGauss");
		double mean = 0.0;
		double var = 1.0;

		int[] hist = new int[INTERVALS];
		int value;

		for (int i = 0; i < RANDOMS; i++) {
			value = 40 + (int) Math.round(CRandomGenerator.rndGauss(mean, var) * 10);
			if (value < 80 && value >= 0) {
				hist[value]++;
			}
		}

		for (int i = 0; i < INTERVALS; i++) {
			System.out.print(hist[i] + ", ");
		}
		System.out.println();
	}
}
