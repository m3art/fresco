/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import image.converters.Crgb2grey;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.vector.CBasic;

/**
 *
 * @author gimli
 */
public class CCrossCorrelationMetric extends CAreaSimilarityMetric {

  private static final Logger logger = Logger.getLogger(CCrossCorrelationMetric.class.getName());

  public CCrossCorrelationMetric(BufferedImage inputA, BufferedImage inputB, double radius, Shape shape) {
    super(Crgb2grey.oneBandImage(inputA), Crgb2grey.oneBandImage(inputB), radius, shape);
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
  public double getValue(double[] inputAValues, double[] inputBValues) {
    double crossVar = 0, varSrc = 0, varRef = 0;
    long start = System.currentTimeMillis();
    double meanA = CBasic.mean(inputAValues);
    double meanB = CBasic.mean(inputBValues);
    long meanTime = System.currentTimeMillis() - start;

    start = System.currentTimeMillis();
    for (int i = 0; i < inputAValues.length; i++) {

      crossVar += (inputAValues[i] - meanA) * (inputBValues[i] - meanB);
      varSrc += Math.pow(inputAValues[i] - meanA, 2);
      varRef += Math.pow(inputBValues[i] - meanB, 2);
    }
    logger.log(Level.FINEST, "Mean time: {0}ms, CrossVar time: {1}ms", new Object[]{meanTime, System.currentTimeMillis() - start});

    if (crossVar != 0 && varSrc != 0 && varRef != 0) {
      return Math.pow(crossVar, 2) / (varSrc * varRef);
    } else {
      return 0;
    }
  }
}
