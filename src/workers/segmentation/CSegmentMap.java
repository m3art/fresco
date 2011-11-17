/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import java.awt.Point;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import utils.vector.CBasic;

/**
 * @author gimli
 */
public class CSegmentMap {

	private LinkedList<CSegment> segments;
	private int[][] map;

	public CSegmentMap(int sizeX, int sizeY, int mapZeroValue) {
		map = new int[sizeX][sizeY];
		segments = new LinkedList<CSegment>();
		if (mapZeroValue != 0) {
			for (int x = 0; x < getWidth(); x++) {
				for (int y = 0; y < getHeight(); y++) {
					map[x][y] = mapZeroValue;
				}
			}
		}
	}

	public static CSegmentMap clone(CSegmentMap mapa) {
		if (mapa != null) {
			CSegmentMap newMap = new CSegmentMap(mapa.getWidth(), mapa.getHeight(), -1);

			LinkedList<CSegment> original = mapa.getSegments();
			for (int i = 0; i < mapa.getNumSegments(); i++) {
				newMap.addSegment(new CSegment(original.get(i)));
			}

			for (int i = 0; i < mapa.getWidth(); i++) {
				for (int j = 0; j < mapa.getHeight(); j++) {
					newMap.setNumberAt(i, j, mapa.getNumberAt(i, j));
				}
			}

			return newMap;
		} else {
			return mapa;
		}
	}

	public void addSegment(CSegment newSegment) {
		segments.add(newSegment);
	}

	public boolean checkArea(int segment_number) {
		CSegment segment = getSegmentByNumber(segment_number), new_segment = null;
		int x, y, news = 0;

		//initialize
		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map[x][y] == segment_number) {
					map[x][y] = -segment_number;
				}
			}
		}

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map[x][y] == -segment_number) { // which are marked and not associated
					if (news == 0) // first value is old value
					{
						new_segment = floodFill(new Point(x, y), -segment_number, segment_number);
					} else {
						addSegment(floodFill(new Point(x, y), -segment_number, getNumSegments()));
					}
					news++;
				}
			}
		}
		segment = new_segment;
		if (news == 1) {
			return false;
		} else {
			return true;
		}
	}

	public CSegment floodFill(Point start, int look_for, int value) {
		int segment_size,
				maxX = -1, maxY = -1, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		LinkedList<Point> new_segment = new LinkedList<Point>();
		Point checked;

		map[start.x][start.y] = value;
		new_segment.add(new Point(start.x, start.y)); // first point to check
		segment_size = 1;
		while (!new_segment.isEmpty()) {
			checked = new_segment.removeFirst();
			// change border
			if (checked.x > maxX) {
				maxX = checked.x;
			}
			if (checked.y > maxY) {
				maxY = checked.y;
			}
			if (checked.x < minX) {
				minX = checked.x;
			}
			if (checked.y < minY) {
				minY = checked.y;
			}
			// add all neibouroughs
			if (checked.x > 0 && map[checked.x - 1][checked.y] == look_for) {
				map[checked.x - 1][checked.y] = value;
				new_segment.add(new Point(checked.x - 1, checked.y));
			}
			if (checked.y + 1 < getHeight() && map[checked.x][checked.y + 1] == look_for) {
				map[checked.x][checked.y + 1] = value;
				new_segment.add(new Point(checked.x, checked.y + 1));
			}
			if (checked.y > 0 && map[checked.x][checked.y - 1] == look_for) {
				map[checked.x][checked.y - 1] = value;
				new_segment.add(new Point(checked.x, checked.y - 1));
			}
			if (checked.x + 1 < getWidth() && map[checked.x + 1][checked.y] == look_for) {
				map[checked.x + 1][checked.y] = value;
				new_segment.add(new Point(checked.x + 1, checked.y));
			}
			segment_size++;
		}
		return new CSegment(minX, minY, maxX + 1, maxY + 1, value, segment_size, null);
	}

	public void addToSegmentPixelAt(int x, int y, int segment_number) {
		if (segments != null) {
			CSegment segment = getSegmentByNumber(segment_number);

			// border checking
			if (x < segment.getLowX()) {
				segment.setLowX(x);
			}
			if (x >= segment.getHighX()) {
				segment.setHighX(x + 1);
			}
			if (y < segment.getLowY()) {
				segment.setLowY(y);
			}
			if (y >= segment.getHighY()) {
				segment.setHighY(y + 1);
			}
			segment.increase();
		}
		setNumberAt(x, y, segment_number);
	}

	public boolean isRegionBorder(int id, int x, int y) {
		int i, j;
		int[][] N = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
		if (x > getWidth() || y > getHeight() || getNumberAt(x, y) != id) {
			return false;
		}
		for (i = 0; i < 3; i++) {
			for (j = 0; j < 3; j++) // test vicinity of pixel
			{
				if (N[i][j] == 1 && // pixel is c-connected with neig.
						x - 1 + i >= 0 && x - 1 + i < getWidth() && // neig. pixel is in width range
						y - 1 + j >= 0 && y - 1 + j < getHeight() && // neig. pixel is in height range
						getNumberAt(x - 1 + i, y - 1 + j) != id) {// neig. pixel is background pixel
					return true;
				}
			}
		}
		return false;
	}

	public CSegment joinSegments(CSegment A, CSegment B) {
		int[] color = new int[A.getColor().length];
		for (int b = 0; b < color.length; b++) {
			color[b] = (A.getColor()[b] * A.size() + B.getColor()[b] * B.size()) / (A.size() + B.size());
		}
		CSegment joinProduct = new CSegment(Math.min(A.getLowX(), B.getLowX()),
				Math.min(A.getLowY(), B.getLowY()),
				Math.max(A.getHighX(), B.getHighX()),
				Math.max(A.getHighY(), B.getHighY()),
				segments.size(),
				A.size() + B.size(),
				color);
		joinProduct.setNeighbours(CBasic.mergeTwoArraysUnique(A.getNeighbours(), B.getNeighbours(), A.getNumber(), B.getNumber()));

		remarkMap(B.getNumber(), segments.size());
		remarkMap(A.getNumber(), segments.size());
		addSegment(joinProduct);
		return joinProduct;
	}

	public CSegment joinSegments(int IDa, int IDb) {
		return joinSegments(getSegmentByNumber(IDa), getSegmentByNumber(IDb));
	}

	/** This procedure extract border of the segment i O(n)
	 *
	 * @param id - segment identifier
	 * @return LinkedList of points on the border line
	 */
	public LinkedList<Point> getInnerBorder(int id) {
		LinkedList<Point> border = new LinkedList<Point>();
		CSegment segment = getSegmentByNumber(id);
		Point curr = null,
				first = null,
				pos = new Point(segment.getLowX() - 1, segment.getLowY()); // searching position
		int dir = 0; //direction of movement
		int[] Dx = {1, 1, 0, -1, -1, -1, 0, 1};
		int[] Dy = {0, -1, -1, -1, 0, 1, 1, 1};

		if (segment.size() == 0) {
			return null;
		}

		do {
			while (pos.x + Dx[dir] >= segment.getLowX() && pos.y + Dy[dir] >= segment.getLowY()
					&& pos.x + Dx[dir] < segment.getHighX() && pos.y + Dy[dir] < segment.getHighY()
					&& map[pos.x + Dx[dir]][pos.y + Dy[dir]] == id) {
				if (first == null) {
					first = new Point(pos.x, pos.y);
				}
				curr = new Point(pos.x + Dx[dir], pos.y + Dy[dir]);
				border.add(curr);
				dir = (dir + 1) % 8;
			}
			pos.x += Dx[dir];
			pos.y += Dy[dir];
			if (first != null) {
				if (dir % 2 == 1) {
					dir = (dir + 6) % 8;
				} else {
					dir = (dir + 7) % 8;
				}
			}
		} while (first == null || !(pos.x == first.x && pos.y == first.y));

		return border;
	}

	/**
	 * For segment with id creates linked list of all neighbours points
	 * @param id segment identification
	 * @return linked list of all neighbours pixels
	 */
	public LinkedList<Point> getOuterBorder(int id) {
		LinkedList<Point> border = new LinkedList<Point>();
		CSegment segment = getSegmentByNumber(id);
		Point first = null,
				pos = new Point(segment.getLowX() - 1, segment.getLowY()); // searching position
		int dir = 0; //direction of movement
		int[] Dx = {1, 1, 0, -1, -1, -1, 0, 1};
		int[] Dy = {0, -1, -1, -1, 0, 1, 1, 1};

		if (segment.size() == 0) {
			return null;
		}

		do {
			if (pos.x > segment.getHighX()) {
				return null;
			}
			while (pos.x + Dx[dir] >= segment.getLowX() && pos.y + Dy[dir] >= segment.getLowY()
					&& pos.x + Dx[dir] < segment.getHighX() && pos.y + Dy[dir] < segment.getHighY()
					&& map[pos.x + Dx[dir]][pos.y + Dy[dir]] == id) {
				if (first == null) {
					first = new Point(pos.x, pos.y);
				}
				dir = (dir + 1) % 8;
			}
			pos.x += Dx[dir];
			pos.y += Dy[dir];
			if (first != null) {
				border.add(new Point(pos.x, pos.y));
				if (dir % 2 == 1) {
					dir = (dir + 6) % 8;
				} else {
					dir = (dir + 7) % 8;
				}
			}
		} while (first == null || !(pos.x == first.x && pos.y == first.y));

		return border;
	}

	/**
	 * This function go around the segment and mark all segmentID which found
	 * @param segmentID number of refering segment
	 * @return array of segment IDs which defines neighbours of segmentID
	 */
	public int[] getNeighs(int segmentID) {
		int[] output;
		LinkedList<Point> border = new LinkedList<Point>(); //getOuterBorder(segmentID);
		LinkedList<Integer> out = new LinkedList<Integer>();
		int i = 0, j, lastVal = -231;
		Point candidate;

		//if (border == null) return null;

		CSegment seg = segments.get(segmentID);
		for (i = seg.getLowX(); i < seg.getHighX(); i++) {
			for (j = seg.getLowY(); j < Math.min(seg.getHighY() + 1, getHeight()); j++) {
				if (j > 0 && map[i][j - 1] != seg.getNumber()
						&& map[i][j] == seg.getNumber()) {
					border.add(new Point(i, j - 1));
				}
				if (j > 0 && map[i][j - 1] == seg.getNumber()
						&& map[i][j] != seg.getNumber()) {
					border.add(new Point(i, j));
				}
			}
		}

		for (j = seg.getLowY(); j < seg.getHighY(); j++) {
			for (i = seg.getLowX(); i < Math.min(seg.getHighX() + 1, getWidth()); i++) {
				if (i > 0 && map[i - 1][j] != seg.getNumber()
						&& map[i][j] == seg.getNumber()) {
					border.add(new Point(i - 1, j));
				}
				if (i > 0 && map[i - 1][j] == seg.getNumber()
						&& map[i][j] != seg.getNumber()) {
					border.add(new Point(i, j));
				}
			}
		}

		while (!border.isEmpty()) {
			candidate = border.removeFirst();
			if (candidate.x > getWidth() - 1
					|| candidate.x < 0
					|| candidate.y > getHeight() - 1
					|| candidate.y < 0) {
				continue;
			} else {
				out.add(getNumberAt(candidate));
			}
		}

		output = new int[out.size()];
		ListIterator<Integer> iter = out.listIterator();
		for (i = 0; i < out.size(); i++) {
			output[i] = iter.next().intValue();
		}
		Arrays.sort(output);

		out.clear();

		for (i = 0; i < output.length; i++) {
			if (lastVal != output[i]) {
				out.add(output[i]);
				lastVal = output[i];
			}
		}
		output = new int[out.size()];
		iter = out.listIterator();
		for (i = 0; i < out.size(); i++) {
			output[i] = iter.next().intValue();
		}

		segments.get(segmentID).setNeighbours(output);

		return output;
	}

	/**
	 * Removes all nonused segments and renumber map and segments id's
	 */
	public void optimize() {
		boolean[] usedSegments = new boolean[segments.size()];
		LinkedList<CSegment> newSegmentList = new LinkedList<CSegment>();
		CSegment currSegment;
		int x, y, i, newValue;

		for (x = 0; x < getWidth(); x++) {
			for (y = 0; y < getHeight(); y++) {
				usedSegments[map[x][y]] = true;
			}
		}

		newValue = 0;
		for (i = 0; i < segments.size(); i++) {
			if (!usedSegments[i]) {
				continue;
			}

			currSegment = segments.get(i);
			remarkSegment(i, newValue);
			newSegmentList.add(currSegment);
			newValue++;
		}
		segments = newSegmentList;
	}

	/**
	 * Changes all ID values in map with new Value
	 * @param ID identification of segment and old value
	 * @param newValue new ID number for the segment
	 */
	private void remarkSegment(int ID, int newValue) {
		remarkMap(ID, newValue);
		CSegment segment = segments.get(ID);
		segment.setNumber(newValue);
	}

	/**
	 * Creates new segment from old one. The old one finish own existence
	 * @param originalValue old value in map
	 * @param destValue new value in map
	 */
	private int[][] remarkMap(int originalValue, int destValue) {
		CSegment segment = segments.get(originalValue);
		int x, y;

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map[x][y] == originalValue) {
					map[x][y] = destValue;
				}
			}
		}
		return map;
	}

	/* gets and sets */
	public LinkedList<CSegment> getSegments() {
		return segments;
	}

	public int[][] getSegmentMask() {
		return map;
	}

	public int[] getMeanAt(int x, int y) {
		return getSegmentAt(x, y).getColor();
	}

	public void setSegmentMask(int[][] map) {
		this.map = map;
	}

	public void setSegmentList(LinkedList<CSegment> list) {
		segments = list;
	}

	/* number at position */
	public int getNumberAt(Point p) {
		return getNumberAt(p.x, p.y);
	}

	public int getNumberAt(int x, int y) {
		return map[x][y];
	}

	public void setNumberAt(int x, int y, int value) {
		map[x][y] = value;
	}

	/* segment */
	public CSegment getSegmentAt(int x, int y) {
		return segments.get(map[x][y]);
	}

	public CSegment getSegmentByNumber(int number) {
		return segments.get(number);
	}

	public int getSegmentSize(int number) {
		return segments.get(number).size();
	}

	/* size */
	public int getNumSegments() {
		return segments.size();
	}

	public int getWidth() {
		return map.length;
	}

	public int getHeight() {
		return map[0].length;
	}
}
