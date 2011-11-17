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
public class CPointAndTransformation extends CGenotype {

	/** Position of individual in image */
	private Point position;
	/** Transformation parameters, translation, angle and scale */
	private double dx, dy, angle, scale;
	/** Fitness counter */
	private final IFitnessOperator<CPointAndTransformation> fitnessEvaluator;
	private double fitness;
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

	public void setValues(int x, int y, double dx, double dy, double angle, double scale) {
		position = new Point(x, y);
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

	public Point getPosition() {
		return position;
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

	/**
	 * Projection of this center point applied on another pixel
	 * @param x coordinate of projected pixel
	 * @param y coordinate of projected pixel
	 * @return projected point
	 */
	Point2D.Double getProjection(double x, double y) {
		return CAffineTransformation.getProjected(x, y, dx, dy, angle, scale, scale);
	}

	public Point getIntProjection() {
		Point2D.Double projection = getProjection(position.x, position.y);
		return new Point((int) Math.round(projection.x), (int) Math.round(projection.y));
	}

	IFitnessOperator<CPointAndTransformation> getFitnessEvaluator() {
		return fitnessEvaluator;
	}

	double getShiftX() {
		return dx;
	}

	double getShiftY() {
		return dy;
	}

	double getAngle() {
		return angle;
	}

	double getScale() {
		return scale;
	}

	void setPosition(Point nearestEdgePixel) {
		position = nearestEdgePixel;
		validFitness = false;
	}
}
