/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.IMutationOperator;
import utils.CRandomGenerator;

/**
 *
 * @author gimli
 */
class CPointMutation implements IMutationOperator<CPointAndTransformation> {

	private static final double MUTATION_PROBABBILITY = 0.01;
	private final double POSITION_VARIANCE = 3;
	private final double SHIFT_VARIANCE = 3;
	private final double ANGLE_VARIANCE = 0.1;
	private final double SCALE_VARIANCE = 0.1;

	/** gimli based mutation */
	public void mutateByGimli(CPointAndTransformation prototype) {

		int x, y;
		double dx, dy, angle, scale;

		if (Math.random() < MUTATION_PROBABBILITY) {
			x = (int) (prototype.getPosition().x + CRandomGenerator.rndGauss(0, POSITION_VARIANCE));
			y = (int) (prototype.getPosition().x + CRandomGenerator.rndGauss(0, POSITION_VARIANCE));
		} else {
			x = prototype.getPosition().x;
			y = prototype.getPosition().y;
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			dx = (int) (prototype.getShiftX() + CRandomGenerator.rndGauss(0, SHIFT_VARIANCE));
			dy = (int) (prototype.getShiftY() + CRandomGenerator.rndGauss(0, SHIFT_VARIANCE));
		} else {
			dx = prototype.getShiftX();
			dy = prototype.getShiftY();
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			angle = (int) (prototype.getAngle() + CRandomGenerator.rndGauss(0, ANGLE_VARIANCE));
		} else {
			angle = prototype.getAngle();
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			scale = (int) (prototype.getScale() + CRandomGenerator.rndGauss(0, SCALE_VARIANCE));
		} else {
			scale = prototype.getAngle();
		}


		prototype.setValues(x, y, dx, dy, angle, scale);
	}

	public void mutate(CPointAndTransformation prototype) {

		int x, y;
		double dx, dy, angle, scale;

		double rangeFactor = CRefPointMarker.MUTATION_FACTOR * (1 - prototype.getFitness());

		if (Math.random() < MUTATION_PROBABBILITY) {
			x = (int) (prototype.getPosition().x + rangeFactor * Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2);
			y = (int) (prototype.getPosition().y + rangeFactor * Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2);
		} else {
			x = prototype.getPosition().x;
			y = prototype.getPosition().y;
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			dx = (int) (prototype.getShiftX() + rangeFactor * Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2);
			dy = (int) (prototype.getShiftY() + rangeFactor * Math.random() * CRefPointMarker.DXY_RANGE - CRefPointMarker.DXY_RANGE / 2);
		} else {
			dx = prototype.getShiftX();
			dy = prototype.getShiftY();
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			angle = (int) (prototype.getAngle() + rangeFactor * Math.random() * CRefPointMarker.ANGLE_RANGE - CRefPointMarker.ANGLE_RANGE / 2);
		} else {
			angle = prototype.getAngle();
		}

		if (Math.random() < MUTATION_PROBABBILITY) {
			scale = (int) (prototype.getScale() + rangeFactor * Math.random() * CRefPointMarker.SCALE_RANGE - CRefPointMarker.SCALE_RANGE / 2);
		} else {
			scale = prototype.getScale();
		}


		prototype.setValues(x, y, dx, dy, angle, scale);
	}
}
