/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import java.awt.image.BufferedImage;
import utils.vector.CBasic;

/**
 * Class for covariance count on requested square size
 * @author gimli
 */
public class CCovarianceMetric  extends CAreaSimilarityMetric {

	public CCovarianceMetric(BufferedImage inputA, BufferedImage inputB, double radius, Shape shape) {
		super(inputA, inputB, radius, shape);
	}
public double getValue(int[] inputAValues, int[] inputBValues) {
    double[] doubleInputAValues = new double[inputAValues.length];
    double[] doubleInputBValues = new double[inputBValues.length];

    for (int i = 0; i < inputAValues.length; i++) {
        doubleInputAValues[i] = (double)inputAValues[i];
      
    }
    for (int i = 0; i < inputBValues.length; i++) {
       doubleInputBValues[i] = (double)inputBValues[i];
      }
    return getValue(doubleInputAValues, doubleInputBValues);

  }

	@Override
	public double getValue(double[] a, double[] b) {
		return CBasic.covariance(a,b);
	}
}
