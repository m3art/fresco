/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package support;

import workers.CImageWorker;

/**
 * @author gimli
 * @version 30.6.2009
 */
public abstract class CSupportWorker<T, V> extends CImageWorker<T, V> {

	@Override
	public String getTypeName() {
		return "Support function";
	}

	public CImageWorker.Type getType() {
		return CImageWorker.Type.SUPPORT;
	}
}
