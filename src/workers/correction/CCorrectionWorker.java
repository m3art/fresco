/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

import java.awt.image.BufferedImage;
import workers.CImageWorker;

/**
 * @author gimli
 * @version 14.5.2009
 */
public abstract class CCorrectionWorker extends CImageWorker<BufferedImage, Void> {

	public Type getType() {
		return Type.CORRECTOR;
	}

	public String getTypeName() {
		return "CORRECTOR";
	}
}
