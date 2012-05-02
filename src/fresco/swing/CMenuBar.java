/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CData;
import fresco.action.IAction.RegID;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

/**
 * Singelton for fresco menu
 * @author gimli
 */
public class CMenuBar extends JMenuBar implements IFrescoComponent {

	private static Toolkit tools = Toolkit.getDefaultToolkit();
	JMenu fileMenu, editMenu, viewMenu, analysisMenu, correctionMenu, transformMenu, segmentationMenu, supportMenu;
	// fileMenu items
	CMenuItem open, close, save, saveAs, imageInfo, exit;
	// viewMenu items
	CCheckBoxMenuItem show2ndInput;
	CRadioMenuItem original, meaned, gray, band1, band2, band3;
	CRadioMenuItem tabbed, verticalSplit, horizontalSplit, alphaBlend;
	// edit menu items
	CMenuItem fixImage, fixSegments, rename, capture;
	// analysis items
	CMenuItem diff, sobel, laplacian, wavelets, mutualInfo, pca, patternAnalysis, intPoints;
	// correction items
	CMenuItem ahe, colorShift;
	// transform items
	CMenuItem rotateLeft, rotateRight, verticalFlip, mirrorFlip, perspectiveTrans;
	// segmentation
	CMenuItem colorQuantization;
	// support
	CMenuItem registrationMarkSelector, regQuality;

	/**
	 * Constructor contains all necessary:
	 * listeners, menu items, ...
	 */
	public CMenuBar() {
		super();

		createFileMenu();
		add(fileMenu);

		createEditMenu();
		add(editMenu);

		createViewMenu();
		add(viewMenu);

		createAnalysisMenu();
		add(analysisMenu);

		createCorrectionMenu();
		add(correctionMenu);

		createTransformMenu();
		add(transformMenu);

		createSegmentationMenu();
		add(segmentationMenu);

		createSupportMenu();
		add(supportMenu);
	}

	private void createFileMenu() {
		fileMenu = new JMenu("File");

		open = new CMenuItem("Open ...", RegID.imageOpen);
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, tools.getMenuShortcutKeyMask()));
		open.addActionListener(CData.userActionListener);
		fileMenu.add(open);

		save = new CMenuItem("Save As ...", RegID.imageSave);
		save.setToolTipText("Saves output image");
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, tools.getMenuShortcutKeyMask()));
		save.addActionListener(CData.userActionListener);
		fileMenu.add(save);

		imageInfo = new CMenuItem("Image Info", RegID.imageInfo);
		imageInfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, tools.getMenuShortcutKeyMask()));
		imageInfo.addActionListener(CData.userActionListener);
		fileMenu.add(imageInfo);
	}

	private void createEditMenu() {
		editMenu = new JMenu("Edit");

		fixImage = new CMenuItem("Fix image", RegID.fixImage);
		fixImage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, tools.getMenuShortcutKeyMask()));
		fixImage.addActionListener(CData.userActionListener);
		editMenu.add(fixImage);

		fixSegments = new CMenuItem("Fix segments", RegID.fixSegments);
		fixSegments.addActionListener(CData.userActionListener);
		editMenu.add(fixSegments);

		rename = new CMenuItem("Rename image ...", RegID.rename);
		rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, tools.getMenuShortcutKeyMask()));
		rename.addActionListener(CData.userActionListener);
		editMenu.add(rename);

		capture = new CMenuItem("Capture image", RegID.capture);
		capture.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, tools.getMenuShortcutKeyMask()));
		capture.addActionListener(CData.userActionListener);
		editMenu.add(capture);
	}

	private void createViewMenu() {
		viewMenu = new JMenu("View");

		show2ndInput = new CCheckBoxMenuItem("Show 2nd input", false, RegID.show2ndInput);
		show2ndInput.addActionListener(CData.userActionListener);
		viewMenu.add(show2ndInput);

		ButtonGroup viewGroup = new ButtonGroup(), orientation = new ButtonGroup();
		original = new CRadioMenuItem("Original color", false, RegID.originalView);
		original.addActionListener(CData.userActionListener);
		meaned = new CRadioMenuItem("Segment mean color", true, RegID.meanedView);
		meaned.addActionListener(CData.userActionListener);
		gray = new CRadioMenuItem("Gray scale color", false, RegID.grayView);
		gray.addActionListener(CData.userActionListener);
		band1 = new CRadioMenuItem("Red channel", false, RegID.band1View);
		band1.addActionListener(CData.userActionListener);
		band2 = new CRadioMenuItem("Green channel", false, RegID.band2View);
		band2.addActionListener(CData.userActionListener);
		band3 = new CRadioMenuItem("Blue channel", false, RegID.band3View);
		band3.addActionListener(CData.userActionListener);

		viewGroup.add(original);
		viewGroup.add(meaned);
		viewGroup.add(gray);
		viewGroup.add(band1);
		viewGroup.add(band2);
		viewGroup.add(band3);

		horizontalSplit = new CRadioMenuItem("Horizontal split", true, RegID.horizontalSplit);
		horizontalSplit.addActionListener(CData.userActionListener);
		horizontalSplit.setEnabled(false);
		verticalSplit = new CRadioMenuItem("Vertical split", false, RegID.verticalSplit);
		verticalSplit.addActionListener(CData.userActionListener);
		verticalSplit.setEnabled(false);
		tabbed = new CRadioMenuItem("Tabbed", false, RegID.tabbed);
		tabbed.addActionListener(CData.userActionListener);
		tabbed.setEnabled(false);
		alphaBlend = new CRadioMenuItem("Blending", false, RegID.alphaBlend);
		alphaBlend.addActionListener(CData.userActionListener);
		alphaBlend.setEnabled(false);

		orientation.add(horizontalSplit);
		orientation.add(verticalSplit);
		orientation.add(tabbed);
		orientation.add(alphaBlend);

		viewMenu.add(horizontalSplit);
		viewMenu.add(verticalSplit);
		viewMenu.add(alphaBlend);
		viewMenu.add(tabbed);

		viewMenu.add(new JSeparator());

		viewMenu.add(original);
		viewMenu.add(meaned);
		viewMenu.add(gray);
		viewMenu.add(band1);
		viewMenu.add(band2);
		viewMenu.add(band3);
	}

	private void createAnalysisMenu() {
		analysisMenu = new JMenu("Analysis");

		diff = new CMenuItem("Images differences", RegID.diff);
		diff.addActionListener(CData.userActionListener);
		analysisMenu.add(diff);

		sobel = new CMenuItem("Sobel's edge detector", RegID.sobel);
		sobel.addActionListener(CData.userActionListener);
		analysisMenu.add(sobel);

		laplacian = new CMenuItem("Laplacian of gaussian", RegID.laplace);
		laplacian.addActionListener(CData.userActionListener);
		analysisMenu.add(laplacian);

		wavelets = new CMenuItem("Wavelet deomposition", RegID.wavelets);
		wavelets.addActionListener(CData.userActionListener);
		analysisMenu.add(wavelets);

		mutualInfo = new CMenuItem("Mutual information graph", RegID.mutualInfo);
		mutualInfo.addActionListener(CData.userActionListener);
		analysisMenu.add(mutualInfo);

		pca = new CMenuItem("Principal component analysis", RegID.pca);
		pca.addActionListener(CData.userActionListener);
		analysisMenu.add(pca);

		patternAnalysis = new CMenuItem("Pattern analysis", RegID.patternAnalyzer);
		patternAnalysis.addActionListener(CData.userActionListener);
		analysisMenu.add(patternAnalysis);
    
    intPoints = new CMenuItem("Find Interesting points", RegID.intPoints);
		intPoints.addActionListener(CData.userActionListener);
		analysisMenu.add(intPoints);
	}

	public void checkEnabled() {

		if (CData.showImage[0] == -1) {
			save.setEnabled(false);
			imageInfo.setEnabled(false);
			rename.setEnabled(false);
			sobel.setEnabled(false);
			laplacian.setEnabled(false);
			wavelets.setEnabled(false);
			pca.setEnabled(false);
			rotateLeft.setEnabled(false);
			rotateRight.setEnabled(false);
			segmentationMenu.setEnabled(false);
		} else {
			save.setEnabled(true);
			imageInfo.setEnabled(true);
			rename.setEnabled(true);
			sobel.setEnabled(true);
			laplacian.setEnabled(true);
			wavelets.setEnabled(false);
			pca.setEnabled(false);
			rotateLeft.setEnabled(true);
			rotateRight.setEnabled(true);
			segmentationMenu.setEnabled(true);
		}
    if (CData.showImage[0] != -1) { 
      intPoints.setEnabled(true);
    }
    else {
      intPoints.setEnabled(false);
    }

		if (CData.output == null) {
			fixImage.setEnabled(false);
			fixSegments.setEnabled(false);
			capture.setEnabled(false);
		} else {
			if (!CData.output.isSegmented()) {
				fixSegments.setEnabled(false);
			} else {
				fixSegments.setEnabled(true);
			}
			capture.setEnabled(true);
			fixImage.setEnabled(true);
		}

		if (CData.showImage[0] != -1 && CData.showImage[2] != -1) {
			diff.setEnabled(true);
			mutualInfo.setEnabled(true);
			patternAnalysis.setEnabled(true);
			registrationMarkSelector.setEnabled(true);
			if (CData.getImage(CData.showImage[0]).getNumOfMarks() == CData.getImage(CData.showImage[2]).getNumOfMarks())
				regQuality.setEnabled(true);
			else
				regQuality.setEnabled(false);
		} else {
			diff.setEnabled(false);
			mutualInfo.setEnabled(false);
			patternAnalysis.setEnabled(false);
			registrationMarkSelector.setEnabled(false);
			regQuality.setEnabled(false);
		}

		if (show2ndInput.isSelected()) {
			horizontalSplit.setEnabled(true);
			verticalSplit.setEnabled(true);
			tabbed.setEnabled(true);
			alphaBlend.setEnabled(true);
		}

		if (CData.showImage[0] != -1
				&& CData.showImage[2] != -1
				&& CData.getImage(CData.showImage[0]).getNumOfMarks() >= 4
				&& CData.getImage(CData.showImage[2]).getNumOfMarks() >= 4) {
			perspectiveTrans.setEnabled(true);
		} else {
			perspectiveTrans.setEnabled(false);
		}
	}

	private void createCorrectionMenu() {
		correctionMenu = new JMenu("Correction");
		ahe = new CMenuItem("Contrast enhancement", RegID.ahe);
		ahe.addActionListener(CData.userActionListener);
		colorShift = new CMenuItem("Color shift", RegID.colorShift);
		colorShift.addActionListener(CData.userActionListener);
		correctionMenu.add(ahe);
		correctionMenu.add(colorShift);
	}

	private void createTransformMenu() {
		transformMenu = new JMenu("Transform");
		rotateLeft = new CMenuItem("Rotate 90\u02DA left", RegID.rotateLeft);
		rotateLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, tools.getMenuShortcutKeyMask()));
		rotateLeft.addActionListener(CData.userActionListener);
		transformMenu.add(rotateLeft);

		rotateRight = new CMenuItem("Rotate 90\u02DA right", RegID.rotateRight);
		rotateRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, tools.getMenuShortcutKeyMask()));
		rotateRight.addActionListener(CData.userActionListener);
		transformMenu.add(rotateRight);

		perspectiveTrans = new CMenuItem("Perspective transformation", RegID.register);
		perspectiveTrans.addActionListener(CData.userActionListener);
		transformMenu.add(perspectiveTrans);
	}

	private void createSegmentationMenu() {
		segmentationMenu = new JMenu("Segmentation");

		colorQuantization = new CMenuItem("Color quantization ...", RegID.colorQuantization);
		colorQuantization.addActionListener(CData.userActionListener);
		segmentationMenu.add(colorQuantization);
	}

	private void createSupportMenu() {
		supportMenu = new JMenu("Support");

		registrationMarkSelector = new CMenuItem("Corresponding points selection", RegID.registrationMarkSearch);
		registrationMarkSelector.addActionListener(CData.userActionListener);
		supportMenu.add(registrationMarkSelector);

		regQuality = new CMenuItem("Registration marks overview", RegID.registrationMarksQuality);
		regQuality.addActionListener(CData.userActionListener);
		supportMenu.add(regQuality);
	}
}
