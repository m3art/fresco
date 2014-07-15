/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import fresco.CData;
import fresco.action.CActionManager;
import fresco.swing.CDrawPanel;
import image.converters.Crgb2grey;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
import workers.analyse.CBestWindowSize;
import workers.analyse.CInterestingPointsThread;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CLoGParams;

/**
 *
 * @author Jakub
 */
public class CCOGLearning {

  /**
   * learning parameters
   */
  /**
   * learning rate for parameters of COG corner detector
   */
  private double learnRate;
  /**
   * learning rate for the variance of the LoG edge detector gaussian one order
   * of magnitude greater than COG params, as values are also an order of
   * magnitude larger
   */
  private double learnRateLOGSigma;
  /**
   * learning rate for the size of the LoG gaussian one order of magnitude
   * greater than COG params, as values are also one order of magnitude larger
   */
  private double learnRateLOGSize;
  /**
   * maximum iterations of the learning alg
   */
  private int maxIters ;
  /**
   * derivative is calculated numerically therefore, we must choose, how far
   * apart we sample the score function to determine the derivative this is a
   * multiplicative factor
   */
  private double derStep;
  /**
   * The learning procedure uses regularization this is the weight of the
   * regularization member
   */
  private double lambda;
  /**
   * Convergence regulating parameters
   */
  /**
   * increase of score smaller than this is not considered an increase
   */
  private double convergenceConstant;
  /**
   * for how many iters the score function has not grown
   */
  private int convergenceGrowthCounter;
  /**
   * for how many iters the score function has not beaten its maximum
   */
  private int convergenceMaxCounter;
  /**
   * how many iters the function is allowed to not grow (see
   * convergenceConstant) or not improve the overall maximum
   */
  private int convergenceMaximumIters;
  /**
   * overall maximum score reached during learning
   */
  private double maxScore ;
  /**
   * score reached in this iteration
   */
  private double lastScore;
  /**
   * number of loaded pictures - should be half number of files loaded
   */
  private int setSize;
  /**
   * number of parameters being learned
   */
  private int paramCount;
  private BufferedImage imageA, imageAGreyscale;
  /**
   * arrays
   */
  /**
   * best window sizes
   */
  private int[] windowSizes;
  /**
   * growth of the score function in the direction of the params, as calculated
   * during learning
   */
  private double[] growth;
  /**
   * cornerer parameters for the individual threads
   */
  private CCOGParams[] pc;
  /**
   * edger parameters for the individual threads
   */
  private CLoGParams[] pe;
  /**
   * array to store individual threads
   */
  private CInterestingPointsThread[] threadField;
  /**
   * array to store workers the threads are running
   */
  private CInterestingPoints[] workerField;
  /**
   * array to store scores calculated by the threads
   */
  private double[] scoreField;
  CDrawPanel[] imagePanel;
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  public CCOGLearning(CDrawPanel[] imagePanel) {
    this.imagePanel = imagePanel;
    this.initConstants();
    this.initArrays();
  }

  public void initConstants() {



    learnRate = 2.0;
    learnRateLOGSigma = 10 * learnRate;
    learnRateLOGSize = 10 * learnRate;
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
    threadField = new CInterestingPointsThread[paramCount + 1];
    workerField = new CInterestingPoints[paramCount + 1];
    scoreField = new double[paramCount + 1];
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
      /*
       * if (!CData.input2Showed) { show2ndInput(); }
       */
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

  /**
   * Method runs at start of each learning iteration Increments individual
   * parameters by derStep setting them up for threads to calculate derivatives
   * in directions of the params
   */
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

  /**
   * Method calculates the steepest direction of the score function, grows the
   * parameters accordingly and refills the parameter arrays with the new
   * parameter values, thus readying them for the next iteration of learning
   *
   * Method also empties the arrays used for storing the workers, threads and
   * score field.
   */
  public void refillParams() {
    for (int j = 0; j < paramCount; j++) {
      growth[j] = (scoreField[j + 1] - scoreField[0]);
      if (growth[j] > 0) {
        growth[j] -= lambda * pc[j].getRegularization();
      } else {
        growth[j] += lambda * pc[j].getRegularization();
      }
    }

    pc[0].centerWhitenessW = pc[0].centerWhitenessW + growth[0] * learnRate;
    pc[0].distW = pc[0].distW + growth[1] * learnRate;
    pc[0].perpCW = pc[0].perpCW + growth[2] * learnRate;
    pc[0].whiteDiffW = pc[0].whiteDiffW + growth[3] * learnRate;
    pc[0].thresholdq = pc[0].thresholdq + growth[4] * learnRate;
    pc[0].mixW = pc[0].mixW + growth[5] * learnRate;
    pc[0].normalizeWeights();
    pe[0].gaussSigma = pe[0].gaussSigma + growth[6] * learnRateLOGSigma;
    pe[0].gaussSizeQ = pe[0].gaussSizeQ + growth[7] * learnRateLOGSize;
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
      workerField[j] = null;
      threadField[j] = null;
      scoreField[j] = 0.0;
    }
  }

  /**
   * runs CInterestingPoints using params from pc[], pe[] on all loaded images
   * at once
   *
   * expects images and tagged references loaded in alternating order: image1 -
   * reference1 - image2 - reference2 - ...
   */
  public void runIntPtsOnAllImgs() {
    for (int i = 0; i < CData.imagesSize(); i += 2) {
      //Odd number of images
      // -> abort
      if (CData.imagesSize() == i + 1) {
        break;
      }
      if (!CData.input2Showed) {
//        show2ndInput();
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
        workerField[j] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CInterestingPoints.Cornerer.COG, CInterestingPoints.Edger.sobel, pc[j], pe[j], j, i);
        threadField[j] = new CInterestingPointsThread(workerField[j], CData.getImage(CData.showImage[2]).getImage());
        threadField[j].start();
      }
      for (int j = 0; j < paramCount + 1; j++) {
        try {
          threadField[j].join();
        } catch (InterruptedException e) {
          logger.log(Level.SEVERE, "Thread " + j + " interrupted");
        }
        scoreField[j] += (threadField[j].result) / (double) setSize;
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
    System.out.println("COG base score: " + scoreField[0]);
  }

  /**
   * runs CInterestingPoints using the params in pc[], pe[] on images in the
   * supplied list
   *
   * expects images and tagged references loaded in alternating order: image1 -
   * reference1 - image2 - reference2 - ...
   *
   *
   * @param imgIDs - image IDs. To run on images 1 and 3, supply imgIDs == {1,
   * 3}, and load them in order image1 - reference1 - image2 - reference2 -
   * image3 - reference 3 (total of >= 6 loaded files
   */
  public void runIntPtsOnList(int[] imgIDs) {
    for (int i = 0; i < imgIDs.length; i++) {

      if (!CData.input2Showed) {
//        show2ndInput();
      }
      CData.showImage[0] = 2 * imgIDs[i];
      System.out.println("Now working on image no. " + imgIDs[i] + " (file No. "+(2*imgIDs[i])+")");
      imagePanel[0].setSizeByImage();
      CData.showImage[2] = 2 * imgIDs[i] + 1;
      imagePanel[2].setSizeByImage();
      for (int j = 0; j < paramCount + 1; j++) {
        pc[j].windowSize = windowSizes[imgIDs[i]];
      }
      for (int j = 0; j < paramCount + 1; j++) {
        //logger.info("i. e. CW: " + pc[j].centerWhitenessW + " dist: " + pc[j].distW + " perpC: " + pc[j].perpCW + "whiteDiff: " + pc[j].whiteDiffW);
        workerField[j] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CInterestingPoints.Cornerer.COG, CInterestingPoints.Edger.sobel, pc[j], pe[j], j, i);
        threadField[j] = new CInterestingPointsThread(workerField[j], CData.getImage(CData.showImage[2]).getImage());
        threadField[j].start();
      }
      for (int j = 0; j < paramCount + 1; j++) {
        try {
          threadField[j].join();
        } catch (InterruptedException e) {
          logger.log(Level.SEVERE, "Thread " + j + " interrupted");
        }
        //score is averaged out over all images
        scoreField[j] += (threadField[j].result) / (double) imgIDs.length;
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
    System.out.println("COG base score: " + scoreField[0]);
  }

  /**
   * Method that updates all convergence criteria
   */
  public void updateConvergence() {
    if ((scoreField[0] - lastScore) < convergenceConstant) {
      convergenceGrowthCounter++;
      //logger.info("convergence growth counter now at " + convergenceGrowthCounter);
    } else {
      convergenceGrowthCounter = 0;
      //logger.info("convergence growth counter now at " + convergenceGrowthCounter);
    }
    if (scoreField[0] < maxScore) {
      convergenceMaxCounter++;
      //logger.info("convergence max counter now at " + convergenceMaxCounter);
    } else {
      maxScore = scoreField[0];
      convergenceMaxCounter = 0;
      //logger.info("convergence max counter now at " + convergenceMaxCounter);
    }
    lastScore = scoreField[0];
  }

  private void runIter() {
  }

  private void runLearning(int[] imgList) {
    for (int iter = 0; iter < maxIters; iter++) {
      logger.info("started iter " + iter);
      initParams();
      runIntPtsOnList(imgList);
      updateConvergence();
      refillParams();
      if (convergenceGrowthCounter >= convergenceMaximumIters) {
        break;
      }
      if (convergenceMaxCounter >= convergenceMaximumIters) {
        break;
      }
    }
  }

  /**
   *
   */
  public void runCrossVal() throws InterruptedException {
    //int setSize = CData.imagesSize() / 2;
    double scores[] = new double[setSize];
    CCOGParams resultPC[] = new CCOGParams[setSize];
    CLoGParams resultPE[] = new CLoGParams[setSize];
    double overallScore = 0.0;
    int[] imgList = new int[setSize - 1];
    for (int i = 0; i < setSize; i++) {
      int heldout = i;
      int k = 0;
      //construct image list
      for (int j = 0; j < setSize - 1; j++) {
        if (k == heldout) {
          k++;
        }
        imgList[j] = k;
        k++;
      }
      System.out.println("heldout now " + heldout);
      //initialize
      initConstants();
      initArrays();
      calculateWindowSizes();
      //run learning - stores results in all pc[], pe[] thanks to refillParams()
      runLearning(imgList);
      CData.showImage[0] = 2 * heldout;
      CData.showImage[2] = 2 * heldout + 1;

      //run CInterestingPoints on heldout with learned params. Only change required = windowSize
      pc[0].windowSize = windowSizes[heldout];
      CInterestingPoints validWorker = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CInterestingPoints.Cornerer.COG, CInterestingPoints.Edger.sobel, pc[0], pe[0], -1, 2 * heldout);
      CInterestingPointsThread validThread = new CInterestingPointsThread(validWorker, CData.getImage(CData.showImage[2]).getImage());
      validThread.start();
      validThread.join();
      //run CInterestingPoints on heldout with random cornerer to get baseline
      CInterestingPoints randWorker = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CInterestingPoints.Cornerer.random, CInterestingPoints.Edger.sobel, pc[0], pe[0], -2, 2 * heldout);
      CInterestingPointsThread randThread = new CInterestingPointsThread(randWorker, CData.getImage(CData.showImage[2]).getImage());
      randThread.start();
      randThread.join();
      System.out.println("Random score for heldout " + heldout + ": " + randThread.result);

      scores[heldout] = validThread.result;
      //store learned params
      resultPC[heldout] = pc[0];
      resultPE[heldout] = pe[0];
      System.out.println("Validated score for heldout " + heldout + ": " + scores[heldout]);
    }
    //print results
    for (int i = 0; i < setSize; i++) {
      System.out.println("score[" + i + "]: " + scores[i]);
      resultPC[i].systemPrint();
      resultPE[i].systemPrint();
      overallScore += scores[i];
    }
    System.out.println("overall score: " + overallScore / (double) setSize);


  }
}
