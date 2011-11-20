/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco;

import fresco.swing.CImagePanel;
import java.util.logging.Level;
import workers.segmentation.*;
import image.converters.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.LinkedList;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import utils.vector.CBasic;

/**
 * @author Honza Blazek
 * ImageContainer is class for work with images in application it contains 3
 * sizes of original image preview - the smallest image, zoomed which size is
 * dependent on zoom_size and original
 */
public class CImageContainer {

	public static enum Screen {

		MEANED, ORIGINAL, FRIST_BAND, SECOND_BAND, THIRD_BAND, GRAY_SCALE
	};
	/** Size of icon in left column */
	private static int ICON_HEIGHT = 35;
	/** Caches of used images - better response when analysis is made */
	private BufferedImage original, gray, meaned, band1, band2, band3;
	/** Cached icon */
	private ImageIcon icon;
	/** Points added into image - registration purposes */
	private LinkedList<Point> regMarks;
	/** Source of image */
	private File file;
	/** Array of integers. Each pixel belongs to one segment */
	private CSegmentMap map;
	/** Marked segment is shown */
	private int selectedSegment = -1;
	/** If image is changed, before repaint cached image is recomputed. */
	private boolean changed = false;
	/** Logging tasks */
	private static final Logger logger = Logger.getLogger(CImageContainer.class.getName());

	public CImageContainer(BufferedImage picture, boolean ref) {
		if (ref) {
			original = picture;
		} else {
			original = new BufferedImage(picture.getWidth(), picture.getHeight(), picture.getType());
			WritableRaster raster = original.getRaster();
			picture.copyData(raster);
			original.setData(raster);
		}
		renewIcon();
		map = null;
	}

	public CImageContainer(CImageContainer container) throws IOException {
		if (container == null) {
			throw new IOException();
		} else {
			if (container.getSegmentMap() != null) {
				map = CSegmentMap.clone(container.getSegmentMap());
				(new CMeanBySegmentWorker(this)).execute();
			} else {
				map = null;
			}
			original = container.getImage();
			regMarks = container.getMarks();

			renewIcon();
		}
	}

	/**
	 * Constructor
	 * @param picture is loaded picture
	 * @param file is preferred filename
	 */
	public CImageContainer(BufferedImage picture, File file) throws IOException {
		int i, j;
		int[] pixel = new int[picture.getData().getNumBands()];
		if (picture.getType() == 0) {
			throw new IOException();
		} else if (picture.getData().getNumBands() != 3) {
			original = new BufferedImage(picture.getWidth(), picture.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			WritableRaster orig = original.getRaster();
			Raster raster = picture.getData();
			for (i = 0; i < picture.getWidth(); i++) {
				for (j = 0; j < picture.getHeight(); j++) {
					raster.getPixel(i, j, pixel);
					orig.setPixel(i, j, C2ThreeBands.convert(pixel));
				}
			}
			logger.fine("Converted to three bands");
		} else {
			original = picture;
		}
		this.file = file;
		//createPreview();
		renewIcon();
	}

	public void recountColourMeans() {
		int[][] mapa = map.getSegmentMask();
		CSegment[] list = new CSegment[map.getNumSegments()];
		Raster raster = original.getData();
		int x, y, c, b;
		int[] pixel = new int[raster.getNumBands()], sum = new int[raster.getNumBands()];

		map.getSegments().toArray(list);

		for (int i = 0; i < list.length; i++) {
			c = 0;
			for (b = 0; b < sum.length; b++) {
				sum[b] = 0;
			}
			for (x = list[i].getLowX(); x < list[i].getHighX(); x++) {
				for (y = list[i].getLowY(); y < list[i].getHighY(); y++) {
					raster.getPixel(x, y, pixel);
					if (mapa[x][y] == list[i].getNumber()) {
						sum = CBasic.sum(pixel, sum);
						c++;
					}
				}
			}
			list[i].setColor(CBasic.divide(c, sum));
		}

		(new CMeanBySegmentWorker(this)).execute();
	}

	public final void renewIcon() {
		int width = getWidth() * ICON_HEIGHT / getHeight();
		icon = new ImageIcon(original.getScaledInstance(width, ICON_HEIGHT, BufferedImage.SCALE_FAST));
	}

	/**
	 * @return icon for container
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * Function getImage
	 * @return original image in container
	 */
	public BufferedImage getImage() {
		return original;
	}

	/**
	 * Imports to the container new image
	 * common usage is to replace original image by transformed
	 */
	public void setImage(BufferedImage image) {
		if (isSegmented()
				&& (image.getWidth() != original.getWidth()
				|| image.getHeight() != original.getHeight())) {
			map = null;
		}
		original = image;
		renewIcon();
		change();
	}

	public int getWidth() {
		return original.getWidth();
	}

	public int getHeight() {
		return original.getHeight();
	}

	public Dimension getSize() {
		return new Dimension(original.getWidth(), original.getHeight());
	}

	public int[] getMeanAt(int x, int y) {
		if (map.getMeanAt(x, y) == null) {
			//checkSegment(map.getNumberAt(x, y));
			return new int[3];
		}
		return map.getSegmentAt(x, y).getColor();
	}

	public BufferedImage getTransformedImage(Screen screen) {
		switch (screen) {
			case ORIGINAL:
				return original;
			case MEANED:
				if (map != null && (changed || meaned == null)) {
					logger.log(Level.FINE, "Transforming image to {0}", screen);
					(new CMeanBySegmentWorker(this)).execute();
				}
				if (meaned != null) {
					return meaned;
				} else {
					return original;
				}
			case GRAY_SCALE:
				if (changed || gray == null) {
					logger.log(Level.FINE, "Transforming image to {0}", screen);
					gray = (new Crgb2gray()).convert(original);
				}
				return gray;
			case FRIST_BAND:
				if (changed || band1 == null) {
					logger.log(Level.FINE, "Cutting red channel.");
					band1 = Crgb2OneBand.convert(original, 0);
				}
				return band1;
			case SECOND_BAND:
				if (changed || band2 == null) {
					logger.log(Level.FINE, "Cutting green channel.");
					band2 = Crgb2OneBand.convert(original, 1);
				}
				return band2;
			case THIRD_BAND:
				if (changed || band3 == null) {
					logger.log(Level.FINE, "Cutting blue channel.");
					band3 = Crgb2OneBand.convert(original, 2);
				}
				return band3;
		}
		// unreachable state
		return null;
	}

	public void setMeaned(BufferedImage convertedImage) {
		meaned = convertedImage;
	}

	/**
	 * This procedure creates marks on the picture
	 * @param x position of mark
	 * @param y position of mark
	 */
	public void putMark(int x, int y) {
		if (regMarks == null) {
			regMarks = new LinkedList<Point>();
		}

//		if (regMarks.size() < 4) {
			regMarks.add(new Point(x, y));
			logger.log(Level.FINE, "Mark put at: [{0}, {1}]", new Object[]{x, y});
//		}
	}

	/**
	 * Mark specified by coordinates will be removed. If it does not exist last
	 * added mark will be removed.
	 * @param x
	 * @param y
	 */
	public void removeMark(int x, int y) {
		for (Point mark : regMarks) {
			if (Math.abs(mark.x - x) < CImagePanel.CROSS_SIZE * 2 / (double) CData.getFocus() * 100
					&& Math.abs(mark.y - y) < CImagePanel.CROSS_SIZE * 2 / (double) CData.getFocus() * 100) {
				regMarks.remove(mark);
				return;
			}
		}
		removeLastMark();
	}

	/**
	 * Procedure which correct segment - resets all and check size, go through all pixels
	 */
	public void checkSegment(int id) {
		CSegment segment = map.getSegmentByNumber(id);
		Raster input = original.getData();
		int[] pixel = new int[input.getNumBands()], color = new int[input.getNumBands()];
		int x, y, segment_size = 0;

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map.getNumberAt(x, y) == id) {
					input.getPixel(x, y, pixel);
					color = CBasic.sum(pixel, color);
					segment_size++;
				}
			}
		}
		segment.setColor(CBasic.divide(segment_size, color));
		segment.setSize(segment_size);
	}

	/* gets and sets */
	public void removeLastMark() {
		regMarks.removeLast();
	}

	public int getNumOfMarks() {
		if (regMarks == null) {
			return 0;
		} else {
			return regMarks.size();
		}
	}

	public LinkedList<Point> getMarks() {
		return regMarks;
	}

	public void setMarks(LinkedList<Point> marks) {
		regMarks = marks;
	}

	public File getFile() {
		return file;
	}

	public String getFilename() {
		return file.getName();
	}

	public void setFilename(String filename) {
		file = new File(filename);
	}

	public void setSegmentMap(CSegmentMap _map) {
		map = _map;
		(new CMeanBySegmentWorker(this)).execute();
	}

	public CSegmentMap getSegmentMap() {
		return map;
	}

	public CSegment getSelectedSegment() {
		return map.getSegmentByNumber(selectedSegment);
	}

	public void selectSegmentAt(int x, int y) {
		selectedSegment = map.getSegmentAt(x, y).getNumber();
	}

	public void deselectSegment() {
		selectedSegment = -1;
	}

	public boolean hasSelectedSegment() {
		return (selectedSegment != -1);
	}

	public boolean isSegmented() {
		return (map != null && map.getNumSegments() != 0);
	}

	public boolean isChanged() {
		return changed;
	}

	public void change() {
		changed = true;
	}
}
