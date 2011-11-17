/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import java.awt.Point;

/**
 * Class for realization of segment
 */
public class CSegment {

	private Point[] border;         // left top corner and right bottom of segment
	private int segment_number, // idetifier in segment map
			segment_size;       // # pixels in segment
	private int[] color;            // there is possible to define color for visualisation
	private int[] neighbours = null;		// defines adjacent segments
	private double[] stdErr;

	public CSegment(int min_x, int min_y, int max_x, int max_y, int num, int size, int[] _color) {
		border = new Point[2];
		border[0] = new Point(min_x, min_y);
		border[1] = new Point(max_x, max_y);
		segment_number = num;
		segment_size = size;
		if (_color != null) {
			color = new int[_color.length];
			for (int b = 0; b < _color.length; b++) {
				color[b] = _color[b];
			}
		}
	}

	public CSegment(CSegment original) {
		border = new Point[2];
		border[0] = new Point(original.getLowX(), original.getLowY());
		border[1] = new Point(original.getHighX(), original.getHighY());
		segment_number = original.getNumber();
		segment_size = original.size();
		color = new int[original.getColor().length];
		for (int i = 0; i < original.getColor().length; i++) {
			color[i] = original.getColor()[i];
		}
	}

	public void increase() {
		segment_size++;
	}

	public void decrease() {
		segment_size--;
	}

	public int getLowX() {
		return border[0].x;
	}

	public void setLowX(int val) {
		border[0].x = val;
	}

	public int getLowY() {
		return border[0].y;
	}

	public void setLowY(int val) {
		border[0].y = val;
	}

	public int getHighX() {
		return border[1].x;
	}

	public void setHighX(int val) {
		border[1].x = val;
	}

	public int getHighY() {
		return border[1].y;
	}

	public void setHighY(int val) {
		border[1].y = val;
	}

	public int getNumber() {
		return segment_number;
	}

	public void setNumber(int _number) {
		segment_number = _number;
	}

	public int[] getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(int[] neighs) {
		neighbours = neighs;
	}

	public int size() {
		return segment_size;
	}

	public void setSize(int _size) {
		segment_size = _size;
	}

	public int[] getColor() {
		return color;
	}

	public void setColor(int[] _color) {
		color = _color;
	}

	public double[] getStdErr() {
		return stdErr;
	}

	public void setStdErr(double[] _stdErr) {
		stdErr = _stdErr;
	}
}
