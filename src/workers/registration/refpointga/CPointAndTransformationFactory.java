/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.IFitnessOperator;
import java.awt.geom.Point2D;
import utils.CRandomGenerator;

/**
 * Basic factory for genotype creation for GA&lt;CPointAndTransformation&gt;
 *
 * @author gimli
 */
public class CPointAndTransformationFactory {

	/**
	 * Initialize point around defined position. Gaussian distribution.
	 * @param width image width - max value
	 * @param height image height - max value
	 * @param x defines center of random selection
	 * @param y center in vertical
	 * @param var defines variance of random selection
	 * @param fitnessEvaluator sets fitness measure to new individual
	 */
	public static CPointAndTransformation createIdentity(int width, int height, int x, int y, double var, IFitnessOperator<CPointAndTransformation> fitnessEvaluator) {
		CPointAndTransformation individual = new CPointAndTransformation(fitnessEvaluator);

		Point2D.Double posInReal = CRandomGenerator.rndGauss2D(new Point2D.Double(x, y), var);

		int posX = (int) Math.max(width - 1, Math.min(0, posInReal.x));
		int posY = (int) Math.max(height - 1, Math.min(0, posInReal.y));

		double angle = CRandomGenerator.rndGauss(0, 3);
		double dx = CRandomGenerator.rndGauss(0, var);
		double dy = CRandomGenerator.rndGauss(0, var);
		double scale = CRandomGenerator.rndGauss(1, 0.01);

		individual.setValues(posX, posY, dx, dy, angle, scale);

		return individual;
	}

	/**
	 * Constants for random uniform generator are used according to article: Multimodal
	 * genetic algorithms-based algorithm for automatic point correspondence
	 * (2010 - Pattern Recognition)
	 *
	 * @param width range for x position [0, width]
	 * @param height range for y position [0, height]
	 * @param fitnessEvaluator
	 * @return initialized point and transformation coefficients
	 */
	public static CPointAndTransformation createArticleBased(int width, int height, IFitnessOperator<CPointAndTransformation> fitnessEvaluator) {
		CPointAndTransformation individual = new CPointAndTransformation(fitnessEvaluator);

		int x = (int) (Math.random() * width);
		int y = (int) (Math.random() * height);
		double dx = Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2;
		double dy = Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2;
		double angle = Math.random() * CRefPointMarker.ANGLE_RANGE - CRefPointMarker.ANGLE_RANGE / 2;
		double scale = Math.random() * CRefPointMarker.SCALE_RANGE - CRefPointMarker.SCALE_RANGE / 2 + 1;

		individual.setValues(x, y, dx, dy, angle, scale);

		return individual;
	}
}
