/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction.colorshift;

/**
 * Data structure for storing mean colour and its variance
 * @author gimli
 */
public class MeanAndVar {

	public double[] mean = new double[3];
	public double[] var = new double[3];
}
