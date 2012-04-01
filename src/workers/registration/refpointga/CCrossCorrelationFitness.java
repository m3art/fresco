/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.IFitnessOperator;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import utils.metrics.CAreaSimilarityMetric.Shape;
import utils.metrics.CCrossCorrelationMetric;

/**
 *
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CCrossCorrelationFitness implements IFitnessOperator<CPointAndTransformation> {

	/** Area of cross-correlation computation */
	final int sampleRadius;
	private CCrossCorrelationMetric metric;
	private static final Logger logger = Logger.getLogger(CCrossCorrelationFitness.class.getName());

	public CCrossCorrelationFitness(BufferedImage src, BufferedImage ref, int radius) {
		sampleRadius = radius;
		if (src != null && ref != null) {
			setSourceImages(src, ref);
		}
	}

	public final void setSourceImages(BufferedImage src, BufferedImage ref) {
		metric = new CCrossCorrelationMetric(src, ref, sampleRadius, Shape.CIRCULAR);
	}

	/**
	 * sum_{x,y in G}(((Ir(x,y) - avgIr)(Ic(x,y) - avgIc))^2)/
	 * sum_{x,y in G}((Ir(x,y) - avgIr)^2) * ((Ic(x,y) - avgIc)^2)
	 * @param individual center of projection
	 * @return fitness in center of projection
	 */
	@Override
	public double getFitness(CPointAndTransformation individual) {
		return metric.getDistance(individual);
	}
}
