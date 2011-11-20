/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.action;

import fresco.CData;
import fresco.CImageContainer.Screen;
import fresco.action.IAction.RegID;
import fresco.swing.CButton;
import fresco.swing.CCheckBoxMenuItem;
import fresco.swing.CMenuItem;
import fresco.swing.CPopupMenuItem;
import fresco.swing.CRadioMenuItem;
import fresco.swing.CToggleButton;
import fresco.swing.CToolBar.Tool;
import fresco.swing.CZoomComboBox;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * All user actions are resolved here
 * @author gimli
 */
public class CUserActionListener implements ActionListener, ItemListener {

	private static final Logger logger = Logger.getLogger(CUserAction.class.getName());
	private CActionManager manager;
	private CUserAction action = new CUserAction();

	public void setManager(CActionManager manager) {
		this.manager = manager;
	}

	public CActionManager getManager() {
		return manager;
	}

	/**
	 * Solves all user events
	 * @param e user event which runs some action
	 */
	public void actionPerformed(ActionEvent e) {
		try {
			// simple menubar actions
			if (e.getSource() instanceof CMenuItem) {
				action.execute((((CMenuItem) e.getSource()).getID()));
			} // types of view
			else if (e.getSource() instanceof CRadioMenuItem) {
				action.setView(((CRadioMenuItem) e.getSource()).getID(), ((CRadioMenuItem) e.getSource()).isSelected());
			} // 2nd input showing
			else if (e.getSource() instanceof CCheckBoxMenuItem) {
				action.setView(((CCheckBoxMenuItem) e.getSource()).getID(), ((CCheckBoxMenuItem) e.getSource()).isSelected());
			} // tool bar actions
			else if (e.getSource() instanceof CButton || e.getSource() instanceof CToggleButton) {
				action.execute((((IAction) e.getSource()).getID()));
			} // preview bar actions
			else if (e.getActionCommand().equals("loadImage") && e.getSource() instanceof JButton) {
				if (e.getModifiers() % 16 != Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
					action.loadContainer(0, Integer.parseInt(((JButton) e.getSource()).getName()));
				} else {
					action.loadContainer(2, Integer.parseInt(((JButton) e.getSource()).getName()));
				}
			} // popup menu actions
			else if (e.getSource() instanceof CPopupMenuItem) {
				manager.containerDo(((IAction) e.getSource()).getID(), ((CPopupMenuItem) e.getSource()).getRef());
			}
		} catch (OutOfMemoryError oome) {
			JOptionPane.showMessageDialog(new JFrame(), "Process stopped: Java is out of memory (" + Runtime.getRuntime().totalMemory() / (1000000) + "MB used).\n"
					+ "Possible solution: \n"
					+ "1) Downscale your images\n"
					+ "2) Extend PC memory\n"
					+ "3) Replace java -Xmx argument\n",
					"Process warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * user defined values may change there
	 * @param e
	 */
	public void itemStateChanged(ItemEvent e) {
		if (((IAction) e.getSource()).getID() == RegID.zoom) {
			int value = ((CZoomComboBox) e.getSource()).getParam();
			if (value != CData.getFocus()) {
				action.applyZoom(value);
			}
		}
	}

	private class CUserAction {

		public void execute(RegID id) {
			String s;

			if (manager == null) {
				return;
			}

			switch (id) {
				case imageOpen:
					logger.info("Image open executed");
					manager.loadImage();
					break;
				case show2ndInput:
					logger.info("Reseting visibility of 2nd input");
					manager.show2ndInput();
					break;
				case imageSave:
					logger.info("Image save executed");
					manager.saveAsPicture(CData.showImage[0]);
					break;
				case imageInfo:
					logger.info("Image info executed");
					manager.showInfo();
					break;
				case kill:
					manager.killProcess();
					break;
				case rename:
					manager.renamePicture(CData.showImage[0]);
					break;
				case fixImage:
					manager.fixImage();
					break;
				case capture:
					manager.captureImage();
					break;
				case sobel:
					manager.runImageWorker(id, null);
					break;
				case rotateLeft:
				case rotateRight:
				case verticalFlip:
				case mirrorFlip:
				case diff:
//					throw new UnsupportedOperationException("Not yet implemented");
				case register:
					manager.runImageWorker(id, null);
					break;
				case ahe:
					manager.runImageWorker(id, null);
					break;
				case colorShift:
					manager.runImageWorker(id, null);
					break;
				case patternAnalyzer:
					manager.runImageWorker(id, null);
					break;
				case mutualInfo:
					// FIXME: params band1 and band2 are necessary
					Object[] params = {(Integer) 1, (Integer) 2};
					manager.runImageWorker(id, params);
					break;
				case colorQuantization:
					// create input params
					Object[] colorNo;
					s = (String) JOptionPane.showInputDialog(
							new JFrame("Number of output colors ..."),
							"Set number of output color:", "Number of output colors",
							JOptionPane.PLAIN_MESSAGE, null, null, "100");

					if ((s != null) && (s.length() > 0)) {

						int size = Integer.parseInt(s);

						colorNo = new Object[1];
						colorNo[0] = size;
						manager.runImageWorker(id, colorNo);
						break;
					}
					break;
				case laplace:
					// create input params
					Object[] objects;
					s = (String) JOptionPane.showInputDialog(
							new JFrame("Size of laplacian matrix ..."),
							"Set size of matrix (3 or 5)", "Size of laplacian matrix ...",
							JOptionPane.PLAIN_MESSAGE, null, null, "5");

					if ((s != null) && (s.length() > 0)) {

						int size = Integer.parseInt(s);

						objects = new Object[1];
						objects[0] = size;
						manager.runImageWorker(id, objects);
						break;
					}
				case registerMarks:
					CData.setTool(Tool.regMarker);
					logger.log(Level.INFO, "New tool selected: {0}", CData.getTool().name());
					break;
				case registrationMarkSearch:
					manager.runImageWorker(RegID.registrationMarkSearch, null);
					break;
			}
		}

		public void applyZoom(int focus) {
			logger.log(Level.FINE, "Applying zoom: {0}", focus);
			manager.setZoom(focus);
		}

		/**
		 * Puts loaded image into visible imagePanel
		 * @param panelID specifies panel which obtain new container
		 * @param imageID defines container
		 */
		public void loadContainer(int panelID, int imageID) {
			logger.log(Level.INFO, "Loading container {0} into {1} panel", new Object[]{imageID, panelID});
			manager.loadContainer(panelID, imageID);
		}

		/**
		 * Reset current view to new view type
		 * @param regID which type of view will be set
		 * @param selected value of menu item in this state
		 */
		private void setView(RegID regID, boolean selected) {
			logger.log(Level.INFO, "Setting view {0}", regID);

			switch (regID) {
				case show2ndInput:
					if ((selected && !CData.input2Showed)
							|| (!selected && CData.input2Showed)) {
						manager.show2ndInput();
					}
					break;
				case band1View:
					logger.info("Reseting view to first band");
					manager.switchView(Screen.FRIST_BAND);
					break;
				case band2View:
					logger.info("Reseting view to 2nd band");
					manager.switchView(Screen.SECOND_BAND);
					break;
				case band3View:
					logger.info("Reseting view to 3rd band");
					manager.switchView(Screen.THIRD_BAND);
					break;
				case grayView:
					logger.info("Reseting view to grayscale");
					manager.switchView(Screen.GRAY_SCALE);
					break;
				case meanedView:
					logger.info("Reseting view to mean value");
					manager.switchView(Screen.MEANED);
					break;
				case originalView:
					logger.info("Reseting view to original values");
					manager.switchView(Screen.ORIGINAL);
					break;
				default:
					manager.setContentStructure(regID);
					break;
			}
		}
	}
}
