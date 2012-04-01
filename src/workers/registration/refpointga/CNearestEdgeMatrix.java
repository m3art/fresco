/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import image.CBinaryImage;
import java.awt.Point;
import java.awt.geom.Point2D;
import utils.metrics.CEuclidMetrics;
import utils.vector.CMatrixGenerator;

/**
 * Matrix contains for each coordinates [x,y] position of nearest edge pixel
 *
 * @author gimli
 * @version Oct 16, 2011
 */
public class CNearestEdgeMatrix {

	/** size of matrix */
	final int width, height;
	/** Content of CNearestEdgeMatrix, field contains nearest Pixel for each position in image*/
	Point2D.Double[][] matrix;
	/** If no edge is in CBinaryImage this Object is not valid. Getters throws UnsupportedOperationException in this case. */
	boolean isValid;

	/**
	 * In time of O(n) searches nearest edge point
	 * @param i
	 */
	public CNearestEdgeMatrix(final CBinaryImage i) {
		width = i.getWidth();
		height = i.getHeight();
		// eucleidian distance form matrix[x][y]
		double[][] distance = CMatrixGenerator.create(width, height, Double.MAX_VALUE);

		matrix = new Point2D.Double[width][height];

		// evaluate nearest pixel from top left and bottom right corner
		int k, l; //coords form bottom right
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				matrix[x][y] = evaluate(x, y, i);
				if (matrix[x][y] != null) {
					distance[x][y] = CEuclidMetrics.distance(matrix[x][y], new Point2D.Double(x, y));
				} else {
					distance[x][y] = Double.MAX_VALUE;
				}

				k = width - 1 - x;
				l = height - 1 - y;

				matrix[k][l] = evaluate(k, l, i);
				if (matrix[k][l] != null) {
					distance[k][l] = CEuclidMetrics.distance(matrix[k][l], new Point2D.Double(k, l));
				} else {
					distance[k][l] = Double.MAX_VALUE;
				}
			}
		}

		isValid = checkSettings(width, height);
	}

	private Point2D.Double evaluate(final int x, final int y, final CBinaryImage i) {
		Point2D.Double center = new Point2D.Double(x, y);
		double min = Double.MAX_VALUE, distance;
		Point2D.Double nearest = null;

		if (i.isOne(x, y)) {
			return center;
		}

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {

				if (x + dx >= 0 && x + dx < width && // check range of neighbor on x axis
						y + dy >= 0 && y + dy < height && // check range of neighbor on y axis
						matrix[x + dx][y + dy] != null) { // check value of that neighbor

					distance = CEuclidMetrics.distance(matrix[x + dx][y + dy], center); // compute distance from this point
					if (distance < min) { // set as best if possible
						min = distance;
						nearest = matrix[x + dx][y + dy];
					}
				}
			}
		}

		return nearest;
	}

	/**
	 * @param p center of searching
	 * @return nearest edge point, if there is several of them, one is chosen.
	 * @throws UnsupportedOperationException in case when no edge is in image
	 */
	public Point2D.Double getNearest(final Point2D.Double p) {
		if (isValid) {
			return matrix[Math.max(0, Math.min((int)Math.round(p.x), width - 1))][Math.max(0, Math.min((int)Math.round(p.y), height - 1))];
		} else {
			throw new UnsupportedOperationException("No edges in BinaryImage. Try different treshold.");
		}
	}

	/**
	 * @param x coordinate of center of searching
	 * @param y coordinate of center of searching
	 * @return nearest edge point, if there is several of them, one is chosen.
	 * @throws UnsupportedOperationException in case when no edge is in image
	 */
	public Point2D.Double getNearest(int x, int y) {
		if (isValid) {
			return matrix[x][y];
		} else {
			throw new UnsupportedOperationException("No edges in BinaryImage. Try different treshold.");
		}
	}

	private boolean checkSettings(int width, int height) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (matrix[x][y] == null) {
					return false;
				}
			}
		}
		return true;
	}
}
