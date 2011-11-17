/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.statiscics;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gimli
 */
public class CMutualInformation {

	public static int range = 256, colorBand = 1, colorBand2 = 1;
	Raster in1, in2;
	int numOfPixels;
	int[] histX, histY;
	int[][] histXY;
	private static final Logger logger = Logger.getLogger(CMutualInformation.class.getName());

	/**
	 * Constructor must obtain two images with the same size
	 * @param input1
	 * @param input2
	 * @throws java.io.IOException if the size of image not correspond
	 */
	public CMutualInformation(final BufferedImage input1, final BufferedImage input2, int band1, int band2) {
		in1 = input1.getData();
		in2 = input2.getData();
		//colorBand = band1;
		//colorBand2 = band2;
		numOfPixels = in1.getWidth() * in2.getHeight();
	}

	/**
	 * Initialization of statistics (time consuming - recomended to use in
	 * separate thread).
	 *
	 * Mutual information:<br />
	 * <b>H(X,Y) = H(X) + H(Y) - H(Y|X)</b>, where<br />
	 * <b>H(X) = -\sum_{x\in X}p(x)*log(p(x)) and <br />
	 * <b>H(Y|X) =  -sum_{y\in Y}p(y|X)*log(p(y|X))
	 */
	public void init() throws IOException {
		if (in1.getWidth() != in2.getWidth() || in1.getHeight() != in2.getHeight()) {
			throw new IOException("Bad format of input data");
		}
		histX = CHistogram.getHistogram(in1, range, colorBand);
		histY = CHistogram.getHistogram(in2, range, colorBand2);
		histXY = CHistogram.get2DHistogram(in1, in2, range, colorBand, colorBand2);
	}

	public void initOnRect(Rectangle rect1, Rectangle rect2) {
		histX = CHistogram.getSubHistogram(in1, rect1, range, colorBand);
		histY = CHistogram.getSubHistogram(in2, rect2, range, colorBand2);
		histXY = CHistogram.get2DSubHistogram(in1, in2, rect1, rect2, range, colorBand, colorBand2);
	}

	/**
	 * Gets value of mutual information for two specified color values
	 * depends on input images. Output values from each band are multiplied
	 * @param pixel1 color from the first image
	 * @param pixel2 color from the second image
	 * @return value of mutual information measure
	 */
	public double entropy(int[] pixel1, int[] pixel2) {
		double Hxy = 0, Hx = 0, Hy = 0;

		Hx += entropyGain(histX[pixel1[colorBand]]);
		Hxy += entropyGain(histXY[pixel1[colorBand]][pixel2[colorBand2]]);
		Hy += entropyGain(histY[pixel2[colorBand2]]);

		return -Hx - Hy + 2 * Hxy;
	}

	public double MIOnRects(Rectangle rect1, Rectangle rect2) {
		double out = 0, weight = 0;
		int[] pixel1 = new int[in1.getNumBands()], pixel2 = new int[in2.getNumBands()];
		Point.Double center = new Point.Double(rect1.getWidth() - rect1.x, rect2.getHeight() - rect2.y);


		for (int x = 0; x < rect1.width; x++) {
			for (int y = 0; y < rect1.height; y++) {
				in1.getPixel(rect1.x + x, rect1.y + y, pixel1);
				in2.getPixel(rect2.x + x, rect2.y + y, pixel2);
				out += entropy(pixel1, pixel2) / ((x - center.x) * (x - center.x) + (y - center.y) * (y - center.y) + 1);
				weight += 1 / ((x - center.x) * (x - center.x) + (y - center.y) * (y - center.y) + 1);
			}
		}

		return out / weight;
	}

	/**
	 * From the probability value counts entropy value
	 * @param value probability of color in the image
	 * @return entropyGain value for this corresponding color
	 */
	private double entropyGain(double value) {
		if (value == 0) {
			logger.fine("Bug stayed");
		}
		double prst = value / (double) numOfPixels;
		if (prst > 1) {
			logger.log(Level.WARNING, "Prst out of range: {0}", prst);
		}
		return -prst * Math.log(prst);
	}
}
