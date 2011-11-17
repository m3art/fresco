/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CData;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * This class represents JPanel with one input - an image. Panel changes own
 * size to resized image by CData.getFocus and is possible to put it into
 * JScrollPane
 * @author gimli
 */
public class CDrawPanel extends JPanel implements Scrollable {

	static int maxUnitIncrement = 20;
	protected BufferedImage toDraw;
	private static final Logger logger = Logger.getLogger(CDrawPanel.class.getName());

	public void setImage(BufferedImage source) {
		toDraw = source;
	}

	public void setSizeByImage() {
		if (toDraw != null) {
			Dimension realSize = new Dimension(toDraw.getWidth(), toDraw.getHeight());
			setSize(realSize.width * CData.getFocus() / 100, realSize.height * CData.getFocus() / 100);
			logger.log(Level.FINE, "Panel resized by new image size: {0}x{1}", new Object[]{getWidth(), getHeight()});
		} else {
			setSize(0, 0);
		}
	}

	@Override
	public void paint(Graphics gr) {
		if (toDraw == null) {
			return;
		}
		gr.drawImage(toDraw, 0, 0, getWidth(), getHeight(), this);
	}

	@Override
	public Dimension getPreferredSize() {
		return getPreferredScrollableViewportSize();
	}

	public Dimension getPreferredScrollableViewportSize() {
		if (toDraw == null) {
			return new Dimension(0, 0);
		}
		return new Dimension(CData.getFocus() * toDraw.getWidth() / 100, CData.getFocus() * toDraw.getHeight() / 100);
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		//Get the current position.
		int currentPosition = 0;
		if (orientation == SwingConstants.HORIZONTAL) {
			currentPosition = visibleRect.x;
		} else {
			currentPosition = visibleRect.y;
		}

		//Return the number of pixels between currentPosition
		//and the nearest tick mark in the indicated direction.
		if (direction < 0) {
			int newPosition = currentPosition - (currentPosition / maxUnitIncrement) * maxUnitIncrement;
			return (newPosition == 0) ? maxUnitIncrement : newPosition;
		} else {
			return ((currentPosition / maxUnitIncrement) + 1) * maxUnitIncrement - currentPosition;
		}
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			return visibleRect.width - maxUnitIncrement;
		} else {
			return visibleRect.height - maxUnitIncrement;
		}
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
