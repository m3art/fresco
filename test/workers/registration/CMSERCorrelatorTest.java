/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import Jama.Matrix;
import image.colour.CBilinearInterpolation;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import utils.metrics.CAreaSimilarityMetric;
import utils.metrics.CCrossCorrelationMetric;

/**
 *
 * @author Jakub
 */
public class CMSERCorrelatorTest  {
  
  public CMSERCorrelatorTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Test
  public void testGetMSERIntensity() {
    System.out.println("getMSERIntensity");
    int[] sizes = new int[10];
    for (int i = 0; i < 10; i++) {
      sizes[i] = 10*i;
    }
    sizes[3] = sizes[4] = 20;
    CMSERCorrelator instance = new CMSERCorrelator(1, 9);
    int expResult = 3;
    int result = instance.getMSERIntensity(sizes);
    assertEquals(expResult, result);
    
  }

  @Test
  public void testProcessPoint() {
    System.out.println("addToBoundary");
    
    CMSERCorrelator instance = new CMSERCorrelator(1, 9);
    
    int surroundWidth = 3;
    
    int[] surround = new int[surroundWidth*surroundWidth];
    for (int i = 0; i < surroundWidth*surroundWidth; i++) {
      surround[i] = 10*i;
    }
    int[] MSERtable = new int[surroundWidth*surroundWidth];
    for (int i = 0; i < surroundWidth*surroundWidth; i++) {
      MSERtable[i] = -1;
    }
        boolean[] isBoundary = new boolean[surroundWidth*surroundWidth];
    for (int i = 0; i < surroundWidth*surroundWidth; i++) {
      isBoundary[i] = false;
    }
    
    LinkedList<Double> boundary = new LinkedList<Double>();
    LinkedList<Double> nextBoundary = new LinkedList<Double>();
    
    
    //CASE 1 - to be added to boundary
    Point2D.Double input = new Point2D.Double(0, 2); //intensity is 60 at this point
    int thresholdIntensity = 55;
    instance.processPoint(input, boundary, nextBoundary, isBoundary, surround, MSERtable, thresholdIntensity, surroundWidth);
    
    assertEquals(boundary.size(), 1);
    assertEquals(nextBoundary.size(), 0);
    boundary.clear();
    nextBoundary.clear();
       
    //CASE 2 - not to be added to boundary - too low intensity
    input = new Point2D.Double(0, 2); //intensity is 60 at this point
    thresholdIntensity = 65;
    instance.processPoint(input, boundary, nextBoundary, isBoundary, surround, MSERtable, thresholdIntensity, surroundWidth);
    assertEquals(boundary.size(), 0);
    assertEquals(nextBoundary.size(), 1);
    boundary.clear();
    nextBoundary.clear();
    
    //CASE 3 - not to be added to boundary or nextBoundary - already in boundary
    input = new Point2D.Double(0, 2); //intensity is 60 at this point
    thresholdIntensity = 55;
    isBoundary[6] = true;
    instance.processPoint(input, boundary, nextBoundary, isBoundary, surround, MSERtable, thresholdIntensity, surroundWidth);
    assertEquals(boundary.size(), 0);
    assertEquals(nextBoundary.size(), 0);
    isBoundary[6] = false;
    boundary.clear();
    nextBoundary.clear();
    
    //CASE 4 - not to be added to boundary or nextBoundary - already in MSER
    input = new Point2D.Double(0, 2); //intensity is 60 at this point
    thresholdIntensity = 55;
    MSERtable[6] = 60;
    instance.processPoint(input, boundary, nextBoundary, isBoundary, surround, MSERtable, thresholdIntensity, surroundWidth);
    assertEquals(boundary.size(), 0);
    assertEquals(nextBoundary.size(), 0);
    boundary.clear();
    nextBoundary.clear();
    
  }

  @Test
  public void testGetMserPlus() {
    System.out.println("getMserPlus");
    int[] surround = {
    12, 16, 20, 26, 35, 30, 35,
    12, 40, 42, 80, 51, 55, 39,
    14, 51, 54, 54, 60, 31, 24,
    26, 74, 77, 82, 85, 84, 22,
    49, 71, 77, 70, 60, 74, 18, 
    11, 71, 74, 69, 68, 69, 11,
    21, 13, 11, 11, 14, 16, 10
  };
    int w = 7;
    int h = 7;
    CMSERCorrelator instance = new CMSERCorrelator(2, 9);
    
    int [] expResult = {
      0, 0, 0, 0, 0, 0, 0, 
      0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 1, 0, 0, 
      0, 1, 1, 1, 1, 1, 0, 
      0, 1, 1, 1, 1, 1, 0, 
      0, 1, 1, 1, 1, 1, 0, 
      0, 0, 0, 0, 0, 0, 0
    };
    int[] result = instance.getMserPlus(surround, w, h);
    assertArrayEquals(expResult, result);
  }


  @Test
  public void testGetCOG() {
    System.out.println("getCOG");
    int[] mat = {
      7, 13, 11,
      9, 17, 5,
      0, 0, 62
    };
    int w = 3;
    int h = 3;
    Point2D.Double expResult = new Point2D.Double(1.5, 1.25);
    Point2D.Double result = CMSERCorrelator.getCOG(mat, w, h);
    assertEquals(expResult, result);
  }


  @Test
  public void testGetNormalizedRegion() {
    //TEST: Create a mser shape to normalize. Underlying region is uniform
    System.out.println("getNormalizedRegion");
    int w = 51;
    int h = 51;
    int[] regionA = new int[w*h];
    int[] mserA = new int[w*h];
    
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        if ((i+j < 78) & (i+j >15) && (i-j > -11) && (i-j < 33)) {
          mserA[j*w+i] = 1;
          regionA[j*w+i] = 1000;
        }
      }
    }
      
    double[][] resultA = CMSERCorrelator.getNormalizedRegion(regionA, mserA, w, h);
    //Convert to int for getCovMatrix
    int[] resA = new int[w*h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        resA[j*h+i] = (int)resultA[j][i];
      }
    }
    
    /*
     * Covariance matrix of normalized region should be (close to) diagonal. (|A[0][0]| >> |A[0][1]|)
     *   Also A[0][0] ~ A[1][1]
     *   holds if region is uniform, as getNormalizedRegion is concerned w/ shape of region only
     * -elements not on diagonal can be nonzero due to rounding errors and interpolation errors
     * -getCovMatrix accepts int [], so rounding is necessary
    */
    
    //this should hold for any shape of mser
    double [][] covA = CMSERCorrelator.getCovMatrix(resA, w, h);System.out.println("A:");
    assertEquals(0, covA[0][1], 0.01*covA[0][0]);
    assertEquals(0, covA[1][0], 0.01*covA[0][0]);
    assertEquals(covA[0][0], covA[1][1], 0.01*covA[0][0]);
    
    

  }
  @Test
  public void testGetCovMatrix() {
    System.out.println("getCovMatrix");
    int w = 5;
    int h = 5;
    
    int[] mat = {
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0,
      0, 1, 1, 1, 0,
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0,
    };
    double[][] result = CMSERCorrelator.getCovMatrix(mat, w, h);
    double sum = 0.0;
    int ct = 0;
    Point2D.Double COG = CMSERCorrelator.getCOG(mat, w, h);
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        sum += (1/result[0][0]) * (x1-COG.x) * (x1-COG.x) * mat[y1*w+x1];
        ct += mat[y1*w+x1];
      }
    }
    double test = sum/ct;
    /* 
     * Covariance matrix is used for normalization
     * this test only tests if the Cov matrix scales properly
     * if the Cov matrix scales properly && is diagonal
     * then the covariance of the scaled region should be 1.0     
     *   
     */
    assertEquals(test, 1.0, 0.01);
    
  }

  @Test
  public void testGetMaxChannel() {
    System.out.println("getMaxChannel");
    double[][] region1 = {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 10, 10, 10},
      {1, 1, 1, 10, 10, 10, 1},
      {1, 1, 10, 10, 10, 10, 10},
      {1, 10, 10, 10, 10, 10, 10},
      {1, 10, 10, 10, 10, 10, 10},
      {1, 1, 1, 1, 1, 1, 1}
    };
    double[][] region2 = {
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
      {1, 1, 1, 1, 1, 10, 10},
    };
    int w = 7;
    int h = 7;
    int channelCount = 8;
    int maxGrad = 255;
    int expResult1 = 7;
    int result1 = CMSERCorrelator.getMaxChannel(region1, w, h, channelCount, maxGrad);
    assertEquals(expResult1, result1);
    int expResult2 = 0;
    int result2 = CMSERCorrelator.getMaxChannel(region2, w, h, channelCount, maxGrad);
    assertEquals(expResult2, result2);
    
  }

  @Test
  public void testRotateByChannel() {
    System.out.println("rotateByChannel");
    double[][] region = {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 10, 10, 10, 1, 1},
      {1, 1, 10, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1}
    };
    int channel = 4;
    int channelCount = 8;
    int w = 7;
    int h = 7;
    double[] expResult = null;
    double[] result = CMSERCorrelator.rotateByChannel(region, channel, channelCount, w, h);
    
    for (int j = 0; j < h; j++) {
      for (int i = 0; i < w; i++) {
        System.out.print(result[j*w+i] + ", ");
      }
      System.out.println("");
    }
    //assertEquals(expResult, result);
    //fail("The test case is a prototype.");
  }
  @Test
  public void corrTest() {
  /*
   * Comprehensive test of a big part of the MSER correlator
   * Generates a random region and using a random affine transformation, 
   *   generates its transofrmed counterpart.
   * The scaling part of the transformation is limited, as a significant portion
   *   of the transformed region could be outside of the frame.
   * This does not nullify the test - any real transformation will be between 
   *   MSERs that fit in the same frame, so no radical scaling is expected
   * The test should show that the Cross-Correlation between the original 
   *   regions is orders of magnitude smaller than the CC of those normalized 
   *   using MSERs, thus showing that the MSERs help in matching regions 
   * differing by affine transformation only
   */
    
    
    
    int w = 31;
    int h = 31;
    int max = 255;
    int regionOrig [] = new int[w*h];
    int mserOrig [] = new int[w*h];
    
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        regionOrig[j*w+i] = (int)(Math.random()*max);
      }
    }
    for (int i = 1; i < w-1; i++) {
      for (int j = 1; j < h-1; j++) {
        double coin=  Math.random();
        if (coin >= 0.5){
          mserOrig[j*w+i] = 1;
        }
        else {
          mserOrig[j*w+i] = 0;
        }
      }
    }
    Matrix transform = new Matrix(2,2);
    transform.set(0, 0, (Math.random()/4)+1.0);
    transform.set(1, 1, (Math.random()/4)+1.0);
    
    transform.set(1, 0, 2.0*Math.random()-1.0);
    transform.set(0, 1, 2.0*Math.random()-1.0);
    int regionTransform [] = new int[w*h];
    int mserTransform [] = new int[w*h];
    
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++){
        //get the current point (x, y) as a JAMA Matrix
        double[][] pt = new double[2][1];
        pt[0][0] = (x-w/2); 
        pt[1][0] = (y-h/2);
        Matrix X = new Matrix(pt);
        //transform the point using the normalized -> original MSER transform matrix
        Matrix origCoords = transform.times(X);
        if (origCoords.get(0,0) >= w-1 || origCoords.get(1,0) >= h-1 || origCoords.get(0,0) < 0 || origCoords.get(1,0) < 0) {
          regionTransform[y*w+x] = 0;
          mserTransform[y*w+x] = 0;
        }
        //otherwise interpolate from original MSER using calculated coords
        else {
          regionTransform[y*w+x] = (int)CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), regionOrig, w, h);
          double mserCoin = (int)CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), regionOrig, w, h);
          if (mserCoin >= (y+x)/(w+h) )
            mserTransform[y*w+x] = 1;
          else  {
            mserTransform[y*w+x] = 0;
          }
        }
      }
    }
    double [][] normalized1 = CMSERCorrelator.getNormalizedRegion(regionOrig, mserOrig, w, h);
    double [][] normalized2 = CMSERCorrelator.getNormalizedRegion(regionTransform, mserTransform, w, h);
    int channel1 = CMSERCorrelator.getMaxChannel(normalized1, w, h, 8, 255);
    int channel2 = CMSERCorrelator.getMaxChannel(normalized2, w, h, 8, 255);
    double [] res1 = CMSERCorrelator.rotateByChannel(normalized1, channel1, 8, w, h);
    double [] res2 = CMSERCorrelator.rotateByChannel(normalized2, channel2, 8, w, h);
    
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++){
        System.out.print(mserOrig[y*w+x] + ", ");
      }
      System.out.println();
    }
    
    
    CCrossCorrelationMetric CCmetric = new CCrossCorrelationMetric(new BufferedImage(1,1,1), new BufferedImage(1,1,1), 7, CAreaSimilarityMetric.Shape.RECTANGULAR);
    
    double mserCrossCorr = CCmetric.getValue(res1, res2);
    System.out.println("MSER CrossCorr: " + mserCrossCorr);
    double [] origDouble = new double[w*h];
    double [] transDouble = new double[w*h];
    for (int i =0; i<regionOrig.length; i++) {
      origDouble[i] = (double) regionOrig[i] * mserOrig[i];
      transDouble[i] = (double) regionTransform[i] * mserTransform[i];
    }
    double randomCrossCorr = CCmetric.getValue(origDouble, transDouble);
    System.out.println("RANDOM CrossCorr: " + randomCrossCorr);
    assertTrue((mserCrossCorr / randomCrossCorr) > 100);
   
  }
}
