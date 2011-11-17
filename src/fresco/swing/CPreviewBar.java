/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */

package fresco.swing;

import fresco.CData;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import sun.awt.VerticalBagLayout;

/**
 *
 * @author gimli
 */
public class CPreviewBar extends JScrollPane {

	JPanel picturePanel;
	CPopupMenu popupMenu;
	private static final Logger logger = Logger.getLogger(CPreviewBar.class.getName());

	public CPreviewBar() {
		picturePanel = new JPanel(new VerticalBagLayout());
		picturePanel.setBackground(new Color(0xDEEAFF));
		getViewport().add(picturePanel);
	}

	public void refreshBar() {
		JButton button;
		int i;
		getViewport().removeAll();
		logger.fine("Refreshing "+CData.imagesSize()+" icons");
		picturePanel.removeAll();
		for (i = picturePanel.getComponentCount(); i < CData.imagesSize(); i++) {
			button = new JButton(CData.getImage(i).getFilename(),
				CData.getImage(i).getIcon());
			button.setToolTipText(CData.getImage(i).getFilename());
			button.setPreferredSize(new Dimension(picturePanel.getVisibleRect().width-10,80));
			button.setFont (new Font ("Arial", Font.PLAIN, 12));
			// text alignment
			button.setHorizontalTextPosition(JButton.CENTER);
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			// picture indexing
			button.setName(String.valueOf(i));
			// what to do
			button.setActionCommand("loadImage");
			// service
			button.addActionListener(CData.userActionListener);
			// popupMenu
			popupMenu = new CPopupMenu(this,i);
			button.addMouseListener(popupMenu);
			// show
			picturePanel.add(button);
		}
		getViewport().add(picturePanel);
		repaint();
	}

}
