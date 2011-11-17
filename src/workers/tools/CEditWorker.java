/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.tools;

import java.awt.image.BufferedImage;
import workers.CImageWorker;

/**
 *
 * @author gimli
 */
public abstract class CEditWorker extends CImageWorker<BufferedImage, Void> {

	@Override
	public String getTypeName() {
		return "Edit image worker";
	}

	public Type getType() {
		return Type.SUPPORT;
	}
}
