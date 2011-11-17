/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.IFitnessOperator;
import image.converters.Crgb2gray;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import image.colour.CBilinearInterpolation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CCrossCorrelationFitness implements IFitnessOperator<CPointAndTransformation> {

	/**
	 * Input images. Source is transformed to reference image
	 */
	private double[][] source, reference;
	/** Area of cross-correlation computation */
	final int sampleRadius;
	private static final Logger logger = Logger.getLogger(CCrossCorrelationFitness.class.getName());

	public CCrossCorrelationFitness(BufferedImage src, BufferedImage ref, int radius) {
		if (src != null && ref != null) {
			setSourceImages(src, ref);
		}
		sampleRadius = radius;
	}

	public final void setSourceImages(BufferedImage src, BufferedImage ref) {
		source = Crgb2gray.convertImage(src.getData());
		reference = Crgb2gray.convertImage(ref.getData());
	}

	/**
	 * sum_{x,y in G}(((Ir(x,y) - avgIr)(Ic(x,y) - avgIc))^2)/
	 * sum_{x,y in G}((Ir(x,y) - avgIr)^2) * ((Ic(x,y) - avgIc)^2)
	 * @param individual center of projection
	 * @return fitness in center of projection
	 */
	public double getFitness(CPointAndTransformation individual) {
		double sourceAverage = 0, referenceAverage = 0;
		double[] srcValues = new double[(int) Math.pow(sampleRadius * 2 + 1, 2)];
		double[] refValues = new double[(int) Math.pow(sampleRadius * 2 + 1, 2)];
		Point center = individual.getPosition();

		int j = 0;
		for (int dx = -sampleRadius; dx <= sampleRadius; dx++) {
			for (int dy = -sampleRadius; dy <= sampleRadius; dy++) {
				Point2D.Double proj = individual.getProjection(center.x + dx, center.y + dy);

				if (center.x + dx < 0 || center.x + dx > source.length - 1
						|| center.y + dy < 0 || center.y + dy > source[0].length - 1) {
					continue;
				}

				srcValues[j] = source[dx + center.x][dy + center.y];

				sourceAverage += source[dx + center.x][dy + center.y];

				if ((int) proj.x < 0 || (int) (proj.x) + 1 > reference.length - 1
						|| (int) proj.y < 0 || (int) (proj.y) + 1 > reference[0].length - 1) {
					continue;
				}

				refValues[j] = CBilinearInterpolation.getValue(proj, reference[(int) proj.x][(int) proj.y],
						reference[(int) proj.x + 1][(int) proj.y],
						reference[(int) proj.x][(int) proj.y + 1],
						reference[(int) proj.x + 1][(int) proj.y + 1]);

				referenceAverage += refValues[j];
				j++;
			}
		}

		sourceAverage /= Math.pow(sampleRadius * 2 + 1, 2);
		logger.log(Level.FINEST, "Source avg: {0}", sourceAverage);
		referenceAverage /= Math.pow(sampleRadius * 2 + 1, 2);
		logger.log(Level.FINEST, "Reference avg: {0}", referenceAverage);

		double crossVar = 0, varSrc = 0, varRef = 0;

		for (int i = 0; i < srcValues.length; i++) {

			crossVar += (srcValues[i] - sourceAverage) * (refValues[i] - referenceAverage);
			varSrc += Math.pow(srcValues[i] - sourceAverage, 2);
			varRef += Math.pow(refValues[i] - referenceAverage, 2);
		}

		logger.log(Level.FINEST, "Crossvar: {0}", crossVar);
		logger.log(Level.FINEST, "VarSrc: {0} varRef: {1}", new Object[]{varSrc, varRef});

		if (crossVar != 0 && varSrc != 0 && varRef != 0) {
			return Math.pow(crossVar, 2) / (varSrc * varRef);
		} else {
			return 0;
		}
	}
}
