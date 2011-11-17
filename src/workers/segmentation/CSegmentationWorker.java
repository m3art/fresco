/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import workers.CImageWorker;
import java.util.LinkedList;

public abstract class CSegmentationWorker extends CImageWorker<CSegmentMap, Void> {

	public abstract int[][] getMap();

	public abstract LinkedList<CSegment> getSegments();

	public abstract CSegmentMap getSegmentMap();

	@Override
	public CImageWorker.Type getType() {
		return Type.SEGMENTATION;
	}

	public String getTypeName() {
		return "SEGMENTATOR";
	}
}
