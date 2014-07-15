/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import Jama.Matrix;
import image.colour.CBilinearInterpolation;
import image.converters.Crgb2grey;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;
import utils.CIdxMapper;
import utils.geometry.CPerspectiveTransformation;
import utils.metrics.CAreaSimilarityMetric;
import utils.metrics.CCrossCorrelationMetric;
import workers.CImageWorker;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.analyse.paramObjects.CLoGParams;

/**
 * This class calculates correlations between interesting points supplied by a
 * method designed to gather interesting points from an image
 *
 * @author Jakub
 */
public class CMSERCorrelator extends CImageWorker<double[][], Void> {

  public static int MSER_LOW_SIZE_LIMIT;
  public static int MSER_DELTA;
  public static CIdxMapper map;
  public CPointsAndQualities ptsA, ptsB;
  private static BufferedImage inputA, inputB;
  public static int MSERSearchDim = 51;
  final static int channelCount = 32;
  final static int maxGrad = 255;
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  public CMSERCorrelator(int iMSERdelta, int iMSERLowSizeLimit, BufferedImage reference, BufferedImage sensed) {


    this.MSER_DELTA = iMSERdelta;
    this.MSER_LOW_SIZE_LIMIT = iMSERLowSizeLimit;
    this.map = new CIdxMapper();
    this.inputA = reference;
    this.inputB = sensed;


  }

  public CMSERCorrelator(int iMSERdelta, int iMSERLowSizeLimit) {
    this.MSER_DELTA = iMSERdelta;
    this.MSER_LOW_SIZE_LIMIT = iMSERLowSizeLimit;
    this.map = new CIdxMapper();
  }

  public CMSERCorrelator() {
    this.MSER_DELTA = 1;
    this.MSER_LOW_SIZE_LIMIT = 9;
    this.map = new CIdxMapper();
  }

  public CMSERCorrelator(BufferedImage reference, BufferedImage sensed) {
    this.MSER_DELTA = 1;
    this.MSER_LOW_SIZE_LIMIT = 9;
    this.map = new CIdxMapper();
    this.inputA = reference;
    this.inputB = sensed;


  }

  /**
   * get the Maximally stable region from information about region sizes for
   * different intensities
   */
  protected static int getMSERIntensity(int[] sizes) {
    int maxStableIntensity = 0;
    double minSizeChange = Integer.MAX_VALUE;
    for (int i = MSER_DELTA; i < sizes.length - MSER_DELTA; i++) {
      //Do not allow too small regions - one pixel is not an mser
      if (sizes[i] < MSER_LOW_SIZE_LIMIT) {
        continue;
      }
      // Calculate fluctuations in size, record minimal
      double sizeChange = Math.abs(sizes[i - MSER_DELTA] - sizes[i + MSER_DELTA]) / (double) sizes[i];
      if (sizeChange < minSizeChange) {
        minSizeChange = sizeChange;
        maxStableIntensity = i;
      }

    }
    if (minSizeChange == Integer.MAX_VALUE) {
      return -1;
    }
    return maxStableIntensity;
  }

  /**
   * internatl method to avoid code copying processes single point in BFS
   */
  protected static void processPoint(
          Point2D.Double input,
          LinkedList<Point2D.Double> boundary,
          LinkedList<Point2D.Double> nextBoundary,
          boolean[] isBoundary,
          int[] surround,
          int[] MSERtable,
          int thresholdIntensity,
          int surroundWidth) {

    int idx = map.toIdx(input, surroundWidth);
    int testPx = (int) surround[idx];
    if (thresholdIntensity <= testPx && MSERtable[idx] == -1 && !isBoundary[idx]) {
      isBoundary[idx] = true;
      boundary.addLast(input);
    }
    if (thresholdIntensity > testPx) {
      nextBoundary.addLast(input);
    }
  }

  /**
   * gets an mser from the region, if exists positions belonging to mser stored
   * in int[] as 1 positions not belonging to mser stored in int[] as 0
   */
  protected static int[] getMserPlus(int[] surround, int w, int h) {
    //temp variable to record at what intensity was pixel (x, y) added to extremal region
    int[] MSERtable = new int[w * h];
    //indicator variable - to prevent adding pixels to boundary more than once
    boolean[] isBoundary = new boolean[w * h];
    //boundary to be explored for this intensity - essentially a BFS queue
    LinkedList<Point2D.Double> boundary = new LinkedList<>();
    //boundary to be explored for lower intensities
    LinkedList<Point2D.Double> nextBoundary = new LinkedList<>();

    //initialize all pixels as "not in any extremal region"
    for (int i = 0; i < w * h; i++) {
      MSERtable[i] = -1;
    }
    //initialize boundary with middle of examined region
    boundary.addLast(new Point2D.Double((int) (w / 2), (int) (h / 2)));
    int centerPx = (int) surround[((h / 2) * w) + (w / 2)];
    MSERtable[map.toIdx(h / 2, w / 2, w)] = centerPx;
    //this var will store a record of what the size of the extremal region is for each intensity
    int[] sizes = new int[centerPx + 1];
    //sizes[0] = 1;
    int lastSize = 0;
    boolean touch = false;

    for (int intensity = centerPx; intensity >= 0; intensity--) {
      //System.out.println(intensity + ": " + touch);
      //conduct BFS for regions connected to center pixel via pixels with high enough intensity
      while (boundary.size() > 0) {
        Point2D.Double current = boundary.removeFirst();
        int currindex = map.toIdx(current, w);
        int currentPx = (int) surround[currindex];
        isBoundary[currindex] = false;
        if (currentPx < intensity) {
          nextBoundary.addLast(current);
        } else {
          if (map.isEdge(current, w, h)) {
            touch = true;
          }
          //When not previously in putative MSER, increase MSER size
          if (MSERtable[map.toIdx(current, w)] == -1) {
            lastSize++;
          }
          // Mark position in MSERtable with intensity at which it has joined into putative MSER
          MSERtable[map.toIdx(current, w)] = intensity;
          //Test neighbouring pixels in four directions
          if (!map.isLeftEdge(current, w, h)) {
            Point2D.Double leftPx = new Point2D.Double(current.x - 1, current.y);
            CMSERCorrelator.processPoint(leftPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isRightEdge(current, w, h)) {
            Point2D.Double rightPx = new Point2D.Double(current.x + 1, current.y);
            CMSERCorrelator.processPoint(rightPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isTopEdge(current, w, h)) {
            Point2D.Double upPx = new Point2D.Double(current.x, current.y - 1);
            CMSERCorrelator.processPoint(upPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isBottomEdge(current, w, h)) {
            Point2D.Double downPx = new Point2D.Double(current.x, current.y + 1);
            CMSERCorrelator.processPoint(downPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
        }
      }
      while (nextBoundary.size() > 0) {
        boundary.addLast(nextBoundary.removeFirst());
      }
      if (!touch) {
        sizes[intensity] = lastSize;
      } else {
        for (int countdown = intensity; countdown >= 0; countdown--) {
          sizes[countdown] = 0;
        }
        break;
      }

    }

    int maxStableIntensity = getMSERIntensity(sizes);

    if (maxStableIntensity == -1) {
      return null;
    }


    int[] red = {255, 0, 0};
    for (int i = 0; i < w * h; i++) {
      if (MSERtable[i] >= maxStableIntensity) {
        MSERtable[i] = 1;
      } else {
        MSERtable[i] = 0;
      }
    }

    return MSERtable;

  }

  /**
   * returns center of gravity of input region (mat)
   */
  protected static Point2D.Double getCenterOfGravity(int[] mat, int w, int h) {
    double norm = 0;
    Point2D.Double CenterOfGravity = new Point2D.Double();
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        CenterOfGravity.x += mat[y1 * w + x1] * x1;
        CenterOfGravity.y += mat[y1 * w + x1] * y1;
        norm += mat[y1 * w + x1];
      }
    }
    CenterOfGravity.x /= norm;
    CenterOfGravity.y /= norm;
    return CenterOfGravity;

  }

  /**
   * gets the covariance matrix of the input
   */
  protected static double[][] getCovMatrix(int[] mat, int w, int h) {
    double[][] cov = new double[2][2];
    double norm = 0;
    //int sizeW =w/2;
    //int sizeH =h/2;
    Point2D.Double COG = getCenterOfGravity(mat, w, h);
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        cov[0][0] += (x1 - COG.x) * (x1 - COG.x) * mat[y1 * w + x1];
        cov[0][1] += (x1 - COG.x) * (y1 - COG.y) * mat[y1 * w + x1];
        cov[1][0] += (y1 - COG.y) * (x1 - COG.x) * mat[y1 * w + x1];
        cov[1][1] += (y1 - COG.y) * (y1 - COG.y) * mat[y1 * w + x1];
        norm += mat[y1 * w + x1];
      }
    }
    //double normX = (1.0/3.0)*(sizeW)*(sizeW + 1.0)*(2*sizeW+1.0) * h;
    //double normY = (1.0/3.0)*(sizeH)*(sizeH + 1.0)*(2*sizeH+1.0) * w;
    //double normXY = (w/2)*(w/2+1) * (h/2)*(h/2+1);
    cov[0][0] /= norm;
    cov[0][1] /= norm;
    cov[1][0] /= norm;
    cov[1][1] /= norm;
    return cov;
  }

  /**
   * this method should transform the region indicated by the mser table such
   * that the covariance matrix of its shape is diagonal.
   */
  protected static double[][] getNormalizedRegion(int[] region, int[] mser, int w, int h) {

    double cov[][] = getCovMatrix(mser, w, h);
    int centerX = w / 2;
    int centerY = h / 2;

    //hereafter we run in JAMA
    //convert covariance matrix to JAMA format
    Matrix mCov = new Matrix(cov);
    //get eiegenvalues, eigenvecotrs
    Matrix mVectors = mCov.eig().getV();
    Matrix mValues = mCov.eig().getD();
    //get COG, in JAMA format
    Point2D.Double COG = getCenterOfGravity(mser, w, h);
    Matrix mCOG = new Matrix(2, 1);
    mCOG.set(0, 0, COG.x);
    mCOG.set(1, 0, COG.y);
    mValues.set(0, 0, Math.sqrt(mValues.get(0, 0)));
    mValues.set(1, 1, Math.sqrt(mValues.get(1, 1)));

    mValues.set(0, 0, mValues.get(0, 0) / (w / 5));
    mValues.set(1, 1, mValues.get(1, 1) / (w / 5));


    //get transform matrix from normalized to original MSER
    Matrix normToRegion = mVectors.times(mValues);

    //transform the mser into a 2D table
    //get values of original image instead of true/false
    double[][] origMser = new double[h][w];
    for (int i = 0; i < w * h; i++) {
      origMser[i / w][i % w] = mser[i] * region[i];
    }
    double[][] normalized = new double[h][w];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        //get the current point (x, y) as a JAMA Matrix
        double[][] pt = new double[2][1];
        pt[0][0] = (x - centerX);
        pt[1][0] = (y - centerY);
        Matrix X = new Matrix(pt);
        //transform the point using the normalized -> original MSER transform matrix
        Matrix origCoords = normToRegion.times(X);

        //translate according to the center of gravity
        origCoords.plusEquals(mCOG);
        //System.out.println("interpolating from: " + origCoords.get(0, 0) + ", " + origCoords.get(1,0));

        //if not in original rectangle => not in original MSER -> put 0.0 in normalized MSER
        if (origCoords.get(0, 0) >= w - 1 || origCoords.get(1, 0) >= h - 1 || origCoords.get(0, 0) < 0 || origCoords.get(1, 0) < 0) {
          normalized[y][x] = 0.0;
        } //otherwise interpolate from original MSER using calculated coords
        else {
          normalized[y][x] = CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), origMser);
        }
      }
    }
    return normalized;
  }

  /**
   * find out in which direction the gradient is maximal. This direction falls
   * in one of channelCount channels, return this channel
   */
  protected static int getMaxChannel(double[][] region, int w, int h, int channelCount, int maxGrad) {

    int[] channelHistTmp = new int[channelCount];
    int[] channelHist = new int[channelCount];
    for (int i = 0; i < channelCount; i++) {
      channelHistTmp[i] = 0;
    }
    for (int x = 1; x < w - 1; x++) {
      for (int y = 1; y < h - 1; y++) {
        //if (mser[y*h+x+1] == 0 || mser[y*h+x-1] == 0 || mser[(y+1)*h+x] == 0 || mser[(y-1)*h+x] == 0) continue;

        double gradX = (region[y][x + 1] - region[y][x - 1]) / maxGrad;
        double gradY = (region[y + 1][x] - region[y - 1][x]) / maxGrad;
        double gradSize = Math.sqrt(gradX * gradX + gradY * gradY);
        if (gradSize == 0) {
          continue;
        }
        double rot = Math.acos(gradX / gradSize);
        rot /= Math.PI;
        //gradY >= 0 indicates angle between PI and 2*P
        if (gradY >= 0.0) {
          rot = 2.0 - rot;
        }
        //trasnform 2*PI rotation to no rotation
        if (rot == 2.0) {
          rot = 0.0;
        }
        if (rot >= 2.0) {
          //System.out.println(gradX);
        }
        //This should always be calculzated on a normalized region. It's edges within a square patch should not count towards the gradient histogram
        if ((region[y][x + 1] > 0) && (region[y][x - 1] > 0) && (region[y + 1][x] > 0) && (region[y - 1][x] > 0)) {
          channelHistTmp[(int) ((rot / (2.0)) * (double) channelCount)]++;
        }
      }
    }
    int maxChannel = -1;
    int maxVal = 0;

    for (int i = 1; i < channelCount - 1; i++) {
      channelHist[i] = (channelHistTmp[i - 1] + channelHistTmp[i] + channelHistTmp[i + 1]) / 3;
      //System.out.println(channelHist[i]);
    }

    for (int i = 0; i < channelCount; i++) {
      if (channelHist[i] > maxVal) {
        maxVal = channelHist[i];
        maxChannel = i;
      }
    }
    return maxChannel;
  }

  /**
   * rotates region by an angle corresponding to channel/channelCount * 2*pi
   */
  protected static double[] rotateByChannel(double[][] region, int channel, int channelCount, int w, int h) {
    double[] rotated = new double[w * h];
    // this is the angle by which we suspect the region is rotated from the norm position
    //therefore, we look for the pixel required for the rotated region at a position offset by this rotation in the original region
    double theta = -1 * (double) channel / channelCount * 2 * Math.PI;
    //System.out.println("rot by " + theta);
    int centerX = w / 2;
    int centerY = h / 2;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {

        double origX = Math.cos(theta) * (x - centerX) - Math.sin(theta) * (y - centerY);
        double origY = Math.sin(theta) * (x - centerX) + Math.cos(theta) * (y - centerY);
        if ((origX + centerX) >= w - 1 || (origX + centerX) < 0 || (origY + centerY) >= h - 1 || (origY + centerY) < 0) {

          rotated[y * w + x] = 0.0;
        } else {
          rotated[y * w + x] = CBilinearInterpolation.getValue(new Point2D.Double(origX + centerX, origY + centerY), region);
        }

      }
    }

    return rotated;
  }

  protected static double[] getNormFromRegion(int[] region, int w, int h) {
    int[] mser = getMserPlus(region, w, h);
    if (mser == null) {
      return null;
    }
    double[][] normalized = getNormalizedRegion(region, mser, w, h);
    int maxChannel = getMaxChannel(normalized, w, h, channelCount, maxGrad);
    return rotateByChannel(normalized, maxChannel, channelCount, w, h);

  }

  public static double[][] getCorrs(CPointsAndQualities ptsA, CPointsAndQualities ptsB) {

    int dim = MSERSearchDim / 2;
    double[][] result = new double[ptsA.size()][ptsB.size()];


    Raster rasterA = Crgb2grey.oneBandImage(inputA).getData();
    Raster rasterB = Crgb2grey.oneBandImage(inputB).getData();
    /*
     * int[] px = {0, 0, 0}; for (int j = 551; j < 602; j++) {
     *
     * for (int i = 634; i < 685; i++) { px = rasterA.getPixel(i, j, px);
     * System.out.format("%d,", px[0]); } System.out.println("");
    }
     */

    LinkedList<double[]> rotAPlus = new LinkedList<>();
    LinkedList<double[]> rotAMinus = new LinkedList<>();
    LinkedList<double[]> rotBPlus = new LinkedList<>();
    LinkedList<double[]> rotBMinus = new LinkedList<>();
    LinkedList<Integer> rotAPlusIdx = new LinkedList<>();
    LinkedList<Integer> rotAMinusIdx = new LinkedList<>();
    ArrayList<Integer> rotBPlusIdx = new ArrayList<>();
    ArrayList<Integer> rotBMinusIdx = new ArrayList<>();
    /*
     * LinkedList<double[]> momAPlus = new LinkedList<>(); LinkedList<double[]>
     * momAMinus = new LinkedList<>(); LinkedList<double[]> momBPlus = new
     * LinkedList<>(); LinkedList<double[]> momBMinus = new LinkedList<>();
     * LinkedList<Integer> momAPlusIdx = new LinkedList<>(); LinkedList<Integer>
     * momAMinusIdx = new LinkedList<>(); ArrayList<Integer> momBPlusIdx = new
     * ArrayList<>(); ArrayList<Integer> momBMinusIdx = new ArrayList<>();
     */



    CCrossCorrelationMetric CCmetric = new CCrossCorrelationMetric(inputA, inputB, MSERSearchDim, CAreaSimilarityMetric.Shape.RECTANGULAR);
    for (int i = 0; i < ptsA.size(); i++) {

      Point2D.Double a = ptsA.getPoint(i);
      int AX = (int) a.x;
      int AY = (int) a.y;
      int[] currentA = null;
      if (!map.isNearEdge(a, rasterA.getWidth(), rasterA.getHeight(), dim)) {
        currentA = rasterA.getSamples(AX - dim, AY - dim, MSERSearchDim, MSERSearchDim, 0, currentA);
        double[] MSERAPlus = getNormFromRegion(currentA, MSERSearchDim, MSERSearchDim);

        if (MSERAPlus != null) {
          int[] red = {255, 0, 0};
          inputA.getRaster().setPixel((int) ptsA.getPoint(i).x, (int) ptsA.getPoint(i).y, red);
          rotAPlus.push(MSERAPlus);
          rotAPlusIdx.push(i);
        }
        //invert area
        for (int pos = 0; pos < currentA.length; pos++) {
          currentA[pos] = 255 - currentA[pos];
        }
        double[] MSERAMinus = getNormFromRegion(currentA, MSERSearchDim, MSERSearchDim);
        if (MSERAMinus != null) {
          rotAMinus.push(MSERAMinus);
          rotAMinusIdx.push(i);
        }

      }
    }
    for (int j = 0; j < ptsB.size(); j++) {
      Point2D.Double b = ptsB.getPoint(j);
      int BX = (int) b.x;
      int BY = (int) b.y;
      int[] currentB = null;
      if (!map.isNearEdge(b, rasterB.getWidth(), rasterB.getHeight(), dim)) {
        currentB = rasterB.getSamples(BX - dim, BY - dim, MSERSearchDim, MSERSearchDim, 0, currentB);
        double[] MSERBPlus = getNormFromRegion(currentB, MSERSearchDim, MSERSearchDim);
        if (MSERBPlus != null) {
          int[] red = {255, 0, 0};
          inputB.getRaster().setPixel((int) ptsB.getPoint(j).x, (int) ptsB.getPoint(j).y, red);
          rotBPlus.push(MSERBPlus);
          rotBPlusIdx.add(j);
        }
        //invert area
        for (int pos = 0; pos < currentB.length; pos++) {
          currentB[pos] = 255 - currentB[pos];
        }

        double[] MSERBMinus = getNormFromRegion(currentB, MSERSearchDim, MSERSearchDim);
        if (MSERBMinus != null) {
          rotBMinus.push(MSERBMinus);
          rotBMinusIdx.add(j);
        }
      }
    }



    double maxCorrVal = 0.0;
    while (rotAPlusIdx.size() > 0) {
      int i = rotAPlusIdx.removeFirst();
      System.out.println(rotAPlusIdx.size());
      
      double[] inputI = rotAPlus.removeFirst();
      double[][] inputI2 = new double[MSERSearchDim][MSERSearchDim];
//      for (int a = 0; a < MSERSearchDim; a++) {
//        for (int b = 0; b < MSERSearchDim; b++) {
//          inputI2[b][a] = inputI[b * MSERSearchDim + a];
//        }
//      }
      for (int y = 0; y < rotBPlus.size(); y++) {
        int j = rotBPlusIdx.get(y);
        double[] inputJ = rotBPlus.get(y);
        double[][] inputJ2 = new double[MSERSearchDim][MSERSearchDim];
//        for (int a = 0; a < MSERSearchDim; a++) {
//          for (int b = 0; b < MSERSearchDim; b++) {
//            inputJ2[b][a] = inputJ[b * MSERSearchDim + a];
//          }
//        }
        result[i][j] = CCmetric.getValue(inputI, inputJ);
        
      }
    }

    while (rotAMinusIdx.size() > 0) {

      int i = rotAMinusIdx.removeFirst();

      double[] inputI = rotAMinus.removeFirst();
      for (int y = 0; y < rotBMinus.size(); y++) {
        int j = rotBMinusIdx.get(y);
        double[] inputJ = rotBMinus.get(y);
        if (CCmetric.getValue(inputI, inputJ) > result[i][j]) {
          result[i][j] = CCmetric.getValue(inputI, inputJ);

        }
        if (result[i][j] > maxCorrVal) {
          maxCorrVal = result[i][j];
        }


      }
    }
    return result;
  }

  @Override
  public String getWorkerName() {
    return "MSER Correlator";
  }

  @Override
  public double[][] doInBackground() {

    CCOGParams pc = new CCOGParams(CCOGParams.values.def);
    
    CLoGParams pl = new CLoGParams();
    CInterestingPoints extractor;
    CHarrisParams ph = new CHarrisParams();
    extractor = new CInterestingPoints(inputA, CInterestingPoints.Cornerer.harris, CInterestingPoints.Edger.LOG, ph, pl, 0, 0);
    logger.info("getting pts from A");
    this.ptsA = extractor.getPoints(inputA);

    extractor.input = inputB;
    logger.info("getting pts from B");
    this.ptsB = extractor.getPoints(inputB);
    logger.info("getting correlations B");
    double[][] corrs = this.getCorrs(this.ptsA, this.ptsB);
    return corrs;
    /*
     * int[] originalIdx = new int[ptsB.size()]; double[] maxSoFar = new
     * double[ptsB.size()];
     *
     * for (int i = 0; i < ptsA.size(); i++) { double maxCorr = 0.0; int maxIdx
     * = -1; for (int j = 0; j < ptsB.size(); j++) { if (corrs[i][j] > maxCorr)
     * { maxCorr = corrs[i][j]; maxIdx = j; } } if (maxCorr > 0.95 && maxCorr >
     * maxSoFar[maxIdx]) { maxSoFar[maxIdx] = maxCorr; originalIdx[maxIdx] = i;
     *
     *
     * }
     * }
     * LinkedList<Point2D.Double> originals = new LinkedList<Point2D.Double>();
     * LinkedList<Point2D.Double> sensed = new LinkedList<Point2D.Double>();
     * CPerspectiveTransformation trans = new CPerspectiveTransformation(); for
     * (int j = 0; j < ptsB.size(); j++) { int ct = 0; if (maxSoFar[j] > 0.0) {
     * ct++;
     * System.out.println(ptsA.getPoint(originalIdx[j]).x+","+ptsA.getPoint(originalIdx[j]).y+
     * "; "+ptsB.getPoint(j).x+","+ptsB.getPoint(j).y +": " + maxSoFar[j]);
     * originals.push(ptsA.getPoint(originalIdx[j]));
     * sensed.push(ptsB.getPoint(j)); } if (ct == 4) break; }
     * trans.set(originals, sensed); trans.print(); BufferedImage output = new
     * BufferedImage(inputA.getWidth(), inputA.getHeight(),
     * BufferedImage.TYPE_INT_RGB); WritableRaster inRaster =
     * inputB.getRaster(); WritableRaster outRaster = output.getRaster(); int[]
     * pixel = new int[3]; int[] black = {0,0,0}; for (int x = 0; x <
     * inputA.getWidth(); x++) { for (int y = 0; y < inputA.getHeight(); y++) {
     * Point2D.Double ref = trans.getProjected(new Point2D.Double(x, y)); if
     * (ref.x < inputB.getWidth() && ref.x >= 0 && ref.y < inputB.getHeight() &&
     * ref.y >= 0) { inRaster.getPixel((int) ref.x, (int) ref.y, pixel); } else
     * { pixel = black; } outRaster.setPixel(x, y, pixel); } }
     * output.setData(outRaster);
     *
     * return output;
     */
  }

  public double[][] publicRun() {
    return doInBackground();
  }

  @Override
  public String getTypeName() {
    return "REGISTRATION";
  }

  @Override
  public Type getType() {
    return Type.REGISTRATION;
  }
}
