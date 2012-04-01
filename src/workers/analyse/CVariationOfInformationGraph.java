/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.converters.Crgb2grey;
import image.converters.Crgb2hsv;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import image.statiscics.CVariationOfInformation;

/**
 * Mutual information is computed over two gray scale images. Mutual information
 * is defined by pixelGain and joined pixelGain of images. For more see wikipedia.
 * Worker takes two images and computes gain of each pixel to MI. This value is
 * scaled and drawn.
 *
 * @author gimli
 */
public class CVariationOfInformationGraph extends CAnalysisWorker {

	/** Measured images */
	private BufferedImage imageA, imageB;
	/** logging tool */
	private static final Logger logger = Logger.getLogger(CVariationOfInformationGraph.class.getName());

	public CVariationOfInformationGraph(BufferedImage imageA, BufferedImage imageB) {
		this.imageA = /*imageA; /*/ (new Crgb2grey()).convert(imageA);
		this.imageB = /*imageB; /*/ (new Crgb2grey()).convert(imageB);
	}

	/**
	 * MI is not a metric. For such purpose we use Variation of information.
	 * @return heat map of Mutual information of two input images
	 */
	@Override
	protected BufferedImage doInBackground() {
		try {
			CVariationOfInformation mi = new CVariationOfInformation(imageA, imageB);

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
					miValues[x][y] = mi.pixelGain(pixelA, pixelB);
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
