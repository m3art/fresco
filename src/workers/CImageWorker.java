/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers;

import fresco.CData;
import fresco.action.IAction.RegID;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.SwingWorker;
import support.regmarks.CPointPairsOverview;
import workers.analyse.*;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.analyse.paramObjects.CLoGParams;
import workers.correction.CAdaptiveHistogramEnhancing;
import workers.correction.CColorShiftWorker;
import workers.registration.*;
import workers.registration.CInterestingPoints.Cornerer;
import workers.registration.CInterestingPoints.Edger;
import workers.registration.refpointga.CRefPointMarker;
import workers.segmentation.CColorQuantizer;
import workers.tools.CRotation;
import workers.tools.CRotation.Rotation;

public abstract class CImageWorker<T, V> extends SwingWorker<T, V> implements IImageWorker {

  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  /**
   *
   * Creates a note about ImageWorker into macro creator it must contain: type
   * of Worker - ANALYSIS, SEGMENTATION, REGISTRATION, CORRECTOR, MORPHOLOGY
   * name of Worker - PCAWorker, WaveletTransform, WaveletWorker -
   * AdaptiveHistogramEnhancing, ContrastEnhancment, SmoothWorker - SegmentClose
   * - ByUserMarks, HaarWavelet - CannyEdgeDetector, ColorQuantizer,
   * OhlanderPriceReddy input image - numbers/path/ other params -
   * param_name="param_value"
   *
   * examples: ANALYSIS: PCAWorker - input="input name" MORPHOLOGY: SegmentClose
   * - input="segmented image" CORRECTOR: SmoothWorker - input="input name"
   * SEGMENTATION: ColorQuantizer - input="input name", colors="int colors"
   *
   * @return
   */
  /**
   * Creates selected ImageWorker
   *
   * @param id identifier of type of worker
   * @return created imageWorker
   */
  public static CImageWorker createWorker(RegID id, Object[] params) {

    switch (id) {
      case laplace:
        return new CLaplacian(CData.getImage(CData.showImage[0]).getImage());
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
        return new CVariationOfInformationGraph(CData.getImage(CData.showImage[0]).getImage(),
                CData.getImage(CData.showImage[2]).getImage());
      case colorQuantization:
        return new CColorQuantizer(CData.getImage(CData.showImage[0]).getImage());
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
      case registrationMarksQuality:
        CPointPairs pairs = new CPointPairs(CData.getImage(CData.showImage[0]).getMarks(), CData.getImage(CData.showImage[2]).getMarks());
        return new CPointPairsOverview(pairs, CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage());
      case intPoints:
        CCOGParams pic = new CCOGParams(CCOGParams.values.def);
        CLoGParams pe = new CLoGParams();
        return new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), Cornerer.COG, Edger.LOG, pic, pe, 0, 0);
      case harris:

        CHarrisParams ph = new CHarrisParams();
        return new CHarris(CData.getImage(CData.showImage[0]).getImage(), ph);
      case COG:
        CCOGParams pc = new CCOGParams(CCOGParams.values.def);
        return new CCornerDetectorCOG(CData.getImage(CData.showImage[0]).getImage(), pc);
      //case ransac:
        //return new CRansacRegister(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage());
      //case MSERCorrelator:
        //return new CMSERCorrelator(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage());
      case pointPairSelector:
        return new CPointPairSelector(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), 30);
      case pointPairDisplay:
        return new CPointPairMatchDisplay(CData.pairs, CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage());
      default:

        return null;
    }
  }

  public abstract String getTypeName();

  public abstract String getWorkerName();

  public boolean hasDialog() {
    return false;
  }

  /**
   * Evaluate values set by user. Default Worker does not support user input. In
   * this case is call of this method illegal and
   *
   * @throws UnsupportedOperationException is thrown.
   */
  public boolean confirmDialog() {
    throw new UnsupportedOperationException("This worker does not implement user input yet.");
  }

  /**
   * Default user interface to any user. No dialog is necessary (nothing is
   * shown). If you want user input in your worker rewrite this method.
   *
   * @return null
   */
  public JDialog getParamSettingDialog() {
    logger.info("No params are necessary.");

    return null;
  }
}
