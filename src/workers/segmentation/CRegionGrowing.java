/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import image.converters.CBufferedImageToIntArray;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;
import java.util.LinkedList;
import utils.vector.CBasic;

/**
 * @author gimli
 * @version 21.5.2009
 */
public class CRegionGrowing extends CSegmentationWorker {

	public static final int region_size = 1; // three pixels from middle
	private int distance = 8;
	CSegmentMap map;
	int bands;
	int[][][] rgb;

	public CRegionGrowing(BufferedImage input, int dist) {
		Raster raster;
		raster = input.getData();
		bands = raster.getNumBands();
		rgb = CBufferedImageToIntArray.convert(input);
		map = new CSegmentMap(input.getWidth(), input.getHeight(), -1);
		distance = dist;
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
		return "Region growing segmentation";
	}

	@Override
	protected CSegmentMap doInBackground() throws Exception {
		int i, j, segNum = 1;
		CSegment newSegment;

		map.addSegment(new CSegment(0, 0, map.getWidth() - 1, map.getHeight() - 1, 0, map.getHeight() * map.getWidth(), new int[3]));
		for (i = 0; i < map.getWidth(); i++) {
			for (j = 0; j < map.getHeight(); j++) {
				if (map.getNumberAt(i, j) == -1) {
					newSegment = floodFill(i, j, segNum);
					setProgress(100 * newSegment.size() / (map.getWidth() * map.getHeight()));
					map.addSegment(newSegment);
					segNum++;
				}
			}
		}
		return map;
	}

	private CSegment floodFill(int x, int y, int value) {
		int segment_size,
				maxX = -1, maxY = -1, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int[] color = new int[bands];
		LinkedList<Point> new_segment = new LinkedList<Point>();
		Point checked;

		new_segment.add(new Point(x, y)); // first point to check
		segment_size = 0;
		while (!new_segment.isEmpty()) {
			checked = new_segment.removeFirst();
			map.setNumberAt(checked.x, checked.y, value);
			color = CBasic.sum(color, rgb[checked.x][checked.y]);
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

			// add all neibouroughs in range ...
			new_segment.addAll(checkNeighs(checked));
			segment_size++;
		}
		System.out.println("Segment " + value + ": " + segment_size);
		return new CSegment(minX, minY, maxX + 1, maxY + 1, value, segment_size, CBasic.divide(segment_size, color));
	}

	private LinkedList<Point> checkNeighs(Point refPixel) {
		int i, x, y, min = 360, max = 0, rangeLow, rangeHigh, pomHue = -1, lastHue;
		LinkedList<Point> out = new LinkedList<Point>();
		int[] hueHist, newPixel, refPixelColor;

		// store hue values of pixels in different data structure
		refPixelColor = rgb[refPixel.x][refPixel.y];

		hueHist = buildHist(refPixel);
		Arrays.sort(hueHist);
		min = hueHist[0];
		max = hueHist[hueHist.length - 1];
		for (i = 0; i < hueHist.length; i++) {
			if (hueHist[i] == refPixelColor[0]) {
				pomHue = i;
				break;
			}
		}

		// find histogram borders aroun refPixel
		i = pomHue;
		lastHue = refPixelColor[0];
		while (i >= 0) {
			if (hueHist[i] >= lastHue - 1) {
				lastHue = hueHist[i];
				i--;
			} else {
				break;
			}
		}
		rangeLow = hueHist[i + 1] - 1;

		i = pomHue;
		lastHue = refPixelColor[0];
		while (i < hueHist.length) {
			if (hueHist[i] <= lastHue + 1) {
				lastHue = hueHist[i];
				i++;
			} else {
				break;
			}
		}
		rangeHigh = hueHist[i - 1] + 1;

		// add neighs if useful
		if (refPixel.x > 0 && map.getNumberAt(refPixel.x - 1, refPixel.y) == -1) {
			newPixel = rgb[refPixel.x - 1][refPixel.y];
			if (isInSegment(refPixelColor, newPixel, max, min, rangeLow, rangeHigh)) {
				map.setNumberAt(refPixel.x - 1, refPixel.y, -2);
				out.add(new Point(refPixel.x - 1, refPixel.y));
			}
		}
		if (refPixel.y > 0 && map.getNumberAt(refPixel.x, refPixel.y - 1) == -1) {
			newPixel = rgb[refPixel.x][refPixel.y - 1];
			if (isInSegment(refPixelColor, newPixel, max, min, rangeLow, rangeHigh)) {
				map.setNumberAt(refPixel.x, refPixel.y - 1, -2);
				out.add(new Point(refPixel.x, refPixel.y - 1));
			}
		}
		if (refPixel.x + 1 < map.getWidth() && map.getNumberAt(refPixel.x + 1, refPixel.y) == -1) {
			newPixel = rgb[refPixel.x + 1][refPixel.y];
			if (isInSegment(refPixelColor, newPixel, max, min, rangeLow, rangeHigh)) {
				map.setNumberAt(refPixel.x + 1, refPixel.y, -2);
				out.add(new Point(refPixel.x + 1, refPixel.y));
			}
		}
		if (refPixel.y + 1 < map.getHeight() && map.getNumberAt(refPixel.x, refPixel.y + 1) == -1) {
			newPixel = rgb[refPixel.x][refPixel.y + 1];
			if (isInSegment(refPixelColor, newPixel, max, min, rangeLow, rangeHigh)) {
				map.setNumberAt(refPixel.x, refPixel.y + 1, -2);
				out.add(new Point(refPixel.x, refPixel.y + 1));
			}
		}
		return out;
	}

	private int[] buildHist(Point refPixel) {
		int i = 0, x, y,
				xMin = Math.max(refPixel.x - region_size, 0),
				yMin = Math.max(refPixel.y - region_size, 0),
				xMax = Math.min(refPixel.x + region_size + 1, map.getWidth()),
				yMax = Math.min(refPixel.y + region_size + 1, map.getHeight());
		int[] hueHist = new int[(xMax - xMin) * (yMax - yMin)];

		for (x = xMin; x < xMax; x++) {
			for (y = yMin; y < yMax; y++) {
				hueHist[i] = rgb[x][y][0];
				i++;
			}
		}
		return hueHist;
	}

	/**
	 * Defines metric for pixel clasification
	 * @param refPixelHue refering pixel hue in the middle of region [0, 360]
	 * @param newPixelHue new pixel hue - neighbour of refering pixel [0, 360]
	 * @param histMax maximum hue-value with nonzero value in histogram [0, 360]
	 * @param histMin minimum hue-value with nonzero value in histogram [0, 360]
	 * @param rangeLow nearest low hue-value with zero in histogram from refering pixel
	 * @param rangeHigh nearest high hue-value with zero in histogram from refering pixel
	 * @return usability of newPixel in segment
	 */
	private boolean isInSegment(int[] refPixel,
			int[] newPixel,
			int histMax, int histMin,
			int rangeLow, int rangeHigh) {
		return (Math.min(360 - Math.abs(refPixel[0] - newPixel[0]), Math.abs(refPixel[0] - newPixel[0])) < distance
				&& //				newPixel[0] > rangeLow &&
				//				newPixel[0] < rangeHigh &&
				Math.abs(refPixel[1] - newPixel[1]) < distance
				&& Math.abs(refPixel[2] - newPixel[2]) < distance);
	}
}
