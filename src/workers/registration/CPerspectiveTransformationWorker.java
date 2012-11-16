/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import fresco.CImageContainer;
import image.colour.CBilinearInterpolation;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.LinkedList;
import java.util.logging.Logger;
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

	/**
	 * Registration marks lists
	 */
	private final LinkedList<Point2D.Double> patternMarks, transformMarks;
	/**
	 * Images
	 */
	private final BufferedImage original, pattern;
	/**
	 * Output image size (same as pattern.getSize())
	 */
	private final Dimension outputDimensions;
	/**
	 * Image type is the same as type of original image
	 */
	private final int outputImageType;
	private static final Logger logger = Logger.getLogger(CPerspectiveTransformationWorker.class.getName());

	/**
	 * @param toTransform image which will be transformed
	 * @param pattern serves for mark alignment and size of pattern is fitted by
	 * transformation
	 */
	public CPerspectiveTransformationWorker(CImageContainer toTransform, CImageContainer pattern) {
		original = toTransform.getImage();
		outputImageType = original.getType();
		this.pattern = pattern.getImage();
		outputDimensions = new Dimension(pattern.getWidth(), pattern.getHeight());
		transformMarks = toTransform.getMarks();
		patternMarks = pattern.getMarks();
	}

	@Override
	protected BufferedImage doInBackground() {
		Raster origRaster = original.getData();

		//logger.info("Moving marks ...");
		// align user defined marks
		//CMarkAlign.align(origRaster, transformMarks, pattern.getRaster(), patternMarks);

		// compute transformation
		logger.info("Computing transformation ...");
		CPerspectiveTransformation trans = computeTransformation(patternMarks, transformMarks);
		logger.info(trans.dump());

		// image transformation
		logger.info("Transforming image ...");
		return transformImage(origRaster, trans);
	}

	private static CPerspectiveTransformation computeTransformation(LinkedList<Point2D.Double> patternMarks, LinkedList<Point2D.Double> transformMarks) {
		CPerspectiveTransformation trans = new CPerspectiveTransformation();

		if (patternMarks.size() > 3 && transformMarks.size() > 3) {
			Point2D.Double[] marks = new Point2D.Double[4], refs = new Point2D.Double[4];

			for (int i = 0; i < 4; i++) {
				marks[i] = transformMarks.get(i);
				refs[i] = patternMarks.get(i);
			}

			trans.setParameters(refs[0], marks[0], refs[1], marks[1], refs[2], marks[2], refs[3], marks[3]);
		}
		return trans;
	}

	private BufferedImage transformImage(Raster in, CPerspectiveTransformation trans) {
		BufferedImage output = new BufferedImage((int)outputDimensions.getWidth(), (int)outputDimensions.getHeight(), outputImageType);
		WritableRaster out = output.getRaster();
		for (int x = 0; x < outputDimensions.getWidth(); x++) {
			for (int y = 0; y < outputDimensions.getHeight(); y++) {
				Point2D.Double ref = trans.getProjected(new Point2D.Double(x, y));
				double[] pixel;
				if (ref.x <= in.getWidth() - 1 && ref.x >= 0
						&& ref.y <= in.getHeight() - 1 && ref.y >= 0) {
					pixel = CBilinearInterpolation.getValue(ref, in);
				} else {
					pixel = new double[]{0, 0, 0};
				}
				out.setPixel(x, y, pixel);
				setProgress((int)((x*outputDimensions.getHeight()+y)*100/outputDimensions.getWidth()/outputDimensions.getHeight()));
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
