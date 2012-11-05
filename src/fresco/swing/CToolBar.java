/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import file.CImageFile;
import fresco.CData;
import fresco.action.IAction.RegID;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 *
 * @author gimli
 */
public class CToolBar extends JToolBar implements IFrescoComponent {

	public static enum Tool {

		regMarker, segmentSelector, regionSelector
	}
	public static String[] IconSet = {
		"/icons/open2.png",
		"/icons/save2.png",
		"/icons/loupe.png",
		"/icons/rotLeft.png",
		"/icons/rotRight.png",
		"/icons/marker18x18.png",
    "/icons/multInt.png",
  };
	CButton open, save, rotLeft, rotRight, runMult;
	JLabel loupe;
	JToggleButton regMarks;
	CZoomComboBox scaler;

	public CToolBar(String name) {
		super(name, JToolBar.HORIZONTAL);
		ImageIcon[] icons = loadIcons();

		open = new CButton((ImageIcon) icons[0], RegID.imageOpen);
		open.addActionListener(CData.userActionListener);
		add(open);

		save = new CButton((ImageIcon) icons[1], RegID.imageSave);
		save.addActionListener(CData.userActionListener);
		add(save);

		loupe = new JLabel(icons[2]);
		add(loupe);

		scaler = new CZoomComboBox();
		scaler.addItemListener(CData.userActionListener);
		add(scaler);

		rotLeft = new CButton((ImageIcon) icons[3], RegID.rotateLeft);
		rotLeft.addActionListener(CData.userActionListener);
		rotLeft.setToolTipText("Rotate image left");
		add(rotLeft);

		rotRight = new CButton((ImageIcon) icons[4], RegID.rotateRight);
		rotRight.addActionListener(CData.userActionListener);
		rotRight.setToolTipText("Rotate image right");
		add(rotRight);

		regMarks = new CToggleButton((ImageIcon) icons[5], RegID.registerMarks);
		regMarks.addActionListener(CData.userActionListener);
		regMarks.setToolTipText("Marks useful for registration - simple click into image.");
		add(regMarks);
    
    runMult = new CButton((ImageIcon) icons[6], RegID.runMultipleIntPoints);
		runMult.addActionListener(CData.userActionListener);
		add(runMult);
    
    
	}

	private static ImageIcon[] loadIcons() {
		ImageIcon[] icons = new ImageIcon[IconSet.length];

		for (int i = 0; i < IconSet.length; i++) {
			icons[i] = CImageFile.createImageIcon(IconSet[i], IconSet[i]);
		}

		return icons;
	}

	public void checkEnabled() {
		if (CData.showImage[0] == -1) {
			save.setEnabled(false);
			rotLeft.setEnabled(false);
			rotRight.setEnabled(false);
		} else {
			save.setEnabled(true);
			rotLeft.setEnabled(true);
			rotRight.setEnabled(true);
		}

		if (CData.showImage[0] == -1 && CData.showImage[1] == -1 && CData.showImage[2] == -1) {
			scaler.setEnabled(false);
		} else {
			scaler.setEnabled(true);
		}
	}
}
