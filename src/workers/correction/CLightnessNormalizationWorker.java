/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

import image.converters.CBufferedImageToIntArray;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.concurrent.CancellationException;
import image.statiscics.CHistogram;

/**
 *
 * @author gimli
 */
public class CLightnessNormalizationWorker extends CCorrectionWorker {

	int regionSize;
	double effect, lightnessMean;
	int[][][] data;
	BufferedImage output, input;

	public CLightnessNormalizationWorker(BufferedImage original) {
		input = original;
	}

	private void init() {
		data = CBufferedImageToIntArray.convertToHSL(input);
		output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());

		lightnessMean = CHistogram.getMean(input.getData())[2];
	}

	@Override
	public String getWorkerName() {
		return "Lightness Normalization Worker";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (input == null) {
			throw new CancellationException("Dialog cancelled");
		}

		init();

		WritableRaster raster = output.getRaster();

		throw new UnsupportedOperationException("Not supported yet.");
	}
}
