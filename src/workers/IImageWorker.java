/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers;

/**
 *
 * @author gimli
 */
public interface IImageWorker {

	public static enum Type {

		ANALYSIS, SEGMENTATION, REGISTRATION, CORRECTOR, MORPH, SUPPORT
	};

	public abstract Type getType();
}
