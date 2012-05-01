/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import java.awt.image.BufferedImage;

/**
 *
 * @author gimli
 */
public class CMutualInformationMetric extends CAreaSimilarityMetric {

	/**
	 * Constructor must obtain two images with the same size in gray scale
	 * NOTE: probably three band images are not necessary
	 * @param input1
	 * @param input2
	 * @throws java.io.IOException if the size of image not correspond
	 */
	public CMutualInformationMetric(final BufferedImage input1, final BufferedImage input2, int range, Shape shape) {
		super(input1, input2, range, shape);
	}

	@Override
	protected double getValue(double[] inputAValues, double[] inputBValues) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
