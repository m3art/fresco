/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.ICrossOverOperator;

/**
 * Generates new PointAndTransformation from two predecessors. There is necessary
 * to set @param alpha. Position of first child is at average coordinates weighted
 * by alpha. For second child is order of predecessors inverted. Alpha is constant.
 *
 * @author gimli
 */
public class CPointCrossing implements ICrossOverOperator<CPointAndTransformation> {

	private int childParity = 1;

	public CPointAndTransformation breed(CPointAndTransformation father, CPointAndTransformation mother) {
		CPointAndTransformation child = new CPointAndTransformation(father.getFitnessEvaluator()), parentA, parentB;

		if (childParity++ % 2 == 0) {
			parentA = father;
			parentB = mother;
		} else {
			parentA = mother;
			parentB = father;
		}

		double alpha = Math.random();

		int x = (int) (alpha * parentA.getPosition().x + (1 - alpha) * parentB.getPosition().x);
		int y = (int) (alpha * parentA.getPosition().y + (1 - alpha) * parentB.getPosition().y);

		// NOTE: in article is not mentioned this transformation mating
		double dx = alpha * parentA.getShiftX() + (1 - alpha) * parentB.getShiftX();
		double dy = alpha * parentA.getShiftY() + (1 - alpha) * parentB.getShiftY();
		double angle = alpha * parentA.getAngle() + (1 - alpha) * parentB.getAngle();
		double scale = alpha * parentA.getScale() + (1 - alpha) * parentB.getScale();

		child.setValues(x, y, dx, dy, angle, scale);

		return child;
	}
}
