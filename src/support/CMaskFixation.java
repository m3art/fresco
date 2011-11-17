/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package support;

import fresco.*;
import workers.segmentation.CSegmentMap;

/**
 * @author gimli
 * @version 28.6.2009
 */
public class CMaskFixation extends CSupportWorker<Void, Void> {

	CImageContainer A, B;

	public CMaskFixation(CImageContainer A, CImageContainer B) {
		this.A = A;
		this.B = B;
	}

	@Override
	protected Void doInBackground() {
		if (A != null && B != null && B.isSegmented()) {
			A.setSegmentMap(CSegmentMap.clone(B.getSegmentMap()));
		}
		A.recountColourMeans();

		return null;
	}

	@Override
	public String getWorkerName() {
		return "Segment mask fixation.";
	}
}
