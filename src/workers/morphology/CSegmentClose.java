/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.morphology;

import workers.segmentation.CSegmentMap;

/**
 * FIXME: unit test
 * @author gimli
 */
public class CSegmentClose extends CMorphologyWorker {

	CSegmentMap map;

	public CSegmentClose(CSegmentMap _map) {
		map = CSegmentMap.clone(_map);
	}

	@Override
	protected CSegmentMap doInBackground() {
		if (map == null) {
			return map;
		}

		CSegmentBasics worker = new CSegmentBasics(map);
		int i, segments = map.getNumSegments();

		for (i = 0; i < segments; i++) {
			if (map.getSegmentSize(i) > 1000) {
				worker.dilatationStupid(i);
				worker.erosionStupid(i);
				segments = map.getNumSegments();
				setProgress(100 * i / segments);
			}
		}
		return map;
	}

	@Override
	public String getWorkerName() {
		return "Segment closure";
	}
}
