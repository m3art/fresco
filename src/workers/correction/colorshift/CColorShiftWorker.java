/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction.colorshift;

import fresco.CImageContainer;
import fresco.swing.CColorShiftDialog;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;
import image.statiscics.CHistogram;
import workers.correction.CCorrectionWorker;

/**
 *
 * @author gimli
 */
public class CColorShiftWorker extends CCorrectionWorker {

	BufferedImage shifted;
	BufferedImage image;
	BufferedImage pattern;
	double[] dataMean, dataVar, targetMean, targetVar;
	static final Logger logger = Logger.getLogger(CColorShiftWorker.class.getName());

	public CColorShiftWorker(CImageContainer original) {
		CColorShiftDialog dialog = new CColorShiftDialog();
		dialog.setVisible(true);

		MeanAndVar params = dialog.get();

		if (params != null) {
			image = original.getImage();
			targetMean = params.mean;
			targetVar = params.var;
		}
	}

	public CColorShiftWorker(CImageContainer original, CImageContainer pattern) {

		image = original.getImage();
		this.pattern = pattern.getImage();
	}

	@Override
	public String getWorkerName() {
		return "Color shift worker";
	}

	private void setStats() {
		if (targetMean == null) {
			targetMean = CHistogram.getMean(pattern.getData());
			targetVar = CHistogram.getVar(pattern.getData(), targetMean);
		}

		dataMean = CHistogram.getMean(image.getData());
		dataVar = CHistogram.getVar(image.getData(), dataMean);

		shifted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (targetMean == null && pattern == null) {
			throw new CancellationException("Dialog cancelled");
		}

		setStats();
		setProgress(50);

		WritableRaster outRaster = shifted.getRaster();
		int size = outRaster.getWidth() * outRaster.getHeight() * outRaster.getNumBands();
		int[] pixels = new int[size];

		image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), pixels);

		for (int i = 0; i < size; i += outRaster.getNumBands()) {
			for (int b = 0; b < outRaster.getNumBands(); b++) {
				pixels[i + b] = (int) (((double) pixels[i + b] - dataMean[b]) / dataVar[b] * targetVar[b] + targetMean[b]);
				pixels[i + b] = Math.min(Math.max(0, pixels[i + b]), 255);
			}
			setProgress(50 + i * 50 / size);
		}

		outRaster.setPixels(0, 0, outRaster.getWidth(), outRaster.getHeight(), pixels);
		shifted.setData(outRaster);

		return shifted;
	}
}
