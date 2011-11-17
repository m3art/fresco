/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

/**
 * @author Honza Blazek
 */
import java.awt.image.*;

public class CWaveletWorker extends CAnalysisWorker {

	BufferedImage original;
	int depth;

	public CWaveletWorker(BufferedImage _original, int _depth) {
		original = _original;
		depth = _depth;
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		return CWaveletTransform.transform(original, depth);
	}

	public BufferedImage getGraph() {
		return CWaveletTransform.inverse(CWaveletTransform.transform(original, depth), depth);
	}

	public boolean hasGraph() {
		return true;
	}

	@Override
	public String getWorkerName() {
		return "Wavelet transformation";
	}
}
