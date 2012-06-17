/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.converters.CBufferedImageToDoubleArray;
import workers.analyse.pca.CPcaWorker;
import image.statiscics.CHistogramND;
import java.awt.image.BufferedImage;

/**
 *
 * @author gimli
 */
public class CProbabilityDiffWorker extends CAnalysisWorker {

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

		CPca pcaA = new CPca(CBufferedImageToDoubleArray.convert(imageA));


		CHistogramND histogram2D = CHistogramND.createHistogram(inputValues, bins);
	}

}
