package workers.registration;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import image.converters.CNormalization;
import image.converters.Crgb2grey;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;
import utils.geometry.CPerspectiveTransformation;
import utils.metrics.CAreaSimilarityMetric;
import utils.metrics.CCovarianceMetric;
import utils.metrics.CCrossCorrelationMetric;
import workers.CImageWorker;
import workers.analyse.CAnalysisWorker;
import workers.analyse.CBestWindowSize;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CLoGParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.registration.CInterestingPoints;

import workers.registration.CInterestingPoints.Cornerer;
import workers.registration.CInterestingPoints.Edger;

/**
 *
 * @author Jakub
 */
public class CRansacRegister extends CAnalysisWorker {

  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  private BufferedImage inputA, inputB, output;
  private CPointsAndQualities ptsA, ptsB;
  private CInterestingPoints extractor;
  private double[][] correlations;
  private int[] refInitIdx = new int[4];
  private int[] sensedInitIdx = new int[4];
  private int[] refTopIdx = new int[4];
  private int[] sensedTopIdx = new int[4];
  private Point2D.Double[] refInit = new Point2D.Double[4];
  private Point2D.Double[] sensedInit = new Point2D.Double[4];
  private Point2D.Double[] refTop = new Point2D.Double[4];
  private Point2D.Double[] sensedTop = new Point2D.Double[4];
  private CPerspectiveTransformation topTrans = new CPerspectiveTransformation();
  LinkedList<Point2D.Double> inliersRef = new LinkedList<>();
  LinkedList<Point2D.Double> inliersSensed = new LinkedList<>();
  LinkedList<Point2D.Double> inliersRefMSS = new LinkedList<>();
  LinkedList<Point2D.Double> inliersSensedMSS = new LinkedList<>();
  CPerspectiveTransformation maxTrans = new CPerspectiveTransformation();
  int channelCount = 32;
  int iters;
  int inlierThresh;
  int CSThresh;
  static int MSERdelta = 11;
  private CDerotationCorrelator derotCorrelator;
  private CMSERCorrelator mserCorrelator;

  public CRansacRegister(BufferedImage reference, BufferedImage sensed) {
    inputA = reference;
    inputB = sensed;
    correlations = null;
    refInitIdx = new int[4];
    sensedInitIdx = new int[4];
    refInitIdx[0] = refInitIdx[1] = refInitIdx[2] = refInitIdx[3] = -1;
    sensedInitIdx[0] = sensedInitIdx[1] = sensedInitIdx[2] = sensedInitIdx[3] = -1;
    refInit = new Point2D.Double[4];
    sensedInit = new Point2D.Double[4];
    iters = 1000;
    inlierThresh = (inputA.getHeight() + inputB.getWidth()) / 40;
    derotCorrelator = new CDerotationCorrelator(reference, sensed);
    mserCorrelator = new CMSERCorrelator(reference, sensed);


  }

  ;
  
  private boolean getTopParams() {
    //logger.info("getting top params");
    LinkedList<Point2D.Double> inliersRefTop = new LinkedList<Point2D.Double>();
    LinkedList<Point2D.Double> inliersSensedTop = new LinkedList<Point2D.Double>();
    for (int p = 0; p < 4; p++) {

      double maxCor = 0.0;
      int maxA = -1;
      int maxB = -1;
      for (int i = 0; i < ptsA.size(); i++) {
        if (i == refTopIdx[0] || i == refTopIdx[1] || i == refTopIdx[2] || i == refTopIdx[3]) {
          continue;
        }
        for (int j = 0; j < ptsB.size(); j++) {
          if (j == sensedTopIdx[0] || j == sensedTopIdx[1] || j == sensedTopIdx[2] || j == sensedTopIdx[3]) {
            continue;
          }
          if (correlations[i][j] > maxCor) {
            maxA = i;
            maxB = j;
            maxCor = correlations[i][j];
          }
        }
      }
      if ((maxCor <= 0.0) || (maxA == -1) || (maxB == -1)) {
        logger.info("not enough good pts");
        return false;
      }

      refTopIdx[p] = maxA;
      sensedTopIdx[p] = maxB;
      refTop[p] = ptsA.getPoint(maxA);
      sensedTop[p] = ptsB.getPoint(maxB);
      inliersRefTop.add(ptsA.getPoint(maxA));
      inliersSensedTop.add(ptsB.getPoint(maxB));
      //for (int i = 0; i< ptsA.size(); i++) {
      //  correlations[i][maxB] = 0.0;
      //}
      //for (int i = 0; i< ptsB.size(); i++) {
      //  correlations[maxA][i] = 0.0;
      //}    
      //topTrans = new CPerspectiveTransformation();

    }
    //logger.info("top ref positions: " + refTop[0].toString() + refTop[1].toString() + refTop[2].toString() + refTop[3].toString()) ;
    //logger.info("top sensed positions: " + sensedTop[0].toString() + sensedTop[1].toString() + sensedTop[2].toString() + sensedTop[3].toString()) ;

    try {
      topTrans.set(inliersRefTop, inliersSensedTop);

    } catch (Exception e) {
      logger.info(e.toString());
    }
    return true;

  }

  private boolean getInitParams() {

    logger.info("getting params");
    double maxL = inputA.getWidth();
    double maxR = -1.0;
    double maxT = -1.0;
    double maxB = inputA.getHeight();
    int maxLI = -1;
    int maxRI = -1;
    int maxTI = -1;
    int maxBI = -1;
    for (int i = 0; i < ptsA.size(); i++) {
      if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) {
        continue;
      }
      if (ptsA.getPoint(i).y > maxT) {
        maxT = ptsA.getPoint(i).y;
        maxTI = i;
      };
    }
    logger.info("got ref 1 - T ");
    refInitIdx[0] = maxTI;
    refInit[0] = ptsA.getPoint(maxTI);
    for (int i = 0; i < ptsA.size(); i++) {
      if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) {
        continue;
      }
      if (ptsA.getPoint(i).y < maxB) {
        maxB = ptsA.getPoint(i).y;
        maxBI = i;
      };
    }
    refInitIdx[1] = maxBI;
    refInit[1] = ptsA.getPoint(maxBI);

    for (int i = 0; i < ptsA.size(); i++) {
      if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) {
        continue;
      }
      if (ptsA.getPoint(i).x < maxL) {
        maxL = ptsA.getPoint(i).x;
        maxLI = i;
      };
    }
    refInitIdx[2] = maxLI;
    refInit[2] = ptsA.getPoint(maxLI);

    for (int i = 0; i < ptsA.size(); i++) {
      if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) {
        continue;
      }
      if (ptsA.getPoint(i).x > maxR) {
        maxR = ptsA.getPoint(i).x;
        maxRI = i;
      };
    }
    refInitIdx[3] = maxRI;
    refInit[3] = ptsA.getPoint(maxRI);
    logger.info("got ref");

    maxL = inputB.getWidth();
    maxR = -1.0;
    maxT = -1.0;
    maxB = inputB.getHeight();
    maxLI = -1;
    maxRI = -1;
    maxTI = -1;
    maxBI = -1;
    for (int i = 0; i < ptsB.size(); i++) {
      if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) {
        continue;
      }
      if (ptsB.getPoint(i).y > maxT) {
        maxT = ptsB.getPoint(i).y;
        maxTI = i;
      };
    }
    sensedInitIdx[0] = maxTI;
    sensedInit[0] = ptsB.getPoint(maxTI);
    for (int i = 0; i < ptsB.size(); i++) {
      if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) {
        continue;
      }
      if (ptsB.getPoint(i).y < maxB) {
        maxB = ptsB.getPoint(i).y;
        maxBI = i;
      };
    }
    sensedInitIdx[1] = maxBI;
    sensedInit[1] = ptsB.getPoint(maxBI);
    for (int i = 0; i < ptsB.size(); i++) {
      if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) {
        continue;
      }
      if (ptsB.getPoint(i).x < maxL) {
        maxL = ptsB.getPoint(i).x;
        maxLI = i;
      };
    }
    sensedInitIdx[2] = maxLI;
    sensedInit[2] = ptsB.getPoint(maxLI);
    for (int i = 0; i < ptsB.size(); i++) {
      if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) {
        continue;
      }
      if (ptsB.getPoint(i).x > maxR) {
        maxR = ptsB.getPoint(i).x;
        maxRI = i;
      };
    }
    sensedInitIdx[3] = maxRI;
    sensedInit[3] = ptsB.getPoint(maxRI);



    logger.info("ref positions: " + refInit[0].toString() + refInit[1].toString() + refInit[2].toString() + refInit[3].toString());
    logger.info("sensed positions: " + sensedInit[0].toString() + sensedInit[1].toString() + sensedInit[2].toString() + sensedInit[3].toString());



    logger.info("got initial transform");
    return true;

  }

  private double getInliers(CPerspectiveTransformation trans) {
    double inlierCount = 0;
    boolean[] sensedUsed = new boolean[ptsB.size()];
    for (int i = 0; i < ptsA.size(); i++) {
      Point2D.Double refPt = ptsA.getPoint(i);
      for (int j = 0; j < ptsB.size(); j++) {
        if (sensedUsed[j]) {
          continue;
        }
        Point2D.Double sensedPt = ptsB.getPoint(j);
        Point2D.Double transformedRefPt = trans.getProjected(refPt);
        double dist = Math.sqrt((sensedPt.x - transformedRefPt.x) * (sensedPt.x - transformedRefPt.x)
                + (sensedPt.y - transformedRefPt.y) * (sensedPt.y - transformedRefPt.y));
        if (dist < inlierThresh && correlations[i][j] > 0.5) {
          inlierCount += correlations[i][j];
          sensedUsed[j] = true;
          inliersRef.add(refPt);
          inliersSensed.add(sensedPt);
          //logger.info("got inlier");
          break;

        }
      }

    }
    return inlierCount;
  }

  private boolean getRandomParams(double corrThresh) {
    if ((ptsA.size() < 4) || (ptsB.size() < 4)) {
      return false;
    }
    for (int p = 0; p < 4; p++) {
      int indexA;
      int indexB;
      int maxTries = 500;
      int tries = 0;
      double maxCorr = 0.0;
      int maxA = 0;
      int maxB = 0;

      do {
        tries++;


        indexA = (int) ((double) ptsA.size() * Math.random());
        while (indexA == refInitIdx[0] || indexA == refInitIdx[1] || indexA == refInitIdx[2] || indexA == refInitIdx[3]) {
          indexA = (int) ((double) ptsA.size() * Math.random());
        }
        indexB = (int) ((double) ptsB.size() * Math.random());;
        while (indexB == sensedInitIdx[0] || indexB == sensedInitIdx[1] || indexB == sensedInitIdx[2] || indexB == sensedInitIdx[3]) {
          indexB = (int) ((double) ptsB.size() * Math.random());
          if (indexA == ptsA.size()) {
            indexA--;
          }
          if (indexB == ptsB.size()) {
            indexA--;
          }
          if (maxCorr < correlations[indexA][indexB]) {
            maxA = indexA;
            maxB = indexB;
            maxCorr = correlations[indexA][indexB];
          }

        }
      } while ((maxCorr < corrThresh) && (tries < maxTries));
      //logger.info("m: " + maxCorr);
      if (maxCorr >= corrThresh) {
        //logger.info("not mtfail");
      }

      if (maxCorr <= 0.0) {
        maxA = indexA;
        maxB = indexB;
      }

      refInit[p] = ptsA.getPoint(maxA);
      refInitIdx[p] = maxA;
      sensedInit[p] = ptsB.getPoint(maxB);
      sensedInitIdx[p] = maxB;
    }



    return true;
  }

  @Override
  protected BufferedImage doInBackground() {

    System.out.println("Running ransac");
    correlations = derotCorrelator.doInBackground();
    this.ptsA = derotCorrelator.ptsA;
    this.ptsB = derotCorrelator.ptsB;
    CSThresh = ptsA.size() < ptsB.size() ? ptsA.size() / 2 : ptsB.size() / 2;


    System.out.println("got pts");


    boolean enoughPts = getInitParams();
    if (!(enoughPts)) {
      return inputA;
    }
    System.out.println("got init params");
    CPerspectiveTransformation trans = new CPerspectiveTransformation();
    trans.set(refInit, sensedInit);
    trans.print();

    System.out.println("got init trans");


    double inlierCountInit = getInliers(trans);
    System.out.println("got top params");

    enoughPts = getTopParams();
    if (!(enoughPts)) {
      topTrans.copy(trans);
    }
    topTrans.print();
    System.out.println("got top trans");

    double inlierCountTop = getInliers(topTrans);
    logger.info(inlierCountTop + "");
    if (inlierCountTop > inlierCountInit) {
      trans.copy(topTrans);
      logger.info("replacing universal transformation with top transformation");

    }
    System.out.println("compared the two");

    Point size = new Point(inputA.getWidth(), inputA.getHeight());

    BufferedImage output = new BufferedImage(size.x, size.y, inputA.getType());
    WritableRaster outRaster = output.getRaster();
    Raster in = inputB.getData();

    System.out.println("starting RANSAC");

    maxTrans.copy(trans);
    double maxInliers = 0.0;
    logger.info("total pts: refPts: " + ptsA.size() + " sensedPts: " + ptsB.size() + " total: " + ptsA.size() * ptsB.size());
    CPerspectiveTransformation firstTrans = new CPerspectiveTransformation();
    //CSThresh = ptsA.size() < ptsB.size() ? ptsA.size()/2 : ptsB.size()/2;
    for (int iter = 0; iter < iters; iter++) {
      inliersRef.clear();
      inliersSensed.clear();
      //inliersRefMSS.clear();
      //inliersSensedMSS.clear();
      double inlierCount = getInliers(trans);
      if (inlierCount > maxInliers) {
        maxTrans.copy(trans);
        maxInliers = inlierCount;
      }
      if (inlierCount >= CSThresh) {
        //logger.info("passed CSThresh of " + CSThresh);
        //  break;
      }

      double corrThresh = 1 - ((double) iter / (4 * iters));
      getRandomParams(corrThresh);
      boolean succesful = false;
      while (!succesful) {
        try {
          trans.set(refInit, sensedInit);
          succesful = true;
        } catch (java.lang.RuntimeException e) {
          System.out.println(e.toString());
          getRandomParams(corrThresh);
        }
      }
      logger.info("iter " + iter + ": inliers found: " + inlierCount + " corrT: " + corrThresh);
      //logger.info("iter " + iter + ": "); 
      //trans.print();
      if ((corrThresh == 1.0) && (inlierCount > 4)) {
        firstTrans.set(inliersRef, inliersSensed);
      }
    }
    getInliers(firstTrans);
    trans.set(inliersRef, inliersSensed);
    trans.print();


    logger.info("max inliers: " + maxInliers);


    int x, y;
    int[] pixel = new int[3], black = {0, 0, 0};
    Point2D.Double ref;
    //trans.print();

    for (int i = 0; i < maxTrans.a.length; i++) {
      logger.info("" + maxTrans.a[i]);

    }

    /*
     * maxTrans.a[0] = 1; maxTrans.a[1] = 0; maxTrans.a[2] = 10; maxTrans.a[3] =
     * 0; maxTrans.a[4] = 1; maxTrans.a[5] = -67; maxTrans.a[6] = 0;
     * maxTrans.a[7] = 0; maxTrans.a[8] = 1;
     */
    maxInliers = getInliers(maxTrans);
    logger.info("maxInliers: " + maxInliers);

    System.out.println("");
    //maxTrans.print();

    for (x = 0; x < size.x; x++) {
      for (y = 0; y < size.y; y++) {
        ref = maxTrans.getProjected(new Point2D.Double(x, y));
        if (ref.x < inputB.getWidth() && ref.x >= 0
                && ref.y < inputB.getHeight() && ref.y >= 0) {
          in.getPixel((int) ref.x, (int) ref.y, pixel);
        } else {
          pixel = black;
        }
        outRaster.setPixel(x, y, pixel);
      }
    }
    output.setData(outRaster);
    logger.info("image transformed");
    return output;

  }

  @Override
  public String getWorkerName() {
    return "Ransac Register";
  }
}
