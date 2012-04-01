/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.metrics;

import image.colour.CBilinearInterpolation;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.ArrayUtils;
import utils.geometry.CPointAndTransformation;

/**
 *
 * @author gimli
 */
public abstract class CAreaSimilarityMetric {

	public static enum Shape {

		RECTANGULAR, CIRCULAR
	};
	private final Raster imageA, imageB;
	private double radius;
	private final Shape shape;
	private static final Logger logger = Logger.getLogger(CAreaSimilarityMetric.class.getName());

	public CAreaSimilarityMetric(BufferedImage inputA, BufferedImage inputB, double radius, Shape shape) {
		imageA = inputA.getRaster();
		imageB = inputB.getRaster();
		this.radius = radius;
		this.shape = shape;
		logger.log(Level.FINE, "Matric initialized: radius: {0}, shape: {1}", new Object[]{radius, shape});
	}

	/**
	 * Possible reseting of radius during measuring
	 * @param newRadius new value of radius
	 */
	public void setRadius(float newRadius) {
		radius = newRadius;
	}

	protected abstract double getValue(double[] inputAValues, double[] inputBValues);

	/**
	 * Distance between two rectangular areas of the same size (transformation
	 * is identity)
	 * @param centerOfAreaA referring point in first image
	 * @param centerOfAreaB referring point in second image
	 * @return value of matric
	 */
	public double getDistance(Point2D.Double centerOfAreaA, Point2D.Double centerOfAreaB) {
		return getDistance(null, centerOfAreaA, centerOfAreaB);
	}

	public double getDistance(CPointAndTransformation refs) {
		return getDistance(refs, null, null);
	}

	private double getDistance(CPointAndTransformation refs, Point2D.Double centerOfAreaA, Point2D.Double centerOfAreaB) {
		LinkedList<Double> intensityA = new LinkedList<Double>();
		LinkedList<Double> intensityB = new LinkedList<Double>();

		for (double x = -radius; x <= radius; x++) {
			for (double y = -radius; y <= radius; y++) {
				if (shape == Shape.CIRCULAR && Math.pow(x, 2) + Math.pow(y, 2) > Math.pow(radius, 2)) {
					continue;
				}
				Point2D.Double pointA = getPointA(refs, centerOfAreaA, x, y);
				Point2D.Double pointB = getPointB(refs, pointA, centerOfAreaB, x, y);

				// check if corresponding point are defined
				if (pointA.x > 0 && pointA.x < imageA.getWidth() - 1
						&& pointB.x > 0 && pointB.x < imageB.getWidth() - 1
						&& pointA.y > 0 && pointA.y < imageA.getHeight() - 1
						&& pointB.y > 0 && pointB.y < imageB.getHeight() - 1) {

					double[] valuesA = CBilinearInterpolation.getValue(pointA, imageA);
					double[] valuesB = CBilinearInterpolation.getValue(pointB, imageB);

					for (int i = 0; i < imageA.getNumBands(); i++) {
						intensityA.add(valuesA[i]);
						intensityB.add(valuesB[i]);
					}
				}
			}
		}

		return getValue(
				ArrayUtils.toPrimitive(intensityA.toArray(new Double[0])),
				ArrayUtils.toPrimitive(intensityB.toArray(new Double[0])));
	}

	private Point2D.Double getPointA(CPointAndTransformation refs, Point2D.Double centerOfAreaA, double x, double y) {
		if (refs == null) {
			return new Point2D.Double(centerOfAreaA.x + x, centerOfAreaA.y + y);
		}
		return new Point2D.Double(refs.getPosition().x + x, refs.getPosition().y + y);
	}

	private Point2D.Double getPointB(CPointAndTransformation refs, Point2D.Double pointA, Point2D.Double centerOfAreaB, double x, double y) {
		if (refs == null) {
			return new Point2D.Double(centerOfAreaB.x + x, centerOfAreaB.y + y);
		}
		return refs.getProjection(pointA.x, pointA.y);
	}
}
