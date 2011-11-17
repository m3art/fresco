/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils;

import java.awt.geom.Point2D;

/**
 * This generator should offer other than uniform distribution of random numbers
 *
 * @author gimli
 */
public class CRandomGenerator {

	/**
	 * Generates 2D vector with normal distribution centered in
	 * @param mean defines center of normal distribution
	 * @param var defines variance of normal distribution
	 * @return 2D vector from normal distribution with specified params
	 */
	public static Point2D.Double rndGauss2D(Point2D.Double mean, double var) {
		double x1 = Math.random(); // returns number from 0.0 to 1.0
		double x2 = Math.random(); // returns number from 0.0 to 1.0
		double y1 = Math.sqrt(-2 * Math.log(x1)) * Math.cos(2 * Math.PI * x2);
		double y2 = Math.sqrt(-2 * Math.log(x1)) * Math.sin(2 * Math.PI * x2);

		Point2D.Double out = new Point2D.Double(Math.sqrt(var) * y1 + mean.x, Math.sqrt(var) * y2 + mean.y);

		return out;
	}

	/**
	 * Generator of random numbers from normal distribution
	 * @param mean center of Gaussian
	 * @param var variance of normal distribution
	 * @return random variable with Gaussian distribution
	 */
	public static double rndGauss(double mean, double var) {
		double x1 = Math.random(); // returns number from 0.0 to 1.0
		double x2 = Math.random(); // returns number from 0.0 to 1.0
		double out = Math.sqrt(-2 * Math.log(x1)) * Math.cos(2 * Math.PI * x2);

		return mean + (out * var);
	}
}
