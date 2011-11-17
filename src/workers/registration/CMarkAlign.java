/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import image.converters.Crgb2hsv;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.LinkedList;
import image.statiscics.CMutualInformation;
import utils.vector.CBasic;

/**
 * @author Honza Blazek
 */
public class CMarkAlign {

	static int vicinity = 15; // searched vicinity for align
	static int area = 18; // compared areas - for counting Mutual Information

	/**
	 * Main function of the class
	 * @param input1 defines first image
	 * @param marks1 defines marks on the first image, these marks will be aligned
	 * @param input2 defines second image
	 * @param marks2 defines marks on the second image, these are fixed
	 * @throws java.io.IOException if the lists of marks don't have the same size
	 */
	public static void align(BufferedImage input1, LinkedList<Point> marks1,
			BufferedImage input2, LinkedList<Point> marks2) {
		if (marks1.size() != marks2.size()) {
			return;
		}
		boolean aligned = false;
		int i, x, y, bestX, bestY;
		double mi = 0, bestMI = 0;
		CMutualInformation worker;
		Rectangle rect1, rect2;
		Point mark1, mark2;
		Dimension start, end;

		worker = new CMutualInformation((new Crgb2hsv()).convert(input1), (new Crgb2hsv()).convert(input2), 2, 2);

		for (i = 0; i < marks2.size(); i++) {
			aligned = false;
			mark1 = marks1.removeFirst(); // these marks will be moved
			mark2 = marks2.get(i); // on the second picture are the marks fixed

			rect2 = getRegion(mark2, mark1, input1.getWidth(), input1.getHeight(), 10);

			// set the rectangle for searching
			start = new Dimension(Math.max(0, mark1.x - vicinity), Math.max(0, mark1.y - vicinity));
			end = new Dimension(Math.min(input1.getWidth(), mark1.x + vicinity),
					Math.min(input1.getHeight(), mark1.y + vicinity));

			bestMI = bestX = bestY = 0;
			for (x = start.width; x < end.width; x++) {
				for (y = start.height; y < end.height; y++) {
					//mi = Math.abs(getCov(new Point(x, y), mark2, input1, input2));
					rect1 = getRegion(new Point(x, y), mark2, input1.getWidth(), input1.getHeight(), 10);
					worker.initOnRect(rect1, rect2);
					mi = worker.MIOnRects(rect1, rect2);
					if (mi >= bestMI) {
						bestMI = mi;
						bestX = x;
						bestY = y;
						aligned = true;
					}
				}
			}
			if (aligned) {
				marks1.add(new Point(bestX, bestY));
			} else {
				marks1.add(mark1);
			}
		}
	}

	/**
	 * This function extract areas from input images and count their covariance
	 * @param mark1 center point of input 1
	 * @param mark2 center point of input 2
	 * @param input1 first image (on which is mark aligned)
	 * @param input2 second image (fixed marks)
	 * @return value of covariance
	 */
	private static double getCov(Point mark1, Point mark2, Raster input1, Raster input2) {
		Point[] border = getRegionSize(mark1, mark2, input1, input2);
		double[] pixels1 = new double[border[1].x * border[1].y * input1.getNumBands()];
		double[] pixels2 = new double[border[1].x * border[1].y * input1.getNumBands()];

		input1.getPixels(mark1.x - border[0].x, mark1.y - border[0].y, border[1].x, border[1].y, pixels1);
		input2.getPixels(mark2.x - border[0].x, mark2.y - border[0].y, border[1].x, border[1].y, pixels2);

		return CBasic.covariance(pixels1, pixels2);
	}

	/**
	 * Sets maximum and minimum range for area from which we want to count covariance
	 * @param mark1 center point of area at input1
	 * @param mark2 center point of area at input2
	 * @param input1 first image (on which is mark aligned)
	 * @param input2 second image
	 * @return left-top point of area and width-height size
	 */
	private static Point[] getRegionSize(Point mark1, Point mark2, Raster input1, Raster input2) {
		Point[] rect = new Point[2];

		// find right and top border - it's min of area, mark1 and mark2
		rect[0] = new Point(Math.min(area, Math.min(mark1.x, mark2.x)),
				Math.min(area, Math.min(mark1.y, mark2.y)));
		// find left and bottom border - it's min of area and
		// distance between mark1/2 and left/bottom border of picture
		rect[1] = new Point(Math.min(area, Math.min(input1.getWidth() - mark1.x, input2.getWidth() - mark2.x)),
				Math.min(area, Math.min(input1.getHeight() - mark1.y, input2.getHeight() - mark2.y)));
		// change value of rect[1] - it contains size of area
		rect[1].x += rect[0].x;
		rect[1].y += rect[0].y;

		return rect;
	}

	/**
	 * Creates rectangle region which is maximal at image coords and is centered
	 * on mark1
	 * @param mark1 center point - position of user mark
	 * @param mark2 center point - position of user mark in 2nd input image
	 * @param width of image
	 * @param height of image
	 * @param regionSize rectangle which is whole inside image
	 * @return
	 */
	private static Rectangle getRegion(Point mark1, Point mark2, int width, int height, int regionSize) {
		Rectangle out;
		int plusX = Math.min(width - Math.max(mark1.x, mark2.x), regionSize),
				plusY = Math.min(height - Math.max(mark1.y, mark2.y), regionSize),
				minusX = Math.min(Math.min(mark1.x, mark2.x), regionSize),
				minusY = Math.min(Math.min(mark1.y, mark2.y), regionSize);

		out = new Rectangle(mark1.x - minusX,
				mark1.y - minusY,
				plusX + minusX + 1,
				plusX + minusX + 1);

		return out;
	}
}
