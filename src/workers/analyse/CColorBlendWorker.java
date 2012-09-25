package workers.analyse;

import fresco.CData;
import image.converters.CBufferedImageToDoubleArray;
import image.converters.Crgb2Ycbcr;
import image.converters.Crgb2grey;
import image.converters.Crgb2uvY;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.logging.Logger;

/**
 * @author gimli
 * @version Aug 2, 2012
 */
public class CColorBlendWorker extends CAnalysisWorker {

	double[][][] yCbCr;
	private final static Logger logger = Logger.getLogger(CColorBlendWorker.class.getName());

	@Override
	public String getWorkerName() {
		return "Color blend worker";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (CData.showImage[0] == -1 || CData.showImage[2] == -1)
			return null;

		int width = Math.min(CData.getImage(CData.showImage[0]).getWidth(), CData.getImage(CData.showImage[2]).getWidth());
		int height = Math.min(CData.getImage(CData.showImage[0]).getHeight(), CData.getImage(CData.showImage[2]).getHeight());

		yCbCr = new double[width][height][3];

		Raster u = CData.getImage(CData.showImage[0]).getImage().getData();
		Raster v = CData.getImage(CData.showImage[2]).getImage().getData();

		double[] pixel = new double[3];

		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				u.getPixel(x, y, pixel);
				yCbCr[x][y][0] = Crgb2grey.convertToOneValue(pixel);
				v.getPixel(x, y, pixel);
				yCbCr[x][y][1] = Crgb2grey.convertToOneValue(pixel);
				yCbCr[x][y][2] = (yCbCr[x][y][1] + yCbCr[x][y][0])/2;
				//yCbCr[x][y] = Crgb2Ycbcr.inverse(yCbCr[x][y]);
			}
		}

		BufferedImage output = CBufferedImageToDoubleArray.inverse(yCbCr);

		setProgress(100);

		return output;
	}

}
