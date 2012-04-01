/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import geneticalgorithms.CGenotype;
import geneticalgorithms.IFitnessOperator;
import java.awt.Point;
import java.awt.geom.Point2D;
import utils.geometry.CAffineTransformation;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public class CPointAndTransformation extends utils.geometry.CPointAndTransformation implements CGenotype {

	/** Fitness counter */
	private final IFitnessOperator<CPointAndTransformation> fitnessEvaluator;
	/** measure of quality of correspondence */
	private double fitness;
	/** counting of fitness can be expensive, therefore is counted only if necessary
	 * This variable means if {#fitness} is valid number
	 */
	private boolean validFitness = false;

	public CPointAndTransformation(IFitnessOperator<CPointAndTransformation> fitnessEvaluator) {
		this.fitnessEvaluator = fitnessEvaluator;
	}

	public void replaceBy(CPointAndTransformation offspring) {
		position.x = offspring.getPosition().x;
		position.y = offspring.getPosition().y;
		dx = offspring.getShiftX();
		dy = offspring.getShiftY();
		angle = offspring.getAngle();
		scale = offspring.getScale();
		if (offspring.hasValidFitness()) {
			fitness = offspring.getFitness();
			validFitness = true;
		}
	}

	public void setValues(double x, double y, double dx, double dy, double angle, double scale) {
		position = new Point2D.Double(x, y);
		this.dx = dx;
		this.dy = dy;
		this.angle = angle;
		this.scale = scale;
		validFitness = false;
	}

	public void alignToEdge(CNearestEdgeMatrix m) {
		position = m.getNearest(position);
		validFitness = false;
	}

	@Override
	public double getFitness() {
		if (!validFitness) {
			fitness = fitnessEvaluator.getFitness(this);
			validFitness = true;
		}
		return fitness;
	}

	public boolean hasValidFitness() {
		return validFitness;
	}

	Point2D.Double getProjection() {
		return CAffineTransformation.getProjected(position.x, position.y, dx, dy, angle, scale, scale);
	}

	IFitnessOperator<CPointAndTransformation> getFitnessEvaluator() {
		return fitnessEvaluator;
	}

	void setPosition(Point2D.Double nearestEdgePixel) {
		position = nearestEdgePixel;
		validFitness = false;
	}
}
