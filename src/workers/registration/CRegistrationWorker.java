/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import workers.CImageWorker;
import java.awt.image.BufferedImage;

/**
 * @author Honza Blazek
 */
public abstract class CRegistrationWorker extends CImageWorker<BufferedImage, Void> {

	@Override
	public Type getType() {
		return Type.REGISTRATION;
	}

	public String getTypeName() {
		return "REGISTRATION";
	}
}
