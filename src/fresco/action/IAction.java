/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.action;

/**
 *
 * @author gimli
 */
public interface IAction {

	public static enum RegID {

		imageOpen,
		imageSave,
		imageInfo,
		show2ndInput,
		load2ndInput,
		originalView,
		meanedView,
		grayView,
		band1View,
		band2View,
		band3View,
		fixImage,
		fixSegments,
		rename,
		capture,
		close,
		open,
		saveAs,
		zoom,
		kill,
		// look of left image preview
		horizontalSplit,
		verticalSplit,
		tabbed,
		alphaBlend,
		oneInput,
		// analysis
		sobel,
		laplace,
		wavelets,
		pca,
		diff,
		mutualInfo,
		patternAnalyzer,
    intPoints,
    harris,
    COG,
    ransac,
    MSERCorrelator,
		// correction
		ahe,
		colorShift,
		// transformation
		rotateLeft,
		rotateRight,
		verticalFlip,
		mirrorFlip,
		register,
		// segmentation
		colorQuantization,
		//tools
		registerMarks,
		// support
		registrationMarkSearch,
		registrationMarksQuality,
    runMultipleIntPoints
	};

	public RegID getID();
}
