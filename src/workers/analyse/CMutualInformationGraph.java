/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.converters.Crgb2gray;
import image.converters.Crgb2hsv;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import image.statiscics.CMutualInformation;

/**
 *
 * @author gimli
 */
public class CMutualInformationGraph extends CAnalysisWorker {

	private BufferedImage imageA, imageB;
	private int bandA, bandB; // if images are multidimensional (rgb, hsv, etc) it's necessary to define color band
	private static final Logger logger = Logger.getLogger(CMutualInformationGraph.class.getName());

	public CMutualInformationGraph(BufferedImage imageA, BufferedImage imageB, int band1, int band2) {
		this.imageA = /*imageA; /*/ (new Crgb2gray()).convert(imageA);
		this.imageB = /*imageB; /*/ (new Crgb2gray()).convert(imageB);
		bandA = band1;
		bandB = band2;
	}

	/**
	 * Zavedeni pochybne metriky - mutual information
	 * Hodnota je tim vetsi, cim podobnejsi jsou si pixely (entropii) v obrazku
	 * souviset mouhou ruzne barevne kanaly
	 *
	 * Teoreticky ma smysl pocitat MI na nejakem netrivialnim okoli - cimz by
	 * se mel snizit vliv sumu
	 * @return heat map of Mutual information of two input images
	 */
	@Override
	protected BufferedImage doInBackground() {
		try {
			CMutualInformation mi = new CMutualInformation(imageA, imageB, bandA, bandB);
			Rectangle bounds = new Rectangle(0, 0, imageA.getWidth(), imageA.getHeight());

			logger.log(Level.INFO, "{0} started", getWorkerName());
			mi.init();
			setProgress(33);

			Raster rasterA, rasterB;
			BufferedImage output = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			double[][] miValues = new double[imageA.getWidth()][imageA.getHeight()];
			WritableRaster out = output.getRaster();
			int[] miValue = new int[3];
			double max = Double.MIN_VALUE, min = Double.MAX_VALUE;

			rasterA = imageA.getData();
			rasterB = imageB.getData();

			int[] pixelA = new int[rasterA.getNumBands()], pixelB = new int[rasterB.getNumBands()];

			for (int x = 0; x < imageA.getWidth(); x++) {
				for (int y = 0; y < imageB.getHeight(); y++) {
					rasterA.getPixel(x, y, pixelA);
					rasterB.getPixel(x, y, pixelB);
					// FIXME: pridat velikost regionu do parametru pro uzivatele
//					miValues[x][y] = mi.MIOnRects(CMaximalRectangle.getRect(new Point(x,y), 2, bounds),
//							CMaximalRectangle.getRect(new Point(x,y), 2, bounds));
					miValues[x][y] = mi.entropy(pixelA, pixelB);
					if (max < miValues[x][y]) {
						max = miValues[x][y];
					}
					if (min > miValues[x][y]) {
						min = miValues[x][y];
					}
					setProgress(33 + (x * imageA.getHeight() + y) * 33 / (imageA.getWidth() * imageA.getHeight()));
				}
			}

			logger.log(Level.INFO, "Mutual information range: [{0}, {1}]", new Object[]{min, max});

			for (int x = 0; x < imageA.getWidth(); x++) {
				for (int y = 0; y < imageB.getHeight(); y++) {
					miValue[1] =
							miValue[2] = 255;
					miValue[0] = (int) ((miValues[x][y] - min) / (max - min) * 255);
					miValue = Crgb2hsv.inverse(miValue);
					out.setPixel(x, y, miValue);
					setProgress(66 + (x * imageA.getHeight() + y) * 33 / (imageA.getWidth() * imageA.getHeight()));
				}
			}

			return output;
		} catch (IOException e) {
			logger.warning(e.getMessage());
			return null;
		}
	}

	@Override
	public String getWorkerName() {
		return "Mutual information Grapher";
	}
}
