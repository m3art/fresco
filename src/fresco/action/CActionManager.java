/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.action;

import file.CImageFile;
import fresco.CData;
import fresco.CImageContainer;
import fresco.CImageContainer.Screen;
import fresco.action.IAction.RegID;
import fresco.swing.CContentPane;
import fresco.swing.CContentPane.Structure;
import fresco.swing.CDrawPanel;
import fresco.swing.CInfoFrame;
import fresco.swing.CPreviewBar;
import image.converters.Crgb2grey;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import support.regmarks.CPointPairsOverview;
import workers.CImageWorker;
import workers.analyse.CBestWindowSize;
import workers.analyse.CInterestingPointsThread;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.analyse.paramObjects.CLoGParams;
import workers.registration.CPointPairs;
import workers.registration.CPointsAndQualities;
import workers.registration.refpointga.CRefPointMarker;
import workers.registration.CInterestingPoints;
import workers.registration.CInterestingPoints.Cornerer;
import workers.registration.CInterestingPoints.Edger;
import workers.registration.CPointPairSelector;
import workers.segmentation.CSegmentMap;

/**
 * Holds all user actions in Fresco application
 *
 * @author gimli
 */
public class CActionManager {

  CImageWorker imageWorker;
  CStatusManager progressBar;
  CDrawPanel[] imagePanel;
  private CContentPane content;
  private CPreviewBar previewBar;
  private static final Logger logger = Logger.getLogger(CActionManager.class.getName());
  private MIPUtils m;

  /**
   * Constructor have to handle a lot of gui panels
   *
   * @param imagePanel three panels for image painting
   * @param content main window with images
   * @param previewBar shortcut bar for image processing
   * @param progressBar handler of progress bar for showing image worker
   * progress
   */
  public CActionManager(CDrawPanel[] imagePanel, CContentPane content, CPreviewBar previewBar, CStatusManager progressBar) {
    this.imagePanel = imagePanel;
    this.previewBar = previewBar;
    this.progressBar = progressBar;
    this.content = content;
    this.m = new MIPUtils();
  }

  /**
   * Tool for image loading from files
   */
  public void loadImage() {
    CImageFile.openPicture();
  }

  public void fireImageListChanged() {
    logger.info("Set of pictures changed ... update");

    if (CData.showImage[0] == -1 && CData.imagesSize() != 0) {
      CData.showImage[0] = 0;
      loadContainer(0, 0);
    }
    previewBar.refreshBar();
    refreshGUI();
  }

  /**
   * Every user action calls refreshGUI, images are repainted, sizes are set
   * once more etc.
   */
  private static void refreshGUI() {
    CData.mainFrame.checkEnabled();
    CData.mainFrame.repaint();
    ((CContentPane) CData.mainFrame.getContentPane()).getInputs().repaint();
  }

  /**
   * Warning before image closing is generated there
   *
   * @param id image specification
   */
  public void closePicture(int id) {
    if (CData.getImage(id).isChanged()) { // image is not saved
      int userChoose = JOptionPane.showOptionDialog(new JFrame("Image closing"),
              "Warning: Image is not saved. Do you want to save it before closing it?",
              "Image closing", JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, null, null);

      if (userChoose == JOptionPane.CANCEL_OPTION || userChoose == JOptionPane.CLOSED_OPTION) {
        return;
      } else if (userChoose == JOptionPane.OK_OPTION) {
        savePicture(id);
      }
    }

    CData.removeImage(id);
    for (int i = 0; i < CData.showImage.length; i++) {
      if (CData.showImage[i] > id) {
        CData.showImage[i]--;
      } else if (CData.showImage[i] == id) {
        CData.showImage[i] = -1;
      }
    }
    previewBar.refreshBar();
    refreshGUI();
  }

  public void renamePicture(int id) {
    if (id < 0 || id > CData.imagesSize()) {
      return;
    }

    String filename = (String) JOptionPane.showInputDialog(
            new JFrame("Rename ..."),
            "Enter new name: ", "Rename ...",
            JOptionPane.PLAIN_MESSAGE, null, null,
            CData.getImage(id).getFilename());

    if (filename != null && filename.length() > 0) {
      CData.getImage(id).setFilename(filename);
      previewBar.refreshBar();
    }
  }

  public void saveAsPicture(int id) {
    CImageFile.saveAsPicture(CData.getImage(id));
  }

  public void savePicture(int id) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void killProcess() {
    imageWorker.cancel(true);
    imageWorker.removePropertyChangeListener(progressBar);
    imageWorker = null;
  }

  /**
   * User can choose from different types of view
   *
   * @param id defines type of view (CData.View)
   */
  public void switchView(Screen id) {
    CData.view = id;
    refreshGUI();
  }

  /**
   * This action is manager of CData.input2showed field
   */
  public void show2ndInput() {
    CData.input2Showed = !CData.input2Showed;

    if (CData.input2Showed) {
      content.setStructure(content.getTwoImageStructure());
    } else {
      content.setStructure(Structure.oneInput);
    }

    CData.mainFrame.getJMenuBar().getMenu(2).getItem(0).setSelected(CData.input2Showed);
  }

  /**
   * Action manages user defined zoom value
   *
   * @param percentage specify size of showed image in percents of real size
   */
  public void setZoom(int percentage) {
    CData.setFocus(percentage);
    for (int i = 0; i < imagePanel.length; i++) {
      if (i < 3 && CData.showImage[i] != -1) {
        imagePanel[i].setSizeByImage();
        logger.log(Level.FINE, "Image size values: {0}, {1}", new Object[]{imagePanel[i].getWidth(), imagePanel[i].getHeight()});
      } else if (i > 2 && ((CContentPane) CData.mainFrame.getContentPane()).getStructure() == CContentPane.Structure.blend) {
        imagePanel[i].setSizeByImage();
      }
    }
  }

  /**
   * Put specified container into specified image panel
   *
   * @param imagePanelID definition of image panel [0,1,2]
   * @param containerID definition of container
   */
  public void loadContainer(int imagePanelID, int containerID) {
    if (imagePanelID == 2 && !CData.input2Showed) {
      show2ndInput();
    }

    if (CData.imagesSize() > containerID) {
      CData.showImage[imagePanelID] = containerID;
    }

    imagePanel[imagePanelID].setSizeByImage();
    refreshGUI();
  }

  private void loadOutput() {
    if (CData.output == null) {
      return;
    }
    if (CData.showImage[1] != -2) {
      CData.showImage[1] = -2;
    }
    imagePanel[1].setSizeByImage();
  }

  /**
   * Simplified multi-action which manage image container
   *
   * @param action defines type of user action
   * @param containerID defines which container will be managed
   */
  public void containerDo(RegID action, int containerID) {
    switch (action) {
      case close:
        closePicture(containerID);
        break;
      case rename:
        renamePicture(containerID);
        break;
      case open:
        loadContainer(0, containerID);
        break;
      case saveAs:
        saveAsPicture(containerID);
        break;
      case load2ndInput:
        loadContainer(2, containerID);
        break;
    }
  }

  public void showInfo() {
    JFrame infoFrame = new CInfoFrame(CData.getImage(CData.showImage[0]));
    infoFrame.setVisible(true);
  }

  public void runImageWorker(RegID id, Object[] params) {

    imageWorker = CImageWorker.createWorker(id, params);

    JDialog dialog = imageWorker.getParamSettingDialog();
    if (dialog != null) {
      dialog.setVisible(true);
    }
    imageWorker.addPropertyChangeListener(progressBar);
    imageWorker.execute();
  }

  public void cleanImageWorker() {
    if (imageWorker == null) {
      return;
    }
    imageWorker.removePropertyChangeListener(progressBar);
    try {
      logger.log(Level.INFO, "{0}, {1}", new Object[]{imageWorker.getTypeName(), imageWorker.getWorkerName()});
      switch (imageWorker.getType()) {
        case ANALYSIS:
          CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
          break;
        case SEGMENTATION:
          CData.output = new CImageContainer(CData.getImage(CData.showImage[0]).getImage(), false);
          CData.output.setSegmentMap((CSegmentMap) imageWorker.get());
          break;
        case REGISTRATION:
          if (imageWorker instanceof CPointPairSelector) {
            CPointPairs pairs = (CPointPairs) imageWorker.get();
            CData.getImage(CData.showImage[0]).setMarks(pairs.getOrigins());
            CData.getImage(CData.showImage[2]).setMarks(pairs.getProjected());
            return;
          }
          CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
          break;
        case CORRECTOR:
          CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
          break;
        case MORPH:
          CData.output = new CImageContainer(CData.getImage(CData.showImage[0]).getImage(), false);
          CData.output.setSegmentMap((CSegmentMap) imageWorker.get());
          break;
        case SUPPORT:
          if (imageWorker instanceof CRefPointMarker) {
            logger.info("Marks distribution here");
            CPointPairs pairs = (CPointPairs) imageWorker.get();
            CData.getImage(CData.showImage[0]).setMarks(pairs.getOrigins());
            CData.getImage(CData.showImage[2]).setMarks(pairs.getProjected());
            return;
          } else if (imageWorker instanceof CPointPairsOverview) {
            ((CPointPairsOverview) imageWorker).get().setVisible(true);
            return;
          } else if (imageWorker instanceof CInterestingPoints) {
            CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
            return;
          } else {
            CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
          }
          break;
      }
      CData.output.setFilename(CData.getImage(CData.showImage[0]).getFilename());
      CData.showImage[1] = -2;
      loadOutput();
      refreshGUI();
    } catch (NullPointerException npe) {
      logger.log(Level.INFO, "NullPointerException: Bad input params for worker or worker bug.{0}", npe.getMessage());
    } catch (CancellationException ce) {
      logger.info("Operation cancelled");
    } catch (InterruptedException ie) {
      logger.info("Operation interrupted");
    } catch (ExecutionException ee) {
      logger.log(Level.INFO, "Other exception: {0}", ee.getMessage());
    }
    imageWorker = null;
  }

  public void fixImage() {
    if (CData.showImage[0] != -1 && CData.output != null) {
      CData.getImage(CData.showImage[0]).setImage(CData.output.getTransformedImage(CData.view));
    } else if (CData.output != null) {
      captureImage();
    } else {
      JOptionPane.showMessageDialog(new JFrame(), "There is no output image to fix.", "Warning", JOptionPane.WARNING_MESSAGE);
    }

    previewBar.refreshBar();
    refreshGUI();
  }

  public void captureImage() {
    if (CData.output != null) {
      CData.addImage(CData.output);
    }
    previewBar.refreshBar();
    refreshGUI();
  }

  public void setContentStructure(RegID regID) {
    content.setStructure(regID);
    refreshGUI();
  }

  private class MIPUtils {

    double learnrate = 2.0;
    double learnrategSg = 10 * learnrate;
    double learnrategSe = 10 * learnrate;
    int maxIters = 120;
    double derStep = 1.1;
    int setSize = 0;
    int paramCount = 8;
    int[] windowSizes;
    double lambda = 0.01;
    double convergenceConstant = 0.0003;
    int convergenceGrowthCounter = 0;
    int convergenceMaxCounter = 0;
    int convergenceMaximumIters = 7;
    double maxScore = 0.0;
    double lastScore = 0;
    BufferedImage imageA = null;
    BufferedImage imageAGreyscale = null;
    double[] growth;
    CCOGParams[] pc;
    CLoGParams[] pe;
    CInterestingPointsThread[] threadFieldCOG;
    CInterestingPoints[] workerFieldCOG;
    double[] scoreFieldCOG;

    public void MIPUtils() {
      this.initConstants();
      this.initArrays();
    }

    public void initConstants() {

      imageA = null;
      imageAGreyscale = null;

      learnrate = 2.0;
      learnrategSg = 10 * learnrate;
      learnrategSe = 10 * learnrate;
      maxIters = 120;
      derStep = 1.1;
      setSize = CData.imagesSize() / 2;
      paramCount = 8;
      windowSizes = new int[setSize];
      lambda = 0.01;
      convergenceConstant = 0.0003;
      convergenceGrowthCounter = 0;
      convergenceMaxCounter = 0;
      convergenceMaximumIters = 7;
      maxScore = 0;
      double lastScore = 0;
    }

    public void initArrays() {
      growth = new double[paramCount];
      pc = new CCOGParams[paramCount + 1];
      pe = new CLoGParams[paramCount + 1];
      threadFieldCOG = new CInterestingPointsThread[paramCount + 1];
      workerFieldCOG = new CInterestingPoints[paramCount + 1];
      scoreFieldCOG = new double[paramCount + 1];
      for (int j = 0; j < paramCount + 1; j++) {
        pc[j] = new CCOGParams(CCOGParams.values.def);
        pe[j] = new CLoGParams();
      }

    }

    public void calculateWindowSizes() {
      for (int i = 0; i < CData.imagesSize(); i += 2) {
        if (CData.imagesSize() == i + 1) {
          break;
        }
        if (!CData.input2Showed) {
          show2ndInput();
        }
        CData.showImage[0] = i;
        imagePanel[0].setSizeByImage();
        CData.showImage[2] = i + 1;
        imagePanel[2].setSizeByImage();
        imageA = CData.getImage(CData.showImage[0]).getImage();
        imageAGreyscale = (new Crgb2grey()).convert(imageA);
        windowSizes[i / 2] = CBestWindowSize.getBestWindowSize(imageAGreyscale.getData(), 10000, 3, 25);
        logger.info("windowsize: " + windowSizes[i / 2]);
      }
      imageA = null;
      imageAGreyscale = null;
    }

    public void initParams() {
      pc[1].centerWhitenessW *= derStep;
      pc[1].normalizeWeights();
      pc[2].distW *= derStep;
      pc[2].normalizeWeights();
      pc[3].perpCW *= derStep;
      pc[3].normalizeWeights();
      pc[4].whiteDiffW *= derStep;
      pc[4].normalizeWeights();
      pc[5].thresholdq *= derStep;
      pc[5].normalizeWeights();
      pc[6].mixW *= derStep;
      pc[6].normalizeWeights();
      pe[7].gaussSigma *= derStep;
      pe[8].gaussSizeQ *= derStep;
      pe[8].calculateSizeFromQ();
    }

    public void refillParams() {
      for (int j = 0; j < paramCount; j++) {
        growth[j] = (scoreFieldCOG[j + 1] - scoreFieldCOG[0]);
        if (growth[j] > 0) {
          growth[j] -= lambda * pc[j].getRegularization();
        } else {
          growth[j] += lambda * pc[j].getRegularization();
        }
      }

      pc[0].centerWhitenessW = pc[0].centerWhitenessW + growth[0] * learnrate;
      pc[0].distW = pc[0].distW + growth[1] * learnrate;
      pc[0].perpCW = pc[0].perpCW + growth[2] * learnrate;
      pc[0].whiteDiffW = pc[0].whiteDiffW + growth[3] * learnrate;
      pc[0].thresholdq = pc[0].thresholdq + growth[4] * learnrate;
      pc[0].mixW = pc[0].mixW + growth[5] * learnrate;
      pc[0].normalizeWeights();
      pe[0].gaussSigma = pe[0].gaussSigma + growth[6] * learnrategSg;
      pe[0].gaussSizeQ = pe[0].gaussSizeQ + growth[7] * learnrategSe;
      pe[0].calculateSizeFromQ();
      for (int j = 1; j < paramCount + 1; j++) {
        pc[j].centerWhitenessW = pc[0].centerWhitenessW;
        pc[j].distW = pc[0].distW;
        pc[j].perpCW = pc[0].perpCW;
        pc[j].whiteDiffW = pc[0].whiteDiffW;
        pc[j].thresholdq = pc[0].thresholdq;
        pc[j].mixW = pc[0].mixW;
        pc[j].normalizeWeights();
        pe[j].gaussSigma = pe[0].gaussSigma;
        pe[j].gaussSizeQ = pe[0].gaussSizeQ;
        pe[j].calculateSizeFromQ();
      }
      for (int j = 0; j < paramCount + 1; j++) {
        workerFieldCOG[j] = null;
        threadFieldCOG[j] = null;
        scoreFieldCOG[j] = 0.0;
      }
    }

    public void runIntPtsOnAllImgs() {
      for (int i = 0; i < CData.imagesSize(); i += 2) {
        if (CData.imagesSize() == i + 1) {
          break;
        }
        if (!CData.input2Showed) {
          show2ndInput();
        }
        CData.showImage[0] = i;
        imagePanel[0].setSizeByImage();
        CData.showImage[2] = i + 1;
        imagePanel[2].setSizeByImage();
        for (int j = 0; j < paramCount + 1; j++) {
          pc[j].windowSize = windowSizes[i / 2];
        }
        for (int j = 0; j < paramCount + 1; j++) {
          //logger.info("i. e. CW: " + pc[j].centerWhitenessW + " dist: " + pc[j].distW + " perpC: " + pc[j].perpCW + "whiteDiff: " + pc[j].whiteDiffW);
          workerFieldCOG[j] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), Cornerer.COG, Edger.sobel, pc[j], pe[j], j, i);
          threadFieldCOG[j] = new CInterestingPointsThread(workerFieldCOG[j], CData.getImage(CData.showImage[2]).getImage());
          threadFieldCOG[j].start();
        }
        for (int j = 0; j < paramCount + 1; j++) {
          try {
            threadFieldCOG[j].join();
          } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread " + j + " interrupted");
          }
          scoreFieldCOG[j] += (threadFieldCOG[j].result) / (double) setSize;
        } //join cycle
      }//image cycle

      System.out.println(
              "base: CW: " + pc[0].centerWhitenessW
              + " d: " + pc[0].distW
              + " pC: " + pc[0].perpCW
              + " wD: " + pc[0].whiteDiffW
              + " thr: " + pc[0].threshold
              + " mix: " + pc[0].mixW
              + " gSg: " + pe[0].gaussSigma
              + " gSe: " + pe[0].gaussSize);
      System.out.println("COG base score: " + scoreFieldCOG[0]);
    }

    public void runIntPtsOnList(int[] imgIDs) {
      for (int i = 0; i < imgIDs.length; i++) {
        if (CData.imagesSize() == i + 1) {
          break;
        }
        if (!CData.input2Showed) {
          show2ndInput();
        }
        CData.showImage[0] = 2 * imgIDs[i];
        System.out.println("Now working on image no. " + 2 * imgIDs[i]);
        imagePanel[0].setSizeByImage();
        CData.showImage[2] = 2 * imgIDs[i] + 1;
        imagePanel[2].setSizeByImage();
        for (int j = 0; j < paramCount + 1; j++) {
          pc[j].windowSize = windowSizes[imgIDs[i]];
        }
        for (int j = 0; j < paramCount + 1; j++) {
          //logger.info("i. e. CW: " + pc[j].centerWhitenessW + " dist: " + pc[j].distW + " perpC: " + pc[j].perpCW + "whiteDiff: " + pc[j].whiteDiffW);
          workerFieldCOG[j] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), Cornerer.COG, Edger.sobel, pc[j], pe[j], j, i);
          threadFieldCOG[j] = new CInterestingPointsThread(workerFieldCOG[j], CData.getImage(CData.showImage[2]).getImage());
          threadFieldCOG[j].start();
        }
        for (int j = 0; j < paramCount + 1; j++) {
          try {
            threadFieldCOG[j].join();
          } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread " + j + " interrupted");
          }
          scoreFieldCOG[j] += (threadFieldCOG[j].result) / (double) imgIDs.length;
        } //join cycle
      }//image cycle

      System.out.println(
              "base: CW: " + pc[0].centerWhitenessW
              + " d: " + pc[0].distW
              + " pC: " + pc[0].perpCW
              + " wD: " + pc[0].whiteDiffW
              + " thr: " + pc[0].threshold
              + " mix: " + pc[0].mixW
              + " gSg: " + pe[0].gaussSigma
              + " gSe: " + pe[0].gaussSize);
      System.out.println("COG base score: " + scoreFieldCOG[0]);
    }

    public void updateConvergence() {
      if ((scoreFieldCOG[0] - lastScore) < convergenceConstant) {
        convergenceGrowthCounter++;
        //logger.info("convergence growth counter now at " + convergenceGrowthCounter);
      } else {
        convergenceGrowthCounter = 0;
        //logger.info("convergence growth counter now at " + convergenceGrowthCounter);
      }
      if (scoreFieldCOG[0] < maxScore) {
        convergenceMaxCounter++;
        //logger.info("convergence max counter now at " + convergenceMaxCounter);
      } else {
        maxScore = scoreFieldCOG[0];
        convergenceMaxCounter = 0;
        //logger.info("convergence max counter now at " + convergenceMaxCounter);
      }
      lastScore = scoreFieldCOG[0];
    }
  }

  public void runMultipleIntPoints() throws InterruptedException {


    //CrossValidation
    int setSize = CData.imagesSize() / 2;
    double scores[] = new double[setSize];
    CCOGParams resultPC[] = new CCOGParams[setSize];
    CLoGParams resultPE[] = new CLoGParams[setSize];
    double overallScore = 0.0;
    int[] imgList = new int[setSize - 1];
    for (int i = 0; i < setSize; i++) {
      int heldout = i;
      int k = 0;
      for (int j = 0; j < setSize - 1; j++) {
        if (k == heldout) {
          k++;
        }
        imgList[j] = k;
        k++;
      }
      System.out.println("heldout now " + heldout);
      m.initConstants();
      m.initArrays();
      m.calculateWindowSizes();
      for (int iter = 0; iter < m.maxIters; iter++) {
        logger.info("started iter " + iter);
        //System.out.println("started iter " + iter);  
        m.initParams();
        m.runIntPtsOnList(imgList);
        m.updateConvergence();
        m.refillParams();
        if (m.convergenceGrowthCounter >= m.convergenceMaximumIters) {
          //logger.info("learning has g-converged, iters halted at iter " + iter + " w/ a score of " + m.lastScore);
          break;
        }
        if (m.convergenceMaxCounter >= m.convergenceMaximumIters) {
          //logger.info("learning has m-converged, iters halted at iter " + iter + " w/ a score of " + m.lastScore);
          break;
        }
      }
      CData.showImage[0] = 2 * heldout;
      CData.showImage[2] = 2 * heldout + 1;
      m.pc[0].windowSize = m.windowSizes[heldout];

      CInterestingPoints validWorker = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), Cornerer.COG, Edger.sobel, m.pc[0], m.pe[0], -1, 2 * heldout);
      CInterestingPointsThread validThread = new CInterestingPointsThread(validWorker, CData.getImage(CData.showImage[2]).getImage());
      validThread.start();
      validThread.join();

      CInterestingPoints randWorker = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), Cornerer.random, Edger.sobel, m.pc[0], m.pe[0], -2, 2 * heldout);
      CInterestingPointsThread randThread = new CInterestingPointsThread(randWorker, CData.getImage(CData.showImage[2]).getImage());
      randThread.start();
      randThread.join();
      System.out.println("Random score for heldout " + heldout + ": " + randThread.result);

      scores[heldout] = validThread.result;
      resultPC[heldout] = m.pc[0];
      resultPE[heldout] = m.pe[0];
      System.out.println("Validated score for heldout " + heldout + ": " + scores[heldout]);
    }

    for (int i = 0; i < setSize; i++) {
      System.out.println("score[" + i + "]: " + scores[i]);
      resultPC[i].systemPrint();
      resultPE[i].systemPrint();
      overallScore += scores[i];
    }
    System.out.println("overall score: " + overallScore / (double) setSize);

  }
}
