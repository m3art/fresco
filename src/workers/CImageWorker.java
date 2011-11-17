/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers;

import fresco.CData;
import fresco.action.IAction.RegID;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
import workers.analyse.CCannyEdgeDetector;
import workers.analyse.CDiff;
import workers.analyse.CLaplacian;
import workers.analyse.CMutualInformationGraph;
import workers.analyse.CPatternAnalyzer;
import workers.correction.CAdaptiveHistogramEnhancing;
import workers.correction.CColorShiftWorker;
import workers.registration.CPerspectiveTransformationWorker;
import workers.registration.refpointga.CRefPointMarker;
import workers.segmentation.CColorQuantizer;
import workers.tools.CRotation;
import workers.tools.CRotation.Rotation;

public abstract class CImageWorker<T, V> extends SwingWorker<T, V> implements IImageWorker {

	private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

	/**
	 *
	 * Creates a note about ImageWorker into macro creator
	 * it must contain:
	 * type of Worker - ANALYSIS, SEGMENTATION, REGISTRATION, CORRECTOR,
	 *                  MORPHOLOGY
	 * name of Worker - PCAWorker, WaveletTransform, WaveletWorker
	 *                - AdaptiveHistogramEnhancing, ContrastEnhancment, SmoothWorker
	 *                - SegmentClose
	 *                - ByUserMarks, HaarWavelet
	 *                - CannyEdgeDetector, ColorQuantizer, OhlanderPriceReddy
	 * input image    - numbers/path/
	 * other params   - param_name="param_value"
	 *
	 * examples:
	 * ANALYSIS: PCAWorker - input="input name"
	 * MORPHOLOGY: SegmentClose - input="segmented image"
	 * CORRECTOR: SmoothWorker - input="input name"
	 * SEGMENTATION: ColorQuantizer - input="input name", colors="int colors"
	 *
	 * @return
	 */
	/**
	 * Creates selected ImageWorker
	 * @param id identifier of type of worker
	 * @return created imageWorker
	 */
	public static CImageWorker createWorker(RegID id, Object[] params) {
		switch (id) {
			case laplace:
				return new CLaplacian(CData.getImage(CData.showImage[0]).getImage(), (Integer) (params[0]));
			case sobel:
				return new CCannyEdgeDetector(CData.getImage(CData.showImage[0]).getImage());
			case rotateRight:
				return new CRotation(CData.getImage(CData.showImage[0]).getImage(), Rotation.right);
			case rotateLeft:
				return new CRotation(CData.getImage(CData.showImage[0]).getImage(), Rotation.left);
			case diff:
				return new CDiff(CData.getImage(CData.showImage[0]).getImage(),
						CData.getImage(CData.showImage[2]).getImage());
			case mutualInfo:
				return new CMutualInformationGraph(CData.getImage(CData.showImage[0]).getImage(),
						CData.getImage(CData.showImage[2]).getImage(), (Integer) (params[0]), (Integer) (params[1]));
			case colorQuantization:
				BufferedImage image = CData.getImage(CData.showImage[0]).getImage();
				return new CColorQuantizer(CData.getImage(CData.showImage[0]).getImage(), image.getWidth() * image.getHeight() / 1000, 3, (Integer) params[0]);
			case register:
				return new CPerspectiveTransformationWorker(CData.getImage(CData.showImage[0]), CData.getImage(CData.showImage[2]));
			case patternAnalyzer:
				return new CPatternAnalyzer(CData.getImage(CData.showImage[0]), CData.getImage(CData.showImage[2]));
			case ahe:
				return new CAdaptiveHistogramEnhancing(CData.getImage(CData.showImage[0]).getImage());
			case colorShift:
				return new CColorShiftWorker(CData.getImage(CData.showImage[0]));
			case registrationMarkSearch:
				// FIXME: population size can be set by user
				return new CRefPointMarker(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), 150);
			default:
				return null;
		}
	}

	public abstract String getTypeName();

	public abstract String getWorkerName();

	public JDialog getParamSettingDialog() {
		logger.info("No params are necessary.");

//		JDialog dialog = new JDialog(CData.mainFrame, getWorkerName(), ModalityType.APPLICATION_MODAL);
//
//		dialog.add(new JButton(null))
//
//		return dialog;
		return null;
	}
}
