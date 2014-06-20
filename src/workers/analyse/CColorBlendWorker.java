/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import fresco.CData;
import fresco.swing.CWorkerDialogFactory;
import image.converters.CBufferedImageToDoubleArray;
import image.converters.Crgb2Lab;
import image.converters.Crgb2grey;
import image.converters.Crgb2hsv;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * This algorithm was published at Digital Heritage 2013. If you use it for 
 * scientific purposes, please cite us.
 * 
 * @author gimli
 * @version Aug 2, 2012
 */
public class CColorBlendWorker extends CAnalysisWorker {
	
	private enum DiffMeasure {INTENSITY, MUTUAL_MATCHING};

	/** output image */
	private double[][][] rgb;
	
	/** color for first input image iff differ from second input */
	private double[] mappingColorA;
	/** color iff second image differs */
	private double[] mappingColorB;
	/** Difference matrix - same size as the image */
	private double[][] diff;
	/** maximum of difference which has to be colored */
	private double maxDiff;
	
	private DiffMeasure diffMeasure = DiffMeasure.INTENSITY;
	
	private final static Logger logger = Logger.getLogger(CColorBlendWorker.class.getName());

	@Override
	public String getWorkerName() {
		return "Color blend worker";
	}
	
	private static double getSaturation(double hue, double value, double diff, double maxDiff) {
		Crgb2Lab convertor = new Crgb2Lab(Crgb2Lab.WhitePoint.D65);
		double[] mshMax = Crgb2Lab.lab2Msh(convertor.sRgb2Lab(Crgb2hsv.inverse(new double[]{hue, 255, value})));
		double labSat = mshMax[1]/maxDiff * diff;
		
		double[] lab = {mshMax[0], labSat, mshMax[2]};
		
		return Crgb2hsv.convert(convertor.lab2sRgb(Crgb2Lab.msh2Lab(lab)))[1];
	}

	private void computeIntensityDiff(Raster u, Raster v) {
		diff = new double[u.getWidth()][u.getHeight()];
		double[] pixel = new double[3];
		
		maxDiff = 0;
		
		for(int x=0; x<u.getWidth(); x++) {
			for(int y=0; y<u.getHeight(); y++) {
				u.getPixel(x, y, pixel);
				double uI = Crgb2grey.convertToOneValue(pixel);
				v.getPixel(x, y, pixel);
				double vI = Crgb2grey.convertToOneValue(pixel);
				
				diff[x][y] = Math.abs(uI-vI);
				if(maxDiff < diff[x][y]) {
					maxDiff = diff[x][y];
				}
			}
		}
	}
	
	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (CData.showImage[0] == -1 || CData.showImage[2] == -1)
			return null;

		int width = Math.min(CData.getImage(CData.showImage[0]).getWidth(), CData.getImage(CData.showImage[2]).getWidth());
		int height = Math.min(CData.getImage(CData.showImage[0]).getHeight(), CData.getImage(CData.showImage[2]).getHeight());

		Raster u = CData.getImage(CData.showImage[0]).getImage().getData();
		Raster v = CData.getImage(CData.showImage[2]).getImage().getData();
		
		switch(diffMeasure) {
			case INTENSITY:
				computeIntensityDiff(u, v);
				break;
			case MUTUAL_MATCHING:
				throw new UnsupportedOperationException();
		}
		
		rgb = new double[width][height][3];
		
		double[] pixel = new double[3];

		for(int x=0; x<width; x++) {
			for(int y=0; y<height; y++) {
				u.getPixel(x, y, pixel);
				double uI = Crgb2grey.convertToOneValue(pixel);
				v.getPixel(x, y, pixel);
				double vI = Crgb2grey.convertToOneValue(pixel);

				pixel[0] = (uI > vI) ? mappingColorA[0] : mappingColorB[0];
				pixel[2] = (uI + vI)/2;
				
				pixel[1] = getSaturation(pixel[0], pixel[2], diff[x][y], maxDiff);
				
				rgb[x][y] = Crgb2hsv.inverse(pixel);
			}
		}

		BufferedImage output = CBufferedImageToDoubleArray.inverse(rgb);

		setProgress(100);

		return output;
	}

	JColorChooser colorChooser1 = new JColorChooser(Color.red);
	JColorChooser colorChooser2 = new JColorChooser(Color.green);

	@Override
	public JDialog getParamSettingDialog() {
		JPanel panel = new JPanel();
		
		panel.add(colorChooser1);
		panel.add(colorChooser2);

		return CWorkerDialogFactory.createOkCancelDialog(this, panel);
	}

	@Override
	public boolean confirmDialog() {
		mappingColorA = new double[3];
		mappingColorA[0] = colorChooser1.getColor().getRed();
		mappingColorA[1] = colorChooser1.getColor().getGreen();
		mappingColorA[2] = colorChooser1.getColor().getBlue();
		mappingColorB = new double[3];
		mappingColorB[0] = colorChooser2.getColor().getRed();
		mappingColorB[1] = colorChooser2.getColor().getGreen();
		mappingColorB[2] = colorChooser2.getColor().getBlue();

		logger.log(Level.INFO, "ColorA: [{0};{1};{2}], ColorB: [{3};{4};{5}]", new Object[]{mappingColorA[0], mappingColorA[1], mappingColorA[2], mappingColorB[0], mappingColorB[1], mappingColorB[2]});

		mappingColorA = Crgb2hsv.convert(mappingColorA);
		mappingColorB = Crgb2hsv.convert(mappingColorB);

		return true;
	}
}
