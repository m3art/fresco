/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.vector.CBasic;

/**
 * @author Honza Blazek
 */
public class CMarkAlign {

	/** searched vicinity for align */
	static int VICINITY = 80;
	/** compared areas - for counting Variation of Information */
	static int AREA = 18;

	private static final Logger logger = Logger.getLogger(CMarkAlign.class.getName());

	/**
	 * Method takes two images and two sets of marks. The first set is aligned as
	 * covariance between new mark and mark in second image is maximal.
	 *
	 * Size of used vicinity for searching is {@value #VICINITY}
	 *
	 * @param input1 defines first image
	 * @param marks1 defines marks on the first image, these marks will be aligned
	 * @param input2 defines second image
	 * @param marks2 defines marks on the second image, these are fixed
	 * @throws AssertionError if the lists of marks don't have the same size
	 */
	public static void align(final Raster input1, LinkedList<Point2D.Double> marks1,
			final Raster input2, final LinkedList<Point2D.Double> marks2) {
		if (marks1.size() != marks2.size()) {
			throw new AssertionError("Expected corresponding point pairs. Numbers of marks does not match!");
		}

		// go through marks and align each pair
		for (int i = 0; i < marks2.size(); i++) {
			Point2D.Double mark1 = marks1.removeFirst(); // these marks will be moved
			Point2D.Double mark2 = marks2.get(i); // on the second picture are the marks fixed

			// set the rectangle for searching (stay in image area)
			Dimension start = new Dimension((int)Math.max(0, mark1.x - VICINITY), (int)Math.max(0, mark1.y - VICINITY));
			Dimension end = new Dimension((int)Math.min(input1.getWidth(), mark1.x + VICINITY),
					(int)Math.min(input1.getHeight(), mark1.y + VICINITY));

			// count current settings quality
			Point2D.Double bestPosition = new Point2D.Double(mark1.x, mark1.y);
			double bestQuality = getQuality(mark1, input1, mark2, input2);
			logger.log(Level.FINE, "Previous quality [{0}]: {1}", new Object[]{i, bestQuality});
			// go through vicinity and count quality of neighbors
			for (int x = start.width; x < end.width; x++) {
				for (int y = start.height; y < end.height; y++) {
					if (x == mark1.x && y == mark1.y) {
						// mark quality already computed
						continue;
					}

					double quality = getQuality(new Point2D.Double(x,y), input1, mark2, input2);
					if (quality > bestQuality) {
						bestQuality = quality;
						bestPosition.x = x;
						bestPosition.y = y;
					}
				}
			}
			logger.log(Level.FINE, "New quality [{0}]: {1}", new Object[]{i, bestQuality});
			marks1.add(bestPosition); // put new mark at the end of list
		}
	}

	/**
	 * Count hard-coded metric on centered rectangles around marks
	 * @param a mark in first image
	 * @param rasterA first image data
	 * @param b mark in second image
	 * @param rasterB second image data
	 * @return double value of our metric (in this case covariance in each band)
	 */
	private static double getQuality(Point2D.Double a, Raster rasterA, Point2D.Double b, Raster rasterB) {
		Rectangle[] rect = getMaximalAlignedRectangles(a, b, rasterA.getBounds().getSize(), rasterB.getBounds().getSize());

		logger.log(Level.FINEST, "Bounds[0]: {0}", rect[0].toString());
		logger.log(Level.FINEST, "Bounds[1]: {0}", rect[1].toString());

		double[] pixelsA = null, pixelsB = null;
		return CBasic.covariance(rasterA.getSamples(rect[0].x, rect[0].y, rect[0].width, rect[0].height, 2, pixelsA),
			rasterA.getSamples(rect[0].x, rect[0].y, rect[0].width, rect[0].height, 2, pixelsB));
	}

	/**
	 * Sets maximum and minimum range for area from which we want to count covariance
	 * @param mark1 center point of area at input1
	 * @param mark2 center point of area at input2
	 * @param maxSize1 size of first image
	 * @param maxSize2 size of second image
	 * @return left-top point of area and width-height size
	 */
	private static Rectangle[] getMaximalAlignedRectangles(Point2D.Double mark1, Point2D.Double mark2, Dimension maxSize1, Dimension maxSize2) {
		int width = AREA;
		int height = AREA;

		if (mark1.x-width/2 < 0) { // left bound out of image ... cut both sides
			width += 2*(mark1.x-width/2);
		}
		if (mark1.x+width/2 > maxSize1.getWidth()) { // right bound out of image ... cut both sides
			width -= 2*(mark1.x+width/2 - maxSize1.getWidth());
		}
		if (mark2.x-width/2 < 0) { // left bound out of image ... cut both sides
			width += 2*(mark2.x-width/2);
		}
		if (mark2.x+width/2 > maxSize2.getWidth()) { // right bound out of image ... cut both sides
			width += 2*(mark2.x+width/2 - maxSize2.getWidth());
		}

		if (mark1.y-height/2 < 0) { // top bound out of image ... cut both sides
			height += 2*(mark1.y-height/2);
		}
		if (mark1.y+height/2 > maxSize1.getHeight()) { // bottom bound out of image ... cut both sides
			height -= 2*(mark1.y + height/2 - maxSize1.getHeight());
		}
		if (mark2.y-height/2 < 0) { // top bound out of image ... cut both sides
			height += 2*(mark2.y-height/2);
		}
		if (mark2.y+height/2 > maxSize2.getHeight()) { // bottom bound out of image ... cut both sides
			height += 2*(mark2.y+height/2 - maxSize2.getHeight());
		}

		Rectangle rect[] = new Rectangle[2];
		rect[0] = new Rectangle((int)Math.ceil(mark1.x-width/2), (int)Math.ceil(mark1.y-height/2), width, height);
		rect[1] = new Rectangle((int)Math.ceil(mark2.x-width/2), (int)Math.ceil(mark2.y-height/2), width, height);

		return rect;
	}
}
