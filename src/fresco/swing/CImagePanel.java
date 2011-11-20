/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

/**
 * @author Honza Blazek
 */
import fresco.CData;
import fresco.CImageContainer;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import workers.segmentation.CSegment;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.logging.Logger;
import workers.segmentation.CSegmentMap;

public class CImagePanel extends CDrawPanel implements Scrollable, MouseListener {

	/** size of the cross mark */
	public static final int CROSS_SIZE = 4;
	/** Identification number of ImagePanel */
	final int id;
	/** handler of image shown in this panel */
	private CImageContainer container;
	/** loaded is true if valid container is set */
	boolean loaded;
	private Point centerPosition;
	private static final Logger logger = Logger.getLogger(CImagePanel.class.getName());

	public CImagePanel(int id) {
		super();
		this.id = id;
		centerPosition = new Point(0, 0);
		loaded = false;
		addMouseListener(this);
	}

	public String getFileName() {
		return container.getFilename();
	}

	public void clear() {
		container = null;
		toDraw = null;
		loaded = false;
	}

	@Override
	public void setSizeByImage() {
		setContainer();
		super.setSizeByImage();
	}

	/** Before each paint is checked correct setting of drawn image. This method
	 * looks at global variables to check current image shown.
	 */
	private void setContainer() {
		if (CData.showImage[id] == -1) {
			return;
		} else if (CData.showImage[id] == -2) {
			container = CData.output;
		} else {
			container = CData.getImage(CData.showImage[id]);
		}

		toDraw = container.getTransformedImage(CData.view);
	}

	@Override
	public void paint(Graphics g) {
		setContainer();

		super.paint(g);
		if (container == null) {
			return;
		}
		if (container.getMarks() != null) {
			paintRegMarks(g);
		}
		if (container.hasSelectedSegment()) {
			showSegment(g);
		}
	}

	/**
	 * Procedure creates color crosses on the picture
	 * @param g graphics for drawing
	 */
	private void paintRegMarks(Graphics g) {
		int x, y;
		Iterator iter = container.getMarks().iterator();
		Color[] markFg = {Color.BLACK, Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW},
				markBg = {Color.WHITE, Color.YELLOW, Color.PINK, Color.BLACK, Color.BLUE};
		Point mark;
		int i = 0;

		while (iter.hasNext()) {
			mark = (Point) iter.next();
			x = (int) (mark.x * CData.getFocus() / 100);
			y = (int) (mark.y * CData.getFocus() / 100);

			g.setColor(markBg[i % markBg.length]);
			g.drawLine(x - CROSS_SIZE - 1, y - CROSS_SIZE - 1, x + CROSS_SIZE + 1, y - CROSS_SIZE - 1);
			g.drawLine(x - CROSS_SIZE - 1, y - CROSS_SIZE - 1, x - CROSS_SIZE - 1, y + CROSS_SIZE + 1);
			g.drawLine(x + CROSS_SIZE + 1, y + CROSS_SIZE + 1, x - CROSS_SIZE - 1, y + CROSS_SIZE + 1);
			g.drawLine(x + CROSS_SIZE + 1, y + CROSS_SIZE + 1, x + CROSS_SIZE + 1, y - CROSS_SIZE - 1);
			g.setColor(markFg[i % markBg.length]);
			g.drawLine(x + CROSS_SIZE, y, x - CROSS_SIZE, y);
			g.drawLine(x, y + CROSS_SIZE, x, y - CROSS_SIZE);
			logger.log(Level.FINEST, "Mark was painted: {0}, {1}", new Object[]{x, y});
			i++;

			char[] number = ((Integer)i).toString().toCharArray();
			g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 3*CROSS_SIZE));
			int width = g.getFontMetrics().charsWidth(number, 0, number.length);
			g.fillRect(x + CROSS_SIZE, y + CROSS_SIZE, CROSS_SIZE + width, 3*CROSS_SIZE);
			if (g.getColor().equals(Color.black)) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.black);
			}
			g.drawRect(x + CROSS_SIZE, y + CROSS_SIZE, CROSS_SIZE + width, 3*CROSS_SIZE);

			g.drawChars(number, 0, number.length, x + CROSS_SIZE + CROSS_SIZE/2, y + 3*CROSS_SIZE + CROSS_SIZE/2);

		}
	}

	/**
	 * This procedure creates marks on the picture
	 * @param x position of mark
	 * @param y position of mark
	 */
	public void putMark(int x, int y) {
		container.putMark(x - centerPosition.x, y - centerPosition.y);
	}

	private void showSegment(Graphics g) {
		int pos_x, pos_y; // position of the pixel in showed image
		CSegment segment = container.getSelectedSegment();
		CSegmentMap map = container.getSegmentMap();
		int x, y;

		for (x = segment.getLowX(); x < segment.getHighX(); x++) {
			for (y = segment.getLowY(); y < segment.getHighY(); y++) {
				if (map.isRegionBorder(segment.getNumber(), x, y)) {
					pos_x = (int) (x * CData.getFocus());
					pos_y = (int) (y * CData.getFocus());
					g.setColor(Color.GREEN);
					g.drawLine(pos_x, pos_y, pos_x, pos_y);
				}
			}
		}
	}

	public void selectSegmentAt(int x, int y) {
		container.selectSegmentAt((int) ((x - centerPosition.x) / CData.getFocus()),
				(int) ((y - centerPosition.y) / CData.getFocus()));
	}

	public void deselectSegment() {
		container.deselectSegment();
	}

	public CImageContainer getContainer() {
		return container;
	}

	public void mouseClicked(MouseEvent e) {
		if (CData.getTool() == null) {
			logger.fine("No action defined");
			return;
		}

		switch (CData.getTool()) {
			case regMarker:
				if (e.getButton() == MouseEvent.BUTTON1) {
					container.putMark((e.getX() - centerPosition.x) * 100 / CData.getFocus(),
							(e.getY() - centerPosition.y) * 100 / CData.getFocus());
					CData.mainFrame.checkEnabled();
					revalidate();
					repaint();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					if (container.getNumOfMarks() > 0) {
						container.removeMark((e.getX() - centerPosition.x) * 100 / CData.getFocus(),
								(e.getY() - centerPosition.y) * 100 / CData.getFocus());
						revalidate();
						repaint();
					} else {
						logger.warning("No marks to remove.");
					}
				}
				break;
		}
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}
