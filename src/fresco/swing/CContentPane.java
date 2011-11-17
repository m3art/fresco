/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CData;
import fresco.action.CActionManager;
import fresco.action.CStatusManager;
import fresco.action.IAction.RegID;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;

/**
 * Central panel of the application - tzv RootPane
 * @author gimli
 */
public class CContentPane extends JPanel implements IFrescoComponent {

	public static enum Structure {

		tabbed, horizontal, vertical, blend, oneInput
	};
	/** image windows with scrolling */
	public JScrollPane imageWindow[] = new JScrollPane[4];
	/** content of scroll panes */
	public CDrawPanel imagePanel[] = {new CImagePanel(0), new CImagePanel(1), new CImagePanel(2), new CBlendComponent()};
	/** Panes containing all showed images */
	private JSplitPane display;
	private JComponent inputs;
	/** Shortcut pane for managing images */
	private CPreviewBar previewBar;
	private CStatusManager statusBar;
	private CToolBar toolBar;
	/** Synchronization of image scroll panes */
	CScrollPaneSyncListener positionListener;
	private static final Logger logger = Logger.getLogger(CContentPane.class.getName());
	private Structure currStructure, twoImageStructure = Structure.horizontal;

	public CContentPane() {
		super(new BorderLayout());

		positionListener = new CScrollPaneSyncListener();

		add(toolBar = new CToolBar("main toolbar"), BorderLayout.NORTH);
		previewBar = new CPreviewBar();
		previewBar.setPreferredSize(new Dimension(80, 1));
		add(previewBar, BorderLayout.WEST);
		statusBar = new CStatusManager();
		statusBar.setPreferredSize(new Dimension(100, 18));
		add(statusBar, BorderLayout.SOUTH);

		for (int i = 0; i < 3; i++) {
			imagePanel[i].setMinimumSize(new Dimension(0, 0));
			imageWindow[i] = new JScrollPane(imagePanel[i]);
			imageWindow[i].setMinimumSize(new Dimension(0, 0));
			positionListener.manage(imageWindow[i].getViewport());
			imageWindow[i].getViewport().setName("Viewport " + i);
		}

		imageWindow[3] = new JScrollPane();
		imageWindow[3].setMinimumSize(new Dimension(0, 0));
		positionListener.manage(imageWindow[3].getViewport());
		imageWindow[3].getViewport().setName("Viewport " + 3);

		CActionManager manager = new CActionManager(imagePanel, this, previewBar, statusBar);
		CData.userActionListener.setManager(manager);
		statusBar.setManager(manager);

		setStructure(Structure.oneInput);

		for (int i = 0; i < 4; i++) {
			imageWindow[i].setBorder(new LineBorder(Color.BLACK));
		}
	}

	public void setStructure(RegID regID) {

		switch (regID) {
			case horizontalSplit:
				currStructure = Structure.horizontal;
				break;
			case verticalSplit:
				currStructure = Structure.vertical;
				break;
			case tabbed:
				currStructure = Structure.tabbed;
				break;
			case alphaBlend:
				currStructure = Structure.blend;
				break;
			case oneInput:
				currStructure = Structure.oneInput;
			default:
				return;
		}

		setStructure(currStructure);
	}

	public final void setStructure(Structure structure) {
		setInputsStructure(structure);
		if (structure != Structure.oneInput) {
			twoImageStructure = structure;
		}

		if (display != null) { // remove old representation
			//remove(display);
			display.setLeftComponent(inputs);
		} else {
			display = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputs, imageWindow[1]);
			display.setResizeWeight(0.5);
			add(display, BorderLayout.CENTER);
		}

		revalidate();
	}

	public void checkEnabled() {
		toolBar.checkEnabled();
	}

	private JComponent setInputsStructure(Structure structure) {

		switch (structure) {
			case horizontal:
				if (inputs == null || !(inputs instanceof JSplitPane)) {
					inputs = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageWindow[0], imageWindow[2]);
					inputs.setBorder(null);
					((JSplitPane) inputs).setResizeWeight(0.5);
				} else {
					((JSplitPane) inputs).setOrientation(JSplitPane.HORIZONTAL_SPLIT);
					((JSplitPane) inputs).setDividerLocation(0.5);
				}

				break;
			case vertical:
				if (inputs == null || !(inputs instanceof JSplitPane)) {
					inputs = new JSplitPane(JSplitPane.VERTICAL_SPLIT, imageWindow[0], imageWindow[2]);
					inputs.setBorder(null);
					((JSplitPane) inputs).setResizeWeight(0.5);
				} else {
					((JSplitPane) inputs).setOrientation(JSplitPane.VERTICAL_SPLIT);
					((JSplitPane) inputs).setDividerLocation(0.5);
				}

				break;
			case tabbed:
				inputs = new JTabbedPane();
				((JTabbedPane) inputs).add(imageWindow[0], "1st input");
				((JTabbedPane) inputs).add(imageWindow[2], "2nd input");
				break;
			case blend:
				inputs = new CBlendPanel(imageWindow[3], (CBlendComponent) imagePanel[3]);
				break;
			case oneInput:
				inputs = imageWindow[0];
				break;
		}

		return inputs;
	}

	public Structure getTwoImageStructure() {
		return twoImageStructure;
	}

	public Structure getStructure() {
		return currStructure;
	}

	public JComponent getInputs() {
		return inputs;
	}

	public CStatusManager getProgressBar() {
		return statusBar;
	}
}
