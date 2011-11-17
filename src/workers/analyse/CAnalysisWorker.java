/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import workers.CImageWorker;
import java.awt.image.BufferedImage;

/**
 *
 * @author gimli
 */
public abstract class CAnalysisWorker extends CImageWorker<BufferedImage, Void> {

	public CImageWorker.Type getType() {
		return Type.ANALYSIS;
	}

	public String getTypeName() {
		return "ANALYSIS";
	}
}
