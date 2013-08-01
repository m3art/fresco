
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
public class CRansacRegister extends CAnalysisWorker{
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
    
    
        
  
  public CRansacRegister(BufferedImage reference, BufferedImage sensed) {
    inputA = reference;
    inputB = sensed;
    CCOGParams pc = new CCOGParams();
    pc.centerWhitenessW = 0.13;
    pc.distW = 0.05;
    pc.perpCW = 0.4;
    pc.whiteDiffW = 0.2;
    pc.mixW = 0.21;
    pc.normalizeWeights();
    pc.windowSize = 23;
    CLoGParams pl = new CLoGParams();
    extractor = new CInterestingPoints(reference, sensed, Cornerer.COG, Edger.LOG, pc, pl, 0,0);
    
    logger.info("getting pts from A");
    ptsA = extractor.getPoints(inputA);
    logger.info("getting pts from B");
    ptsB = extractor.getPoints(inputB);
    correlations = null;
    refInitIdx = new int[4];
    sensedInitIdx = new int[4];
    refInitIdx[0] = refInitIdx[1] = refInitIdx[2] = refInitIdx[3] = -1;
    sensedInitIdx[0] = sensedInitIdx[1] = sensedInitIdx[2] = sensedInitIdx[3] = -1;
    refInit = new Point2D.Double[4];
    sensedInit = new Point2D.Double[4];
    iters = 1500;
    inlierThresh = (inputA.getHeight()+inputB.getWidth())/40;
    CSThresh = ptsA.size() < ptsB.size() ? ptsA.size()/2 : ptsB.size()/2;
    
  }
  
    
  
  public static int[] getMserPlus(int[] surround, int w, int h) {
    
    int[] MSERtmp = new int[w*h];
    
    boolean[] isBoundary = new boolean[w*h];
    LinkedList<Point2D.Double> boundary = new LinkedList<>();
    LinkedList<Point2D.Double> nextBoundary = new LinkedList<>();
    for (int i = 0; i < w*h; i++) {
      MSERtmp[i] = -1;
    }
    boundary.addLast(new Point2D.Double((int)(w/2), (int)(h/2)));
    int centerPx = (int)surround[((h/2) * w) + (w/2)];
    MSERtmp[(h/2)*w  + (w/2)] = centerPx;
    int[] sizes = new int[centerPx+1];
    int testPx = 0;
    sizes[0] = 1;
    int lastSize = 1;
    boolean touch = false;
    for (int intensity = centerPx; intensity >= 0; intensity--) {
     int i =0;
     while(boundary.size() > 0) {
        i++;
        Point2D.Double current = boundary.removeFirst();
        int currentPx = (int)surround[(int)current.y * w + (int)current.x];   
        int curindex = (int)current.y * w + (int)current.x;
        isBoundary[curindex] = false;
        if (currentPx < intensity) {
          nextBoundary.addLast(current);
        }
        else {
          if (current.x == 0 || current.y == 0 || current.x == w-1 || current.y == h-1) {
            touch = true;
          }
          if (MSERtmp[(int)current.y * w + (int)current.x] == -1) {
            lastSize++;
         }
          MSERtmp[(int)current.y * w + (int)current.x] = intensity;
          if (current.x > 0) {
            int idx = (int)current.y * w + (int)current.x-1;
            testPx = (int)surround[idx];
            if (intensity <= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x-1, current.y));
            }
            if (intensity > testPx) nextBoundary.addLast(new Point2D.Double(current.x-1, current.y));
          }
          if (current.x < w-1) {
            int idx = (int)current.y * w + (int)current.x+1;
            testPx = (int)surround[idx];
            if (intensity <= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x+1, current.y));
            }
            if (intensity > testPx) nextBoundary.addLast(new Point2D.Double(current.x+1, current.y));
          }
          if (current.y > 0) {
            int idx = (int)(current.y-1) * w + (int)current.x;
            testPx = (int)surround[idx];
            if (intensity <= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x, current.y-1));
            }
            if (intensity > testPx) nextBoundary.addLast(new Point2D.Double(current.x, current.y-1));
          }
          if (current.y < h-1) {
            int idx = (int)(current.y+1) * w + (int)current.x;
            testPx = (int)surround[idx];
            if (intensity <= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x, current.y+1));
            }
            if (intensity > testPx) nextBoundary.addLast(new Point2D.Double(current.x, current.y+1));
          }
        }
      }
      
      while(nextBoundary.size() > 0) {
        boundary.addLast(nextBoundary.removeFirst());
      }
      if (!touch) sizes[intensity] = lastSize;
      else sizes[intensity] = 0;
    }
    
    int maxStableSize = centerPx/2;
    double minSizeChange = w*h;
    for (int i = MSERdelta; i < centerPx-MSERdelta; i++) {
      //Do not allow too small regions - one pixel is not an mser
      if (sizes[i] < 9) {
        continue;
       }
      double sizeChange = Math.abs(sizes[i-MSERdelta] - sizes[i+MSERdelta])/(double)sizes[i];
      if (sizeChange < minSizeChange) {
        minSizeChange = sizeChange;
        maxStableSize = i;
      }
      
    }
    if (minSizeChange == w*h) {
      throw new java.lang.RuntimeException("no mser found");
    }
    
    for(int i = 0; i < w*h; i++) {
      if(MSERtmp[i] >= maxStableSize) MSERtmp[i] = 1;
      else MSERtmp[i] = 0;
    }
    
    return MSERtmp;
  
  }
  public static int[] getMserMinus(int[] surround, int w, int h) {
    
    int[] MSERtmp = new int[w*h];
    boolean[] isBoundary = new boolean[w*h];

    LinkedList<Point2D.Double> boundary = new LinkedList<>();
    LinkedList<Point2D.Double> nextBoundary = new LinkedList<>();
    for (int i = 0; i < w*h; i++) {
      MSERtmp[i] = -1;
    }
    boundary.addLast(new Point2D.Double((int)(w/2), (int)(h/2)));
    int centerPx = (int)surround[((h/2) * w) + (w/2)];
    MSERtmp[(h/2)*w  + (w/2)] = centerPx;
    
    int[] sizes = new int[256];
    int testPx = 0;
    
    sizes[0] = 1;
    int lastSize = 1;
    boolean touch = false;
    for (int intensity = centerPx; intensity <= 255; intensity++) {
     
     while(boundary.size() > 0) {
        
        Point2D.Double current = boundary.removeFirst();
        int currentPx = (int)surround[(int)current.y * w + (int)current.x];   
        int curindex = (int)current.y * w + (int)current.x;
        isBoundary[curindex] = false;
        if (currentPx > intensity) {
          nextBoundary.addLast(current);
        }
        
        else {
          if (current.x == 0 || current.y == 0 || current.x == w-1 || current.y == h-1) {
            touch = true;
          }
          if (MSERtmp[(int)current.y * w + (int)current.x] == -1) {
            lastSize++;
          }
          MSERtmp[(int)current.y * w + (int)current.x] = intensity;
          
          if (current.x > 0) {
            int idx = (int)current.y * w + (int)current.x-1;
            testPx = (int)surround[idx];
            if (intensity >= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x-1, current.y));
            }
            if (intensity < testPx) nextBoundary.addLast(new Point2D.Double(current.x-1, current.y));
          }
          if (current.x < w-1) {
            int idx = (int)current.y * w + (int)current.x+1;
            testPx = (int)surround[idx];
            if (intensity >= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x+1, current.y));
            }
            if (intensity < testPx) nextBoundary.addLast(new Point2D.Double(current.x+1, current.y));
          }
          if (current.y > 0) {
            int idx = (int)(current.y-1) * w + (int)current.x;
            testPx = (int)surround[idx];
            if (intensity >= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x, current.y-1));
            }
            if (intensity < testPx) nextBoundary.addLast(new Point2D.Double(current.x, current.y-1));
          }
          if (current.y < h-1) {
            int idx = (int)(current.y+1) * w + (int)current.x;
            testPx = (int)surround[idx];
            if (intensity >= testPx && MSERtmp[idx] == -1 && !isBoundary[idx]) {
              isBoundary[idx] = true;
              boundary.addLast(new Point2D.Double(current.x, current.y+1));
            }
            if (intensity < testPx) nextBoundary.addLast(new Point2D.Double(current.x, current.y+1));
          }
        }
      }
      
      while(nextBoundary.size() > 0) {
        boundary.addLast(nextBoundary.removeFirst());
        
        
      }
      if (!touch) sizes[intensity] = lastSize;
      else sizes[intensity] = 0;
    }
    int maxStableSize = centerPx/2;
    double minSizeChange = w*h;
    for (int i = centerPx+MSERdelta; i < 255-MSERdelta; i++) {
      //Do not allow too small regions - one pixel is not a mser
      if (sizes[i] < 9) {
        continue;
      }
      double sizeChange = Math.abs(sizes[i+MSERdelta] - sizes[i-MSERdelta])/(double)sizes[i];
      if (sizeChange < minSizeChange) {
        minSizeChange = sizeChange;
        maxStableSize = i;
      }
      
      
    }
    //nothing of value found
    if (minSizeChange == w*h) {
     //logger.info("mser problem");
     /*logger.info("sizes: ");
     int prevsize = 0;
     for (int i = 0; i < 255; i++) {
       if (sizes[i] != prevsize) logger.info(sizes[i] + " at "  + i);
       prevsize = sizes[i];
     }*/
     throw new java.lang.RuntimeException("no mser found");
    }
    //logger.info("Mser size: " + maxStableSize);
    for(int i = 0; i < w*h; i++) {
      if(MSERtmp[i] <= maxStableSize) MSERtmp[i] = 1;
      else MSERtmp[i] = 0;
    }
    
    return MSERtmp;
  
  }
  
  public static Point2D.Double getCOG(int[] mat, int w, int h){
    double norm = 0;
    Point2D.Double COG = new Point2D.Double();
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        COG.x += mat[y1*w+x1] * x1;
        COG.y += mat[y1*w+x1] * y1;
        norm += mat[y1*w+x1];
      }
    }
    COG.x /= norm;
    COG.y /= norm;
    return COG;
  
  }
  
  public static double[][] getCovMatrix(int[] mat, int w, int h) {
    double[][] cov = new double[2][2];
    double norm = 0;
    int sizeW =w/2;
    int sizeH =h/2;
    Point2D.Double COG = getCOG(mat, w, h);
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        cov[0][0] += (x1-COG.x) * (x1-COG.x)*mat[y1*w+x1];
        cov[0][1] += (x1-COG.x) * (y1-COG.y)*mat[y1*w+x1];
        cov[1][0] += (y1-COG.y) * (x1-COG.x)*mat[y1*w+x1];
        cov[1][1] += (y1-COG.y) * (y1-COG.y)*mat[y1*w+x1];
        norm+=mat[y1*w+x1];
        
      }
     
    }
    double normX = (1.0/3.0)*(sizeW)*(sizeW + 1.0)*(2*sizeW+1.0) * h;
    double normY = (1.0/3.0)*(sizeH)*(sizeH + 1.0)*(2*sizeH+1.0) * w;
    
    
    double normXY = (w/2)*(w/2+1) * (h/2)*(h/2+1);
    cov[0][0] /= normX;
    cov[0][1] /= normXY;
    cov[1][0] /= normXY;
    cov[1][1] /= normY;
    
    
    return cov;
  }
  
  public static double bilinearInterpolate(double[][] source, double x, double y, int w, int h) {
    double ret = 0.0;
    if (x >= w-1 || x < 0 || y >= h-1 || y < 0) return ret;
    Point2D.Double topLeft = new Point2D.Double(Math.floor(x), Math.floor(y));
    double left = (source[(int)topLeft.y][(int)topLeft.x] * (1-y+topLeft.y)) 
                 + source[(int)topLeft.y+1][(int)topLeft.x] * (y-topLeft.y) ;
    double right = (source[(int)topLeft.y][(int)topLeft.x+1] * (1-y+topLeft.y)) 
                 + source[(int)topLeft.y+1][(int)topLeft.x+1] * (y-topLeft.y) ;
    ret = (x - topLeft.x) * right + (1 - x + topLeft.x ) * left;
    
    return ret;
  }
  
  public static double[][] getNormalizedRegion(int[] region, int[] mser, int w, int h) {
    double cov[][] = getCovMatrix(mser, w, h);
    int centerX = w/2;
    int centerY = h/2;
            
    Matrix mCov = new Matrix(cov);
    Matrix mVectors = mCov.eig().getV();
    Matrix mValues = mCov.eig().getD();
    Point2D.Double COG = getCOG(mser, w, h);
    Matrix mCOG = new Matrix(2, 1);
    mCOG.set(0, 0, COG.x);
    mCOG.set(1, 0, COG.y);
    mValues.set(0, 0, Math.sqrt(mValues.get(0, 0)) );
    mValues.set(1, 1, Math.sqrt(mValues.get(1, 1)) );
    Matrix normToRegion = mVectors.times(mValues);
    //logger.info("nrm: starting calculation");
    double[][] origMser = new double[h][w];
      for (int i = 0; i < w*h; i++) {
        origMser[i/w][i%w] = mser[i]*region[i];
      }
            
      //logger.info("nrm: got origMser");
      double[][] normalized = new double[h][w];
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++){
          double[][] pt = new double[2][1];
          pt[0][0] = (x-centerX); 
          pt[1][0] = (y-centerY);
          Matrix X = new Matrix(pt);
          Matrix origCoords = normToRegion.times(X);
          origCoords.plusEquals(mCOG);
          //logger.info("row " + y + " col " + x + " ok?");
          normalized[y][x] = bilinearInterpolate(origMser, origCoords.get(0, 0), origCoords.get(1, 0), w, h);
          //logger.info("row " + y + " col " + x + " ok");
        }
        
      }
      //logger.info("returning");
    return normalized;
  }
  
  public static int getMaxChannel(double[][] region, int[] mser, int w, int h, int channelCount, int maxGrad) {
    
    int[] channelHist = new int[channelCount];
    for (int i = 0; i < channelCount; i++) channelHist[i] = 0;
    for (int x = 1; x < w-1; x++) {
      for (int y = 1; y < h-1; y++) {
        if (mser[y*h+x+1] == 0 || mser[y*h+x-1] == 0 || mser[(y+1)*h+x] == 0 || mser[(y-1)*h+x] == 0) continue;
        double gradX = (region[y][x+1] - region[y][x-1])/maxGrad;
        double gradY = (region[y+1][x] - region[y-1][x])/maxGrad;
        double gradSize = Math.sqrt(gradX*gradX + gradY*gradY);
        if (gradSize == 0) continue;
        //double rotA = Math.asin(gradY/gradSize);
        double rot = Math.acos(gradX/gradSize);
        //if (gradY < 0) rot = 2*Math.PI - rot;
        rot /= Math.PI;
        if (rot == 1) rot = 0; //safeguard to not exceed channelCount, rot by 2*Pi (~rot==1) is equiv to no rot(~rot==0).
        channelHist[(int)(rot*(double)channelCount)]++;
      }
    }
    int maxChannel = -1;
    int maxVal = 0;
    int cum = 0;
    for (int i = 0; i < channelCount; i++) {
       //System.out.print(i+":"+channelHist[i] + ".");
       cum+=channelHist[i];
      if (channelHist[i] > maxVal) {
       
        maxVal = channelHist[i];
        maxChannel = i;
      }
    }
       //System.out.println(cum);
    return maxChannel;
  }
  
  public static double[] rotateByChannel(double[][] region, int channel, int channelCount, int w, int h) {
    double[] rotated = new double[w*h];
    // this is the angle by which we suspect the region is rotated from the norm position
    //therefore, we look for the pixel required for the rotated region at a position offset by this rotation in the original region
    double theta = -1*(double)channel/channelCount * Math.PI;
    //System.out.println("rot by " + theta);
    int centerX = w/2;
    int centerY = h/2;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        
        double origX = Math.cos(theta)*(x-centerX) - Math.sin(theta)*(y-centerY);
        double origY = Math.sin(theta)*(x-centerX) + Math.cos(theta)*(y-centerY);
        rotated[y*w+x] = bilinearInterpolate(region, origX+centerX, origY+centerY, w, h);        
      }
    }
    
    return rotated;
  }
  
  
  
  private void getCorrs() {
  correlations = new double[ptsA.size()][ptsB.size()];
    
    Raster rasterA = (new Crgb2grey()).convert(inputA).getData();
    
    Raster rasterB = (new Crgb2grey()).convert(inputB).getData();
    //int corrWindowSize = 7;
    int dim = 51;
    CCovarianceMetric CV = new CCovarianceMetric(inputA, inputB, dim, CAreaSimilarityMetric.Shape.RECTANGULAR);
    CCrossCorrelationMetric CC = new CCrossCorrelationMetric(inputA, inputB, dim, CAreaSimilarityMetric.Shape.RECTANGULAR);
    LinkedList<double[]> APlus = new LinkedList<>();
    LinkedList<double[]> AMinus = new LinkedList<>();
    LinkedList<double[]> BPlus = new LinkedList<>();
    LinkedList<double[]> BMinus = new LinkedList<>();
    LinkedList<Integer> APlusIdx = new LinkedList<>();
    LinkedList<Integer> AMinusIdx = new LinkedList<>();
    LinkedList<Integer> BPlusIdx = new LinkedList<>();
    LinkedList<Integer> BMinusIdx = new LinkedList<>();
    for (int i = 0; i < ptsA.size() ; i++) {
      setProgress(50 * i / ptsA.size());
      Point2D.Double a = ptsA.getPoint(i);
      int AX = (int)a.x;
      int AY = (int)a.y;
      boolean AisEdge = false;
      if ((AX < (dim/2)) || (AX >= rasterA.getWidth()-(dim/2)) || (AY < (dim/2)) || (AY >= rasterA.getHeight()-(dim/2))) {
        AisEdge = true;
      }
      else {
        int currentA[] = null;
        currentA = rasterA.getSamples(AX-dim/2, AY-dim/2, dim, dim, 0, currentA);
        boolean gotAPlus = false;
        boolean gotAMinus = false;
        int[] mserAPlus = null;
        int[] mserAMinus = null;
        double[] rotatedAPlus = null;
        double[] rotatedAMinus = null;
        
        try {
          mserAPlus = getMserPlus(currentA, dim, dim);
          double[][] normalizedAPlus = getNormalizedRegion(currentA, mserAPlus, dim, dim);
          int APlusMaxChannel = getMaxChannel(normalizedAPlus, mserAPlus, dim, dim, channelCount, 255);
          rotatedAPlus = rotateByChannel(normalizedAPlus, APlusMaxChannel, channelCount, dim, dim);
          gotAPlus = true;
        }
        catch(java.lang.RuntimeException e) {
          //logger.info("apt: " + i + ": plus: " + e.toString());
        }
        try {
          mserAMinus = getMserMinus(currentA, dim, dim);
          double[][] normalizedAMinus = getNormalizedRegion(currentA, mserAMinus, dim, dim);
          int AMinusMaxChannel = getMaxChannel(normalizedAMinus, mserAMinus, dim, dim, channelCount, 255);
          rotatedAMinus = rotateByChannel(normalizedAMinus, AMinusMaxChannel, channelCount, dim, dim);
          gotAMinus = true;
        }
        catch(java.lang.RuntimeException e) {
          //logger.info("apt: " + i + ": minus" + e.toString());
        }
        if (!gotAMinus && !gotAPlus) {
          logger.info("apt " + i + " has no mser of value");
        }
        int[] green = new int[3];
        green[0] = 0;
        green[1] = 255;
        green[2] = 0;
        int[] black = new int[3];
        black[0] = 0;
        black[1] = 0;
        black[2] = 0;
        
        if (gotAPlus) {
          APlus.add(rotatedAPlus);
          APlusIdx.add(i);
          for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
              if (mserAPlus[y*dim+x] == 1) {
                inputA.getRaster().setPixel((int)ptsA.getPoint(i).x+x-dim/2, (int)ptsA.getPoint(i).y+y-dim/2, green);
              }
              if (x == 0 || x == dim-1 || y == 0 || y == dim-1) {
                inputA.getRaster().setPixel((int)ptsA.getPoint(i).x+x-dim/2, (int)ptsA.getPoint(i).y+y-dim/2, black);
              }
            }
          }
        }
        int[] red = new int[3];
        red[0] = 255;
        red[1] = 0;
        red[2] = 0;
        if (gotAMinus) {
          AMinus.add(rotatedAMinus);
          AMinusIdx.add(i);
          for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
              if (mserAMinus[y*dim+x] == 1) {
                inputA.getRaster().setPixel((int)ptsA.getPoint(i).x+x-dim/2, (int)ptsA.getPoint(i).y+y-dim/2, red);
              }
              if (x == 0 || x == dim-1 || y == 0 || y == dim-1) {
                inputA.getRaster().setPixel((int)ptsA.getPoint(i).x+x-dim/2, (int)ptsA.getPoint(i).y+y-dim/2, black);
              }
            }
          }
          
        }
      }
    }
    if (ptsB.size() ==  0) return;
    for (int i = 0; i < ptsB.size() ; i++) {
      setProgress(50 * i / ptsB.size() + 50);
      boolean BisEdge = false;
      Point2D.Double b = ptsB.getPoint(i);
      int BX = (int)b.x;
      int BY = (int)b.y;
      if ((BX < (dim/2)) || (BX >= rasterA.getWidth()-(dim/2)) || (BY < (dim/2)) || (BY >= rasterA.getHeight()-(dim/2))) {
        BisEdge = true;
      }

      if (!BisEdge) {
        int[] currentB = null;
        currentB = rasterB.getSamples(BX-dim/2, BY-dim/2, dim, dim, 0, currentB);
        boolean gotBPlus = false;
        boolean gotBMinus = false;
        int[] mserBPlus = null;
        int[] mserBMinus = null;
        double[] rotatedBPlus = null;
        double[] rotatedBMinus = null;

        try {
          mserBPlus = getMserPlus(currentB, dim, dim);
          double[][] normalizedBPlus = getNormalizedRegion(currentB, mserBPlus, dim, dim);
          int BPlusMaxChannel = getMaxChannel(normalizedBPlus, mserBPlus, dim, dim, channelCount, 255);
          rotatedBPlus = rotateByChannel(normalizedBPlus, BPlusMaxChannel, channelCount, dim, dim);
          gotBPlus = true;
        }
        catch(java.lang.RuntimeException e) {
          //logger.info("bpt: " + i + ": plus: " + e.toString());
        }
        try {
          mserBMinus = getMserMinus(currentB, dim, dim);
          double[][] normalizedBMinus = getNormalizedRegion(currentB, mserBMinus, dim, dim);
          int BMinusMaxChannel = getMaxChannel(normalizedBMinus, mserBMinus, dim, dim, channelCount, 255);
          rotatedBMinus = rotateByChannel(normalizedBMinus, BMinusMaxChannel, channelCount, dim, dim);
          gotBMinus = true;
        }
        catch(java.lang.RuntimeException e) {
          //logger.info("bpt: " + i + ": minus " + e.toString());
        }

        if (!gotBMinus && !gotBPlus) {
          logger.info("bpt " + i + " has no mser of value");
        }
        int[] green = new int[3];
        green[0] = 0;
        green[1] = 255;
        green[2] = 0;
        int[] black = new int[3];
        black[0] = 0;
        black[1] = 0;
        black[2] = 0;
        
        if (gotBPlus) {
          BPlus.add(rotatedBPlus);
          BPlusIdx.add(i);
          for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
              if (mserBPlus[y*dim+x] == 1) {
                inputB.getRaster().setPixel((int)ptsB.getPoint(i).x+x-dim/2, (int)ptsB.getPoint(i).y+y-dim/2, green);
              }
              if (x == 0 || x == dim-1 || y == 0 || y == dim-1) {
                inputB.getRaster().setPixel((int)ptsB.getPoint(i).x+x-dim/2, (int)ptsB.getPoint(i).y+y-dim/2, black);
              }
            }
          }
          
          
        }
        int[] red = new int[3];
        red[0] = 255;
        red[1] = 0;
        red[2] = 0;
        
        if (gotBMinus) {
          BMinus.add(rotatedBMinus);
          BMinusIdx.add(i);
          for (int x = 0; x < dim; x++) {
            for (int y = 0; y < dim; y++) {
              if (mserBMinus[y*dim+x] == 1) {
                inputB.getRaster().setPixel((int)ptsB.getPoint(i).x+x-dim/2, (int)ptsB.getPoint(i).y+y-dim/2, red);
              }
              if (x == 0 || x == dim-1 || y == 0 || y == dim-1) {
                inputB.getRaster().setPixel((int)ptsB.getPoint(i).x+x-dim/2, (int)ptsB.getPoint(i).y+y-dim/2, black);
              }
              
            }
          }
          
        }
       }
     }
    
    for(int i = 0; i < ptsA.size(); i++ ) {
      for(int j = 0; j < ptsB.size(); j++ ) {
        correlations[i][j] = 0.0;
      }
    }
    logger.info("corr sizes:");
    logger.info("APlus: " + APlus.size() + " AMinus: " + AMinus.size());
    logger.info("BPlus: " + BPlus.size() + " BMinus: " + BMinus.size());
    
    CSThresh = (Math.min(APlus.size(), BPlus.size()) + Math.min(AMinus.size(), BMinus.size())) / 2 ;
    logger.info("CSThresh: " + CSThresh);
    double maxCorrVal = 0.0;
    
    while (APlusIdx.size() > 0) {
      int i = APlusIdx.removeFirst();
      double[] inputI = APlus.removeFirst();
      for (int y = 0; y < BPlus.size(); y++) {
        int j = BPlusIdx.get(y);
        double[] inputJ = BPlus.get(y);
        correlations[i][j] = CC.getValue(inputI, inputJ);
      }
    }
    while (AMinusIdx.size() > 0) {
      int i = AMinusIdx.removeFirst();
      double[] inputI = AMinus.removeFirst();
      for (int y = 0; y < BMinus.size(); y++) {
        int j = BMinusIdx.get(y);
        double[] inputJ = BMinus.get(y);
        if (CC.getValue(inputI, inputJ) > correlations[i][j])  correlations[i][j] = CC.getValue(inputI, inputJ) ;
        if (correlations[i][j] > maxCorrVal) maxCorrVal = correlations[i][j];
        
        
      }
    }
    logger.info("max corr val: " + maxCorrVal+"");
    //logger.info("correlations filled in");
  };
  
  private boolean getTopParams() {
    //logger.info("getting top params");
    LinkedList<Point2D.Double> inliersRefTop = new LinkedList<Point2D.Double>();
    LinkedList<Point2D.Double> inliersSensedTop = new LinkedList<Point2D.Double>();            
    for (int p = 0; p < 4; p++) {
          
      double maxCor = 0.0;
      int maxA = -1;
      int maxB = -1;
      for (int i = 0; i < ptsA.size(); i++) {
        if (i == refTopIdx[0] || i == refTopIdx[1] || i == refTopIdx[2] || i == refTopIdx[3]) continue;
        for (int j = 0; j < ptsB.size(); j++) {
          if (j == sensedTopIdx[0] || j == sensedTopIdx[1] || j == sensedTopIdx[2] || j == sensedTopIdx[3]) continue;
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
      
      } catch(Exception e) {
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
          if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) continue;
        if (ptsA.getPoint(i).y > maxT) {
          maxT = ptsA.getPoint(i).y; 
          maxTI = i;
        };
      }
      logger.info("got ref 1 - T ");
      refInitIdx[0] = maxTI;
      refInit[0] = ptsA.getPoint(maxTI);
      for (int i = 0; i < ptsA.size(); i++) {
          if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) continue;
        if (ptsA.getPoint(i).y < maxB) {
          maxB = ptsA.getPoint(i).y; 
          maxBI = i;
        };
      }
      refInitIdx[1] = maxBI;
      refInit[1] = ptsA.getPoint(maxBI);
      for (int i = 0; i < ptsA.size(); i++) {
          if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) continue;
        if (ptsA.getPoint(i).x < maxL) {
          maxL = ptsA.getPoint(i).x; 
          maxLI = i;
        };
      }
      refInitIdx[2] = maxLI;
      refInit[2] = ptsA.getPoint(maxLI);
      for (int i = 0; i < ptsA.size(); i++) {
          if (i == refInitIdx[0] || i == refInitIdx[1] || i == refInitIdx[2] || i == refInitIdx[3]) continue;
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
          if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) continue;
        if (ptsB.getPoint(i).y > maxT) {
          maxT = ptsB.getPoint(i).y; 
          maxTI = i;
        };
      }
      sensedInitIdx[0] = maxTI;
      sensedInit[0] = ptsB.getPoint(maxTI);
      for (int i = 0; i < ptsB.size(); i++) {
          if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) continue;
        if (ptsB.getPoint(i).y < maxB) {
          maxB = ptsB.getPoint(i).y; 
          maxBI = i;
        };
      }
      sensedInitIdx[1] = maxBI;
      sensedInit[1] = ptsB.getPoint(maxBI);
      for (int i = 0; i < ptsB.size(); i++) {
          if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) continue;
        if (ptsB.getPoint(i).x < maxL) {
          maxL = ptsB.getPoint(i).x; 
          maxLI = i;
        };
      }
      sensedInitIdx[2] = maxLI;
      sensedInit[2] = ptsB.getPoint(maxLI);
      for (int i = 0; i < ptsB.size(); i++) {
          if (i == sensedInitIdx[0] || i == sensedInitIdx[1] || i == sensedInitIdx[2] || i == sensedInitIdx[3]) continue;
        if (ptsB.getPoint(i).x > maxR) {
          maxR = ptsB.getPoint(i).x; 
          maxRI = i;
        };
      }
      sensedInitIdx[3] = maxRI;
      sensedInit[3] = ptsB.getPoint(maxRI);
    
      

    logger.info("ref positions: " + refInit[0].toString() + refInit[1].toString() + refInit[2].toString() + refInit[3].toString()) ;
    logger.info("sensed positions: " + sensedInit[0].toString() + sensedInit[1].toString() + sensedInit[2].toString() + sensedInit[3].toString()) ;
            
       
    
    logger.info("got initial transform");
    return true;
  
  };
  
  private double getInliers(CPerspectiveTransformation trans) {
    double inlierCount = 0;
      boolean[] sensedUsed = new boolean[ptsB.size()];
      for (int i = 0; i < ptsA.size(); i++) {
        Point2D.Double refPt = ptsA.getPoint(i);
        for (int j = 0; j < ptsB.size(); j++) {
            if (sensedUsed[j]) continue;
          Point2D.Double sensedPt = ptsB.getPoint(j);
          Point2D.Double transformedRefPt = trans.getProjected(refPt);
          double dist = Math.sqrt(   (sensedPt.x-transformedRefPt.x)*(sensedPt.x-transformedRefPt.x)
                                    + (sensedPt.y-transformedRefPt.y)*(sensedPt.y-transformedRefPt.y));
          if (dist < inlierThresh && correlations[i][j] > 0.0) {
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
  
  private boolean getRandomParams( double corrThresh) {
    if ((ptsA.size() < 4) || (ptsB.size() < 4)) return false;
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
        
        
        indexA = (int)((double)ptsA.size()*Math.random());
        while (indexA == refInitIdx[0] || indexA == refInitIdx[1] || indexA == refInitIdx[2] || indexA == refInitIdx[3] ) {
          indexA = (int)((double)ptsA.size()*Math.random());
        }
        indexB = (int)((double)ptsB.size()*Math.random());;
        while (indexB == sensedInitIdx[0] || indexB == sensedInitIdx[1] || indexB == sensedInitIdx[2] || indexB == sensedInitIdx[3] ) {
        indexB = (int)((double)ptsB.size()*Math.random());
        if (indexA == ptsA.size()) indexA--;
        if (indexB == ptsB.size()) indexA--;
        if (maxCorr < correlations[indexA][indexB]) {
          maxA = indexA;
          maxB = indexB;
          maxCorr = correlations[indexA][indexB];
        }
        
        }
      }
      while ((maxCorr < corrThresh) && (tries < maxTries));
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
    
    getCorrs();
    boolean enoughPts = getInitParams();
    if (!(enoughPts)) return inputA;
    
    
    CPerspectiveTransformation trans = new CPerspectiveTransformation();
    
     
    
    trans.set(refInit, sensedInit);
    trans.print();
    
    double inlierCountInit = getInliers(trans);
    
    
    enoughPts = getTopParams();
    if (!(enoughPts)) topTrans.copy(trans);
    topTrans.print();
    
    
    double inlierCountTop = getInliers(topTrans);
    logger.info(inlierCountTop+"");
    if (inlierCountTop > inlierCountInit) {
      trans.copy(topTrans);
      logger.info("replacing universal transformation with top transformation");
      
    }
    
    Point size = new Point(inputA.getWidth(), inputA.getHeight());
    
    BufferedImage output = new BufferedImage(size.x, size.y, inputB.getType());
    WritableRaster outRaster = output.getRaster();
    Raster in = inputB.getData();
    
    maxTrans.copy(trans);
    double maxInliers = 0.0;
    logger.info("total pts: refPts: " + ptsA.size() + " sensedPts: " + ptsB.size() + " total: " + ptsA.size()*ptsB.size() );
    CPerspectiveTransformation firstTrans = new CPerspectiveTransformation();
    //CSThresh = ptsA.size() < ptsB.size() ? ptsA.size()/2 : ptsB.size()/2;
    for (int iter = 0; iter < iters; iter++) {
      inliersRef.clear();
      inliersSensed.clear();
      inliersRefMSS.clear();
      inliersSensedMSS.clear();
      double inlierCount = getInliers(trans);
      if (inlierCount > maxInliers) {
          maxTrans.copy(trans);
          maxInliers = inlierCount;
        }
      if (inlierCount >= CSThresh) {
        //logger.info("passed CSThresh of " + CSThresh);
      //  break;
        
      }
      
      double corrThresh = 1-((double)iter/(4*iters)) ;
     getRandomParams(corrThresh);
     boolean succesful = false;
     while(!succesful){
      try{
        trans.set(refInit, sensedInit);
        succesful = true;
      }
      catch(java.lang.RuntimeException e) {
        System.out.println(e.toString());
        getRandomParams(corrThresh);
      }
     }        
     logger.info("iter " + iter + ": inliers found: " + inlierCount + " corrT: " + corrThresh);
      //logger.info("iter " + iter + ": "); 
      //trans.print();
     if ((corrThresh == 1.0) && (inlierCount >  4)) {
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
    
    /*maxTrans.a[0] = 1;
    maxTrans.a[1] = 0;
    maxTrans.a[2] = 10;
    maxTrans.a[3] = 0;
    maxTrans.a[4] = 1;
    maxTrans.a[5] = -67;
    maxTrans.a[6] = 0;
    maxTrans.a[7] = 0;
    maxTrans.a[8] = 1;
    */
    maxInliers = getInliers(maxTrans);
    logger.info("maxInliers: " + maxInliers) ;
    
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
