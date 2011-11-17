/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Different JScrollPanes can be synchronized by this change listener
 *
 * @author gimli
 */
public class CScrollPaneSyncListener implements ChangeListener {

	private Point position = new Point(0, 0);
	private static final Logger logger = Logger.getLogger(CScrollPaneSyncListener.class.getName());
	private LinkedList<JViewport> managedViewports = new LinkedList<JViewport>();

	/**
	 * Basic API for adding new JViewport to manage. Listener is automatically
	 * generated and added to specified viewport
	 * @param viewportToManage new viewport for synchronization
	 */
	public void manage(JViewport viewportToManage) {
		managedViewports.add(viewportToManage);
		alignImageViewport(viewportToManage, position);
		viewportToManage.addChangeListener(this);
	}

	/**
	 * Method verify source of change event and if the source parent (JScrollPane)
	 * is component with mouse cursor, position change is valid and synchronization
	 * is made
	 *
	 * @param e change of position of JViewport
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation(), sourceLocation;
		JViewport source = (JViewport) e.getSource();

		if (source.isShowing()) {
			sourceLocation = source.getLocationOnScreen();
			mouseLocation.x -= sourceLocation.x;
			mouseLocation.y -= mouseLocation.y;
		} else {
			return;
		}

		if (!source.getParent().contains(mouseLocation.x, mouseLocation.y)) {
			logger.log(Level.FINEST, "Mouse position: {0} Component position: {1}",
					new Object[]{MouseInfo.getPointerInfo().getLocation().toString(),
						source.getSize().toString()});
			return;
		}

		int x = source.getViewPosition().x,
				y = source.getViewPosition().y;

		if (x != position.x || y != position.y) {
			position.setLocation(x, y);
			for (JViewport viewport : managedViewports) {
				if (viewport != source) {
					alignImageViewport(viewport, position);
				}
			}
		}
	}

	/**
	 * Image is necessary to keep in ScrollPane and also we want to have synchronized
	 * moving of all scroll panes. In this method alignment is done
	 * @param imageId aligned imageWindow
	 * @param position ideal position of Viewport in ScrollPane
	 */
	private void alignImageViewport(JViewport viewport, Point position) {
		if (viewport.getComponentCount() == 0) { // no image is set
			return;
		}

		Dimension size = viewport.getViewSize();
		Dimension imageSize = ((Scrollable) viewport.getComponent(0)).getPreferredScrollableViewportSize();
		Dimension viewSize = viewport.getExtentSize();
		int maxX = size.width - Math.min(viewSize.width, imageSize.width);
		int maxY = size.height - Math.min(viewSize.height, imageSize.height);

		Point viewPortPosition = new Point(Math.min(maxX, position.x), Math.min(maxY, position.y));

		viewport.setViewPosition(viewPortPosition);
		viewport.invalidate();
	}
}
