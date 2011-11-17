/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.morphology;

import fresco.CImageContainer;
import java.util.Arrays;
import workers.segmentation.CSegment;
import java.awt.Point;
import java.util.LinkedList;
import workers.segmentation.CSegmentMap;

/**
 * FIXME: unit tests
 * @author Honza Blazek
 */
public class CSegmentBasics {

	static int[][] N4 = {{0, 1, 0}, {1, 1, 1}, {0, 1, 0}},
			N8 = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
	CSegmentMap map;

	public CSegmentBasics(CImageContainer container) {
		map = container.getSegmentMap();
	}

	public CSegmentBasics(CSegmentMap map) {
		this.map = CSegmentMap.clone(map);
	}

	public void segmentJoin(int id1, int id2) {
		CSegment segA = map.getSegmentByNumber(id1);
		CSegment segB = map.getSegmentByNumber(id1);


	}

	public void erosionStupid(int segment_number) {
		CSegment segment = map.getSegmentByNumber(segment_number);
		LinkedList<Point> remove = new LinkedList<Point>();
		Point pom;
		int x, y, val;

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map.isRegionBorder(segment_number, x, y)) {
					remove.add(new Point(x, y));
				}
			}
		}
		while (!remove.isEmpty()) {
			pom = remove.removeFirst();
			val = chooseAnotherSegment(pom);
			map.addToSegmentPixelAt(pom.x, pom.y, val);
		}
		map.checkArea(segment_number);
	}

	public void dilatationStupid(int segment_number) {
		CSegment segment = map.getSegmentByNumber(segment_number);
		LinkedList<Integer> changed = new LinkedList<Integer>();
		Object[] segs;
		int[][] N = N8;
		LinkedList<Point> added = new LinkedList<Point>();
		Point pom;
		int x, y, i, j, num;

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map.getNumberAt(x, y) == segment.getNumber()) { // each pixel in segment
					for (i = 0; i < 3; i++) {
						for (j = 0; j < 3; j++) // test vicinity of pixel
						{
							if (N[i][j] == 1 && // pixel is c-connected with neig.
									x - 1 + i > 0 && x - 1 + i < map.getWidth() && // neig. pixel is in width range
									y - 1 + j > 0 && y - 1 + j < map.getHeight() && // neig. pixel is in height range
									map.getNumberAt(x - 1 + i, y - 1 + j) != segment_number) {// neig. pixel is background pixel
								added.add(new Point(x - 1 + i, y - 1 + j));
							}
						}
					}
				}
			}
		}
		while (!added.isEmpty()) {
			pom = added.removeFirst();

			map.getSegmentAt(pom.x, pom.y).decrease();
			if (map.getNumberAt(pom.x, pom.y) == 0) {
				System.out.println(pom.x + ", " + pom.y);
			}
			segs = changed.toArray();
			Arrays.sort(segs);
			if (Arrays.binarySearch(segs, map.getNumberAt(pom.x, pom.y)) == -1) {
				changed.add(map.getNumberAt(pom.x, pom.y));
			}

			map.addToSegmentPixelAt(pom.x, pom.y, segment_number);
			map.getSegmentAt(pom.x, pom.y).increase();
		}
		System.out.println("Affected: " + changed.size());
		while (!changed.isEmpty()) {
			num = changed.removeFirst();
			if (num > 0 && map.getSegmentSize(num) > 1) {
				map.checkArea(num);
			}
		}
	}

	private int chooseAnotherSegment(Point pixel) {
		final int neighs = 9;
		int[] vals = new int[neighs];
		int[] num = new int[neighs];
		int i, n, max, val;
		boolean added = false;

		for (n = 0; n < neighs; n++) { // check all neighs
			if (pixel.x - 1 + n % 3 > 0 && pixel.x - 1 + n % 3 < map.getWidth()
					&& pixel.y - 1 + n / 3 > 0 && pixel.y - 1 + n / 3 < map.getHeight()
					&& map.getNumberAt(pixel.x - 1 + n % 3, pixel.y - 1 + n / 3) != map.getNumberAt(pixel.x, pixel.y)) { // testovany pixel je v obrazku
				i = 0;
				added = false;
				while (i < n && vals[i] != 0) { // value is not in list
					if (map.getNumberAt(pixel.x - 1 + n % 3, pixel.y - 1 + n / 3) == vals[i]) {  // jeho hondota je zapsana na i-te pozici
						num[i] += 1 + n % 2; // pixel N4 connected are more valuable
						added = true;
						break;
					}
					i++;
				}
				if (!added) { // jeho hodnota neni v seznamu
					vals[i] = map.getNumberAt(pixel.x - 1 + n % 3, pixel.y - 1 + n / 3); // pridej ho na i-tou pozici
					num[i] += 1 + n % 2;
					added = true;
				}
			}
		}
		max = val = 0;
		for (i = 0; i < neighs; i++) {
			if (num[i] > max) {
				val = vals[i]; // najdi maximalni hodnotu
				max = num[i];
			} else if (num[i] == 0) {
				break;
			}
		}
		return val;
	}
}
