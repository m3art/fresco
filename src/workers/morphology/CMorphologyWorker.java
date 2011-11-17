/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.morphology;

import workers.CImageWorker;
import workers.segmentation.CSegmentMap;

/**
 *
 * @author gimli
 */
public abstract class CMorphologyWorker extends CImageWorker<CSegmentMap, Void> {

	@Override
	public Type getType() {
		return Type.MORPH;
	}

	public String getTypeName() {
		return "MORPHOLOGY";
	}
}
