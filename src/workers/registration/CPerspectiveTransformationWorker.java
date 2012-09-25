/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import fresco.CImageContainer;
import image.colour.CBilinearInterpolation;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.LinkedList;
import utils.geometry.CPerspectiveTransformation;

/**
 * This class implements perspective transformation which is useful for flat
 * objects scanned from different angels. For this transformation is necessary
 * to define 4 marks in each image which correspond to each other.
 *
 * For better insight read CTransform.java
 *
 * @author Honza Blazek
 */
public class CPerspectiveTransformationWorker extends CRegistrationWorker {

	LinkedList<Point2D.Double> patternMarks, transformMarks;
	BufferedImage original;
	Point size;

	/**
	 *
	 * @param toTransform
	 * @param pattern
	 */
	public CPerspectiveTransformationWorker(CImageContainer toTransform, CImageContainer pattern) {
		original = toTransform.getImage();
		size = new Point(pattern.getWidth(), pattern.getHeight());
		transformMarks = toTransform.getMarks();
		patternMarks = pattern.getMarks();
	}

	@Override
	protected BufferedImage doInBackground() {
		CPerspectiveTransformation trans = new CPerspectiveTransformation();
		BufferedImage output = new BufferedImage(size.x, size.y, original.getType());
		WritableRaster out = output.getRaster();
		Raster in = original.getData();
		int x, y;
		double[] pixel;
		double[] black = {0, 0, 0};
		Point2D.Double ref;

		if (patternMarks.size() > 3 && transformMarks.size() > 3) {
			Point2D.Double[] marks = new Point2D.Double[4], refs = new Point2D.Double[4];

			for (int i = 0; i < 4; i++) {
				marks[i] = transformMarks.remove();
				refs[i] = patternMarks.get(i);
			}

			trans.setParameters(refs[0], marks[0], refs[1], marks[1], refs[2], marks[2], refs[3], marks[3]);
			// trans.setParameters(marks[0], refs[0], marks[1], refs[1], marks[2], refs[2], marks[3], refs[3]);
		}
		trans.print();

		for (x = 0; x < size.x; x++) {
			for (y = 0; y < size.y; y++) {
				ref = trans.getProjected(new Point2D.Double(x, y));
				if (ref.x <= original.getWidth()-1 && ref.x >= 0
						&& ref.y <= original.getHeight()-1 && ref.y >= 0) {
					pixel = CBilinearInterpolation.getValue(ref, in);
				} else {
					pixel = black;
				}
				out.setPixel(x, y, pixel);
			}
		}
		output.setData(out);
		return output;
	}

	@Override
	public String getWorkerName() {
		return "Perspective";
	}
}
