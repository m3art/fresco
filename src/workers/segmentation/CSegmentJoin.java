/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import image.converters.CBufferedImageToIntArray;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;
import utils.vector.CBasic;

/**
 * If you have oversegmented image this class is exactly what you need.
 * We can set maximal error or final count of segments and this class
 * minimizes error for reaching such state. In each step are there joined
 * segments with nearest mean colour up to maximal error or reaching
 * number of segments
 *
 * @author gimli
 * @version 26.5.2009
 */
public class CSegmentJoin extends CSegmentationWorker {

	private int[][][] original;
	private CSegmentMap map, preproMap;
	private int goal = -1, bands;
	private LinkedList<Note> statistics = new LinkedList<Note>();
	private static final Logger logger = Logger.getLogger(CSegmentJoin.class.getName());

	public CSegmentJoin(BufferedImage image, CSegmentMap preprocess_map, int number_of_segments) {
		bands = image.getData().getNumBands();
		original = CBufferedImageToIntArray.convert(image);
		preproMap = preprocess_map;
		goal = number_of_segments;
	}

	@Override
	public int[][] getMap() {
		return map.getSegmentMask();
	}

	@Override
	public LinkedList<CSegment> getSegments() {
		return map.getSegments();
	}

	@Override
	public CSegmentMap getSegmentMap() {
		return map;
	}

	@Override
	public String getWorkerName() {
		return "Segment joining";
	}

	@Override
	/**
	 * In each segment is interesting mean value an variation of colour.
	 * We have to count variations (mean are already set).
	 * Order for joining satisfy:
	 * <ol>
	 * <li>Mean colours of segments are as near as possible - number</li>
	 * <li>Variation after joining is less than in one of joining segments.
	 * Simplification of this condition is possible - mean of one segment is
	 * in range of standard deviation of second segment - range-distance</li>
	 * <li>Size of joined segment in increasing order. Smallest segments has
	 * higher priority</li>
	 * </ol>
	 * These three conditions votes for best candidates.
	 */
	protected CSegmentMap doInBackground() throws Exception {
		int i, j;
		int[] neighs;
		Note mark;
		Note[] notes;
		CSegment joinProduct, A, B;

		map = CSegmentMap.clone(preproMap);
		if (map == null) {
			return null;
		}

		// creates statistics and fill all necessary values
		map.optimize();
		initialize();
		goal = map.getNumSegments() - goal;
		System.out.println("Iters:" + goal);

		for (i = 0; i < goal; i++) {
			if (i != 0 && i % 1000 == 0) {
				map.optimize();
				initialize();
			}

			// sort notes by metric
			notes = new Note[statistics.size()];
			statistics.toArray(notes);
			Arrays.sort(notes, new NoteMetric());

			// join the best candidates
			A = notes[0].getA();
			B = notes[0].getB();
			logger.info("Joining: " + A.getNumber() + ", " + B.getNumber());
			if (map.getNumSegments() == 12098) {
				System.out.println("Stop");
			}
			joinProduct = map.joinSegments(A, B);

			//remark numbers in neighs and add statistics
			remarkNeighs(A, joinProduct.getNumber(), B.getNumber());
			remarkNeighs(B, joinProduct.getNumber(), A.getNumber());

			// count stat for new segment
			joinProduct.setStdErr(getStdErr(joinProduct));

			//remove obsolate
			Iterator<Note> iter = statistics.iterator();
			while (iter.hasNext()) {
				mark = iter.next();
				if (mark.s1.getNumber() == A.getNumber()
						|| mark.s1.getNumber() == B.getNumber()
						|| mark.s2.getNumber() == A.getNumber()
						|| mark.s2.getNumber() == B.getNumber()) {
					iter.remove();
				}
			}

			// add new
			neighs = joinProduct.getNeighbours();
			if (neighs == null) {
				continue;
			}
			for (j = 0; j < neighs.length; j++) {
				B = map.getSegmentByNumber(neighs[j]);
				mark = createNote(joinProduct, B);
				statistics.add(mark);
			}
			setProgress(i * 100 / goal);
		}
		map.optimize();
		System.out.println("#segmentu:" + map.getNumSegments());

		return map;
	}

	private void initialize() {
		int i, j;
		int[] neighs;
		Note mark;
		CSegment pom, segA, segB;

		// initialization
		for (i = 0; i < map.getNumSegments(); i++) {
			pom = map.getSegmentByNumber(i);
			pom.setStdErr(getStdErr(pom));
			map.getNeighs(i);
		}

		// for each pair of candidates fill statistics
		for (i = 0; i < map.getNumSegments(); i++) {
			segA = map.getSegmentByNumber(i);
			neighs = segA.getNeighbours();
			if (neighs == null) {
				continue;
			}
			for (j = 0; j < neighs.length; j++) {
				if (Arrays.binarySearch(map.getSegmentByNumber(neighs[j]).getNeighbours(), i) < 0) {
					logger.severe("Fucking shit: " + i + ", " + neighs[j]);
				}
				if (neighs[j] < i) {
					segB = map.getSegmentByNumber(neighs[j]);
					mark = createNote(segA, segB);
					statistics.add(mark);
				}
			}
		}
	}

	/**
	 * This procedure take all neighs of segment <b>C</b> and in their
	 * neighs list changes number of C to newNumber.
	 * @param C refering segment
	 * @param newNumber number of segment after join
	 */
	private void remarkNeighs(CSegment C, int newNumber, int pairNumber) {
		int[] neighs = C.getNeighbours(), nChange;
		int p, j, i, k = 0;
		int[] shorter;
		CSegment N;
		for (j = 0; j < neighs.length; j++) {
			if (neighs[j] == newNumber || neighs[j] == pairNumber) {
				continue;
			}
			N = map.getSegmentByNumber(neighs[j]);
			nChange = N.getNeighbours();
			p = Arrays.binarySearch(nChange, C.getNumber());
			if (p < 0) {
				logger.info("Double reference of " + newNumber + " in " + N.getNumber() + " removed.");
			} else {
				if (nChange[nChange.length - 1] == newNumber) {
					shorter = new int[nChange.length - 1];
					for (i = 0; i < p; i++) {
						shorter[i] = nChange[i];
					}
					for (i = p; i < nChange.length - 1; i++) {
						shorter[i] = nChange[i + 1];
					}
					N.setNeighbours(shorter);
				} else {
					for (i = p; i < nChange.length - 1; i++) {
						nChange[i] = nChange[i + 1];
					}
					nChange[i] = newNumber;
				}
			}
			//statistics.add(createNote(C,N));
		}
	}

	private Note createNote(CSegment A, CSegment B) {
		int meanDiff = CBasic.norm(CBasic.diff(A.getColor(), B.getColor()));
		double varErr = rangeControl(A, B);
		int sumSize = A.size() + B.size();
		return new Note(A, B, meanDiff, varErr, sumSize);
	}

	/**
	 * Counts standard deviation for colour of segment. Colour mean of segment
	 * have to be set.
	 * @param seg counted segment
	 * @return standard deviation
	 */
	private double[] getStdErr(CSegment seg) {
		int x, y, b;
		double[] err = new double[bands];
		for (x = seg.getLowX(); x < seg.getHighX(); x++) {
			for (y = seg.getLowY(); y < seg.getHighY(); y++) {
				if (map.getNumberAt(x, y) == seg.getNumber()) {
					for (b = 0; b < bands; b++) {
						err[b] += Math.pow(seg.getColor()[b] - original[x][y][b], 2);
					}
				}
			}
		}
		for (b = 0; b < bands; b++) {
			err[b] = Math.sqrt(err[b] / seg.size());
		}
		return err;
	}

	private double rangeControl(CSegment A, CSegment B) {
		int b;
		double errA = 0, errB = 0;

		for (b = 0; b < bands; b++) {
			if (A.getColor()[b] < B.getColor()[b]) {
				errA += (A.getColor()[b] + A.getStdErr()[b]) - B.getColor()[b];
				errB += A.getColor()[b] - (B.getColor()[b] - B.getStdErr()[b]);
			} else {
				errA += B.getColor()[b] - (A.getColor()[b] - A.getStdErr()[b]);
				errB += (B.getColor()[b] + B.getStdErr()[b]) - A.getColor()[b];
			}
		}

		return Math.max(errA, errB);
	}

	private class Note {

		int meanDiff, sumSize;
		double varErr;
		CSegment s1, s2;

		public Note(CSegment s1, CSegment s2, int mean, double var, int size) {
			this.s1 = s1;
			this.s2 = s2;
			meanDiff = mean;
			varErr = var;
			sumSize = size;
		}

		public int getMeanDiff() {
			return meanDiff;
		}

		public double getVarErr() {
			return varErr;
		}

		public int getSumSize() {
			return sumSize;
		}

		public CSegment getA() {
			return s1;
		}

		public CSegment getB() {
			return s2;
		}
	}

	private class NoteMetric implements Comparator<Note> {

		public int compare(Note o1, Note o2) {
			int points = 0;
			if (o1.getMeanDiff() < o2.getMeanDiff()) {
				points++;
			}
			if (o1.getVarErr() > o2.getVarErr()) {
				points++;
			}
			if (o1.getSumSize() < o2.getSumSize()) {
				points++;
			}
			int min, max;
			double ratio1, ratio2;
			min = Math.min(o1.s1.getNumber(), o1.s2.getNumber());
			max = Math.max(o1.s1.getNumber(), o1.s2.getNumber());
			ratio1 = ((double) max) / min;
			min = Math.min(o2.s1.getNumber(), o2.s2.getNumber());
			max = Math.max(o2.s1.getNumber(), o2.s2.getNumber());
			ratio2 = ((double) max) / min;
			if (ratio1 > ratio2) {
				points++;
			}

			if (points > 2) {
				return -1;
			} else if (points == 2) {
				return 0;
			} else {
				return 1;
			}
		}
	}
}
