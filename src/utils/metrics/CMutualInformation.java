/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import image.statistics.CHistogram;
import image.statistics.CHistogramND;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author gimli
 * @version May 1, 2012
 */
public class CMutualInformation extends CAreaSimilarityMetric {

	private static final Logger logger = Logger.getLogger(CMutualInformation.class.getName());

	public CMutualInformation(BufferedImage inputA, BufferedImage inputB, double radius, Shape shape) {
		super(inputA, inputB, radius, shape);
	}

	@Override
	protected double getValue(double[] inputAValues, double[] inputBValues) {

		long start = System.currentTimeMillis();
		CHistogram histA = CHistogram.createHistogram(inputAValues, (int)radius);
		logger.log(Level.FINER, "Histogram A created: {0}. Entropy: {1}", new Object[]{(System.currentTimeMillis()-start), histA.entropy});

		start = System.currentTimeMillis();
		CHistogram histB = CHistogram.createHistogram(inputBValues, (int)radius);
		logger.log(Level.FINER, "Histogram B created: {0}. Entropy: {1}", new Object[]{(System.currentTimeMillis()-start), histB.entropy});

		start = System.currentTimeMillis();
		double[][] valuesAB = new double[2][];
		valuesAB[0] = inputAValues;
		valuesAB[1] = inputBValues;
		CHistogramND histAB = CHistogramND.createHistogram(valuesAB, new int[]{(int)radius, (int)radius});
		logger.log(Level.FINER, "Histogram AB created: {0}. Entropy: {1}", new Object[]{(System.currentTimeMillis()-start), histAB.entropy});

		return histA.entropy + histB.entropy - histAB.entropy;
	}

}
