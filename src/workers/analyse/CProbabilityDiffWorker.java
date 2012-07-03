/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.converters.CBufferedImageToDoubleArray;
import image.statiscics.CHistogramND;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import workers.analyse.pca.CPca;

/**
 *
 * @author gimli
 */
public class CProbabilityDiffWorker extends CAnalysisWorker {

	private double differenceThreshold = 25;
	BufferedImage imageA, imageB;

	public CProbabilityDiffWorker(BufferedImage inputA, BufferedImage inputB) {
		this.imageA = inputA;
		this.imageB = inputB;
	}

	@Override
	public String getWorkerName() {
		return "Probability difference analysis";
	}

	@Override
	protected BufferedImage doInBackground() {

		double[][] inputValues = new double[2][];

		CPca pcaA = new CPca();
		inputValues[0] = pcaA.pcaTransform(CBufferedImageToDoubleArray.convertToPixelArray(imageA))[0];
		inputValues[1] = pcaA.pcaTransform(CBufferedImageToDoubleArray.convertToPixelArray(imageB))[0];

		int[] bins = {255, 255};
		CHistogramND histogram2D = CHistogramND.createHistogram(inputValues, bins);

		double[][] outputValues = new double[bins[0]][bins[1]];

		for(int i=0; i< bins[0]; i++) {
			for(int j=0; i< bins[1]; j++) {
				outputValues[i][j] = histogram2D.binContent[CHistogramND.getBinNumber(new int[]{i,j}, bins)]
						/ histogram2D.histForOneDimension.get(0)[i]
						/ histogram2D.histForOneDimension.get(1)[j]
						/ histogram2D.values;
			}
		}

		BufferedImage output = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster raster = output.getRaster();

		for(int x=0; x<imageA.getWidth(); x++) {
			for(int y=0; y<imageA.getHeight(); y++) {
				double value = outputValues[(int)inputValues[0][x*imageA.getHeight()+y]][(int)inputValues[1][x*imageA.getHeight()+y]] > differenceThreshold ? 0 : 255;
				double[] pixel = { value, value, value};
				raster.setPixel(x, y, pixel);
			}
		}

		return output;
	}

}
