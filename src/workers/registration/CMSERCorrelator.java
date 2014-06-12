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
import java.util.LinkedList;
import utils.CIdxMapper;
import utils.metrics.CAreaSimilarityMetric;
import utils.metrics.CCrossCorrelationMetric;

/**
 *
 * @author Jakub
 */



public class CMSERCorrelator {
  
  public int MSERLowSizeLimit = 9;
  
  public int MSERdelta = 1;
  public CIdxMapper map;
  private CPointsAndQualities ptsA, ptsB;
  private BufferedImage inputA, inputB;
  public int MSERSearchDim = 51;
  int channelCount = 32;
  int maxGrad = 255;
  
  public CMSERCorrelator(int iMSERdelta, int iMSERLowSizeLimit) {
    this.MSERdelta = iMSERdelta;
    this.MSERLowSizeLimit = iMSERLowSizeLimit;
    this.map = new CIdxMapper();
    
  
  }
  
  //get the Maximally stable region from information about region sizes for different intensities
  public int getMSERIntensity(int[] sizes) {
    int maxStableIntensity = 0;
    double minSizeChange = Integer.MAX_VALUE;
    for (int i = MSERdelta; i < sizes.length-MSERdelta; i++) {
      //Do not allow too small regions - one pixel is not an mser
      if (sizes[i] < MSERLowSizeLimit) {
        continue;
       }
      // Calculate fluctuations in size, record minimal
      double sizeChange = Math.abs(sizes[i-MSERdelta] - sizes[i+MSERdelta])/(double)sizes[i];
      if (sizeChange < minSizeChange) {
        minSizeChange = sizeChange;
        maxStableIntensity = i;
      }
      
    }
    if (minSizeChange == Integer.MAX_VALUE) {
      throw new java.lang.RuntimeException("no mser found");
    }
    return maxStableIntensity;
  }
  
  public void processPoint(
           Point2D.Double input, 
           LinkedList<Point2D.Double> boundary, 
           LinkedList<Point2D.Double> nextBoundary, 
           boolean[] isBoundary, 
           int[] surround,
           int[] MSERtable,
           int thresholdIntensity,
           int surroundWidth) {            
    
    int idx = map.toIdx(input, surroundWidth);
    int testPx = (int)surround[idx];
    if (thresholdIntensity <= testPx && MSERtable[idx] == -1 && !isBoundary[idx]) {
      isBoundary[idx] = true;
      boundary.addLast(input);
    }
    if (thresholdIntensity > testPx) nextBoundary.addLast(input);
  }
   
          
  public int[] getMserPlus(int[] surround, int w, int h) {
    
    //temp variable to record at what intensity was pixel (x, y) added to extremal region
    int[] MSERtable = new int[w*h];
    //indicator variable - to prevent adding pixels to boundary more than once
    boolean[] isBoundary = new boolean[w*h];
    //boundary to be explored for this intensity - essentially a BFS queue
    LinkedList<Point2D.Double> boundary = new LinkedList<>();
    //boundary to be explored for lower intensities
    LinkedList<Point2D.Double> nextBoundary = new LinkedList<>();
     
    //initialize all pixels as "not in any extremal region"
    for (int i = 0; i < w*h; i++) {
      MSERtable[i] = -1;
    }
    //initialize boundary with middle of examined region
    boundary.addLast(new Point2D.Double((int)(w/2), (int)(h/2)));
    int centerPx = (int)surround[((h/2) * w) + (w/2)];
    MSERtable[map.toIdx(h/2, w/2, w)] = centerPx;
    //this var will store a record of what the size of the extremal region is for each intensity
    int[] sizes = new int[centerPx+1];
    //sizes[0] = 1;
    int lastSize = 0;
    boolean touch = false;
    for (int intensity = centerPx; intensity >= 0; intensity--) {
      //conduct BFS for regions connected to center pixel via pixels with high enough intensity
      while(boundary.size() > 0) {
        Point2D.Double current = boundary.removeFirst();
        int currindex = map.toIdx(current, w);
        int currentPx = (int)surround[currindex];   
        isBoundary[currindex] = false;
        if (currentPx < intensity) {
          nextBoundary.addLast(current);
        }
        else {
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
            Point2D.Double leftPx = new Point2D.Double(current.x-1, current.y);
            this.processPoint(leftPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isRightEdge(current, w, h)) {
            Point2D.Double rightPx = new Point2D.Double(current.x+1, current.y);
            this.processPoint(rightPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isTopEdge(current, w, h)) {
            Point2D.Double upPx = new Point2D.Double(current.x, current.y-1);
            this.processPoint(upPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
          if (!map.isBottomEdge(current, w, h)) {
            Point2D.Double downPx = new Point2D.Double(current.x, current.y+1);
            this.processPoint(downPx, boundary, nextBoundary, isBoundary, surround, MSERtable, intensity, w);
          }
        }
      }
      while(nextBoundary.size() > 0) {
        boundary.addLast(nextBoundary.removeFirst());
      }
      if (!touch) sizes[intensity] = lastSize;
      else sizes[intensity] = 0;
    }
    int maxStableIntensity = getMSERIntensity(sizes);    
    for(int i = 0; i < w*h; i++) {
      if(MSERtable[i] >= maxStableIntensity) MSERtable[i] = 1;
      else MSERtable[i] = 0;
    }
    return MSERtable;
  
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
    //int sizeW =w/2;
    //int sizeH =h/2;
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
    //double normX = (1.0/3.0)*(sizeW)*(sizeW + 1.0)*(2*sizeW+1.0) * h;
    //double normY = (1.0/3.0)*(sizeH)*(sizeH + 1.0)*(2*sizeH+1.0) * w;
    //double normXY = (w/2)*(w/2+1) * (h/2)*(h/2+1);
    cov[0][0] /= norm;
    cov[0][1] /= norm;
    cov[1][0] /= norm;
    cov[1][1] /= norm;
    return cov;
  }
  
 //this method should transform the region indicated by the mser table
  //  such that the covariance matrix of its shape is diagonal. 
  public static double[][] getNormalizedRegion(int[] region, int[] mser, int w, int h) {
    
    double cov[][] = getCovMatrix(mser, w, h);
    int centerX = w/2;
    int centerY = h/2;
    
    //hereafter we run in JAMA
    //convert covariance matrix to JAMA format
    Matrix mCov = new Matrix(cov);
    //get eiegenvalues, eigenvecotrs
    Matrix mVectors = mCov.eig().getV();
    Matrix mValues = mCov.eig().getD();
    //get COG, in JAMA format
    Point2D.Double COG = getCOG(mser, w, h);
    Matrix mCOG = new Matrix(2, 1);
    mCOG.set(0, 0, COG.x);
    mCOG.set(1, 0, COG.y);
    mValues.set(0, 0, Math.sqrt(mValues.get(0, 0)) );
    mValues.set(1, 1, Math.sqrt(mValues.get(1, 1)) );
    
    mValues.set(0, 0, mValues.get(0, 0)/(w/5) );
    mValues.set(1, 1, mValues.get(1, 1)/(w/5) );
    
    
    //get transform matrix from normalized to original MSER
    Matrix normToRegion = mVectors.times(mValues);
    System.out.println("NormToRegion: ");
    System.out.println(normToRegion.get(0,0) + ", " + normToRegion.get(0,1) + ", " + normToRegion.get(1,0) + ", " + normToRegion.get(1,1));
    //transform the mser into a 2D table
    //get values of original image instead of true/false
    double[][] origMser = new double[h][w];
    for (int i = 0; i < w*h; i++) {
      origMser[i/w][i%w] = mser[i]*region[i];
    }
    double[][] normalized = new double[h][w];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++){
        //get the current point (x, y) as a JAMA Matrix
        double[][] pt = new double[2][1];
        pt[0][0] = (x-centerX); 
        pt[1][0] = (y-centerY);
        Matrix X = new Matrix(pt);
        //transform the point using the normalized -> original MSER transform matrix
        Matrix origCoords = normToRegion.times(X);
        
        //translate according to the center of gravity
        origCoords.plusEquals(mCOG);
        //System.out.println("interpolating from: " + origCoords.get(0, 0) + ", " + origCoords.get(1,0));
        
        //if not in original rectangle => not in original MSER -> put 0.0 in normalized MSER
        if (origCoords.get(0,0) >= w-1 || origCoords.get(1,0) >= h-1 || origCoords.get(0,0) < 0 || origCoords.get(1,0) < 0) {
          normalized[y][x] = 0.0;
        }
        //otherwise interpolate from original MSER using calculated coords
        else {
          normalized[y][x] = CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), origMser);
        }
      }
    }
    return normalized;
  }
  
  //find out in which direction the gradient is maximal. This direction falls in one of channelCount channels, return this channel
  //public static int getMaxChannel(double[][] region, int[] mser, int w, int h, int channelCount, int maxGrad) {
  public static int getMaxChannel(double[][] region, int w, int h, int channelCount, int maxGrad) {
   
    int[] channelHist = new int[channelCount];
    for (int i = 0; i < channelCount; i++) channelHist[i] = 0;
    for (int x = 1; x < w-1; x++) {
      for (int y = 1; y < h-1; y++) {
        //if (mser[y*h+x+1] == 0 || mser[y*h+x-1] == 0 || mser[(y+1)*h+x] == 0 || mser[(y-1)*h+x] == 0) continue;
        double gradX = (region[y][x+1] - region[y][x-1])/maxGrad;
        double gradY = (region[y+1][x] - region[y-1][x])/maxGrad;
        double gradSize = Math.sqrt(gradX*gradX + gradY*gradY);
        if (gradSize == 0) continue;
        //double rotSin = Math.asin(gradY/gradSize);
        double rot = Math.acos(gradX/gradSize);
        //if (gradY < 0) rot = 2*Math.PI - rot;
        rot /= Math.PI;
        if (rot == 1) rot = 0; //safeguard to not exceed channelCount, rot by 2*Pi (~rot==1) is equiv to no rot(~rot==0).
        if (gradY > 0) {
          rot -= 2.0; 
          rot *= -1.0;
        }
        channelHist[(int)((rot/2.0)*(double)channelCount)]++;
      }
    }
    int maxChannel = -1;
    int maxVal = 0;
    for (int i = 0; i < channelCount; i++) {
      if (channelHist[i] > maxVal) {
        maxVal = channelHist[i];
        maxChannel = i;
      }
    }
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
        if ((origX + centerX) >= w - 1 || (origX + centerX) < 0 || (origY + centerY) >= h - 1 || (origY + centerY) < 0) {
          
          rotated[y*w+x] = 0.0;
        }
        else {
          rotated[y*w+x] = CBilinearInterpolation.getValue(new Point2D.Double(origX+centerX, origY+centerY), region);  
        }
        
      }
    }
    
    return rotated;
  }
  
  private double [] getNormFromRegion(int [] region, int w, int h) {
    int [] mser = getMserPlus(region, w, h);
    double [][] normalized = getNormalizedRegion(region, mser, w, h);
    int maxChannel = getMaxChannel(normalized, w, h, channelCount, maxGrad);
    return rotateByChannel(normalized, maxChannel, channelCount, w, h);
  }
  public double[][] getCorrs() {
    int dim = MSERSearchDim/2;
    double[][] result = new double[ptsA.size()][ptsB.size()];
    Raster rasterA = (new Crgb2grey()).convert(inputA).getData();
    Raster rasterB = (new Crgb2grey()).convert(inputB).getData();
    CCrossCorrelationMetric CCmetric = new CCrossCorrelationMetric(inputA, inputB, MSERSearchDim, CAreaSimilarityMetric.Shape.RECTANGULAR);
    for (int i = 0; i < ptsA.size() ; i++) { 
      Point2D.Double a = ptsA.getPoint(i);
      int AX = (int)a.x;
      int AY = (int)a.y;
      int[] currentA = null;        
      if (!map.isNearEdge(a, rasterA.getWidth(), rasterA.getHeight(), dim)) {
        currentA = rasterA.getSamples(AX-dim, AY-dim, MSERSearchDim, MSERSearchDim, 0, currentA);
        double [] APlus = getNormFromRegion(currentA, dim, dim);
        //invert area
        for (int pos = 0; pos < currentA.length; pos++) {
          currentA[pos] = 255-currentA[pos];
        }
        double [] AMinus = getNormFromRegion(currentA, dim, dim);
      }
    }
    for (int j = 0; j < ptsB.size() ; j++) { 
      Point2D.Double b = ptsB.getPoint(j);
      int BX = (int)b.x;
      int BY = (int)b.y;
      int[] currentB = null;        
      if (!map.isNearEdge(b, rasterB.getWidth(), rasterB.getHeight(), dim)) {
        currentB = rasterB.getSamples(BX-dim, BY-dim, MSERSearchDim, MSERSearchDim, 0, currentB);
        double [] BPlus = getNormFromRegion(currentB, dim, dim);
        //invert area
        for (int pos = 0; pos < currentB.length; pos++) {
          currentB[pos] = 255-currentB[pos];
        }
        double [] BMinus = getNormFromRegion(currentB, dim, dim);
      }
    }
    return result;
  }
  
  
	public String getWorkerName() {
		return "MSER Correlator";
	}

  
  
  
  protected double[][] doInBackground() throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  
}
