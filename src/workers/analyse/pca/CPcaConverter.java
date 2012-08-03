/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse.pca;

import image.converters.CBufferedImageToDoubleArray;
import java.awt.image.BufferedImage;
import workers.analyse.CAnalysisWorker;

/**
 *
 * @author gimli
 */
public class CPcaConverter extends CAnalysisWorker {

	BufferedImage input;

	public CPcaConverter(BufferedImage input) {
		this.input = input;
	}

	@Override
	public String getWorkerName() {
		return "Principal compoenet analysis";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		double[][] pixels = CBufferedImageToDoubleArray.convertToPixelArray(input);
		CPca converter = new CPca();

		pixels = converter.pcaTransform(pixels);

		double[] min = new double[pixels[0].length];
		double[] max = new double[pixels[0].length];

		for(int b=0; b<min.length; b++) {
			min[b] = Double.MAX_VALUE;
			max[b] = Double.MIN_VALUE;
		}

		for(double[] pixel: pixels) {
			for(int b=0; b<pixel.length; b++) {
				if (pixel[b]<min[b]) min[b] = pixel[b];
				if (pixel[b]>max[b]) max[b] = pixel[b];
			}
		}

		for(double[] pixel: pixels) {
			for(int b=0; b<pixel.length; b++) {
				pixel[b] = (pixel[b] - min[b])/(max[b]-min[b]) * 255;
			}
		}

		return CBufferedImageToDoubleArray.inverseFromPixelArray(pixels, input.getWidth(), input.getHeight());
	}

}
