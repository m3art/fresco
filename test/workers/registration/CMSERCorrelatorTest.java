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
public class CMSERCorrelatorTest {
  
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
//
    
    
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
//    fail("The test case is a prototype.");
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
    //fail("The test case is a prototype.");
  }


  @Test
  public void testGetNormalizedRegion() {
    
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
          //System.out.print(1 + ", ");
        }
        else {
          //System.out.print(0 + ", ");
        }
      }
      //System.out.println("");
    }
      
    double[][] covA = CMSERCorrelator.getCovMatrix(mserA, w, h);
    System.out.println("A:");
    System.out.print(covA[0][0] + ", ");
    System.out.println(covA[0][1] + ", ");
    System.out.print(covA[1][0] + ", ");
    System.out.println(covA[1][1]);
      System.out.println("");
    
    //double[][] expResult = null;
    System.out.println("A:");
    double[][] resultA = CMSERCorrelator.getNormalizedRegion(regionA, mserA, w, h);
    int[] resA = new int[w*h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        //System.out.print(resultA[j][i] + ", ");
        resA[j*h+i] = (int)resultA[j][i];
        //System.out.print(resA[j*h+i] + ", ");
      }
      //System.out.println("");
    }
    covA = CMSERCorrelator.getCovMatrix(resA, w, h);System.out.println("A:");
    System.out.print(covA[0][0] + ", ");
    System.out.println(covA[0][1] + ", ");
    System.out.print(covA[1][0] + ", ");
    System.out.println(covA[1][1]);
      System.out.println("");
    //assertEquals(expResult, result);
    //fail("The test case is a prototype.");
  }
  @Test
  public void testGetCovMatrix() {
    System.out.println("getCovMatrix");
    int[] mat = {
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0,
      0, 1, 1, 1, 0,
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0,
    };
    double[][] mser = {
      { 0, 0, 1, 0, 0},
      { 0, 0, 1, 0, 0},
      { 0, 1, 1, 1, 0},
      { 0, 0, 1, 0, 0},
      { 0, 0, 1, 0, 0}
    };
    int w = 5;
    int h = 5;
    double[][] expResult = null;
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
    System.out.println(test);
    assertEquals(test, 1.0, 0.01);
    
    /*
    
    System.out.print(result[0][0] + ", ");
    System.out.println(result[0][1] + ", ");
    System.out.print(result[1][0] + ", ");
    System.out.println(result[1][1]);
    Matrix mCov = new Matrix(result);
    Matrix mVectors = mCov.eig().getV();
    Matrix mValues = mCov.eig().getD();
    System.out.print(mVectors.get(0, 0) + ", ");
    System.out.println(mVectors.get(0, 1) + ", ");
    System.out.print(mVectors.get(1, 0) + ", ");
    System.out.println(mVectors.get(1, 1));
    mValues.set(0, 0, Math.sqrt(mValues.get(0, 0)) );
    mValues.set(1, 1, Math.sqrt(mValues.get(1, 1)) );
    System.out.print(mValues.get(0, 0) + ", ");
    System.out.println(mValues.get(0, 1) + ", ");
    System.out.print(mValues.get(1, 0) + ", ");
    System.out.println(mValues.get(1, 1));
    
    int centerX = w/2;
    int centerY = h/2;
    Matrix mCOG = new Matrix(2, 1);
    mCOG.set(0, 0, centerX);
    mCOG.set(1, 0, centerY);
    
    double[][] normalized = new double[h][w];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++){
        //get the current point (x, y) as a JAMA Matrix
        double[][] pt = new double[2][1];
        pt[0][0] = (x-centerX); 
        pt[1][0] = (y-centerY);
        Matrix X = new Matrix(pt);
        Matrix tmp = mValues.times(X);
        Matrix tmp2 = mVectors.times(tmp);
        tmp2.plusEquals(mCOG);
        double val = 0.0;
        sum += X.get(0,0) * X.get(0,0) * (1/mValues.get(0,0)) * (1/mValues.get(0,0)) * mser[y][x];
        ct += mser[y][x];
        if (tmp2.get(0,0) >= w-1 || tmp2.get(1,0) >= h-1 || tmp2.get(0,0) < 0 || tmp2.get(1,0) < 0) {
           normalized[y][x] = 0.0;
        }
        else {
          val = CBilinearInterpolation.getValue(new Point2D.Double(tmp2.get(0, 0), tmp2.get(1, 0)), mser);
          
          normalized[y][x] = val;
        }
      }
    }
    System.out.println(sum/ct);
    for (int y = 0; y < h; y++){ 
      for (int x = 0; x < w; x++) {
        
        System.out.print(normalized[y][x] + ", ");
      }
      System.out.println("");
    }
    System.out.println("");
    
    double[][] cov = new double[2][2];
    double norm = 0;
    Point2D.Double COGnorm = new Point2D.Double(centerX, centerY);
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        cov[0][0] += (x1-COGnorm.x) * (x1-COGnorm.x)*normalized[y1][x1];
        cov[0][1] += (x1-COGnorm.x) * (y1-COGnorm.y)*normalized[y1][x1];
        cov[1][0] += (y1-COGnorm.y) * (x1-COGnorm.x)*normalized[y1][x1];
        cov[1][1] += (y1-COGnorm.y) * (y1-COGnorm.y)*normalized[y1][x1];
        norm+=normalized[y1][x1];
      }
    }
    cov[0][0] /= norm;
    cov[0][1] /= norm;
    cov[1][0] /= norm;
    cov[1][1] /= norm;
    
    System.out.print(cov[0][0] + ", ");
    System.out.println(cov[0][1] + ", ");
    System.out.print(cov[1][0] + ", ");
    System.out.println(cov[1][1]);
    
    */
    //assertArrayEquals(expResult, result);
    //fail("The test case is a prototype.");
  }

  @Test
  public void testGetMaxChannel() {
    System.out.println("getMaxChannel");
    double[][] region = {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 10, 10, 1},
      {1, 1, 1, 10, 10, 10, 1},
      {1, 1, 10, 10, 10, 10, 1},
      {1, 10, 10, 10, 10, 10, 1},
      {1, 10, 10, 10, 10, 10, 1},
      {1, 1, 1, 1, 1, 1, 1}
    };
    
    /*int[] mser = {
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1,
      1, 1, 1, 1, 1, 1, 1
    };*/
    int w = 7;
    int h = 7;
    int channelCount = 8;
    int maxGrad = 255;
    int expResult = 0;
    int result = CMSERCorrelator.getMaxChannel(region, w, h, channelCount, maxGrad);
    System.out.println(result);
    //assertEquals(expResult, result);
    //fail("The test case is a prototype.");
  }

  @Test
  public void testRotateByChannel() {
    System.out.println("rotateByChannel");
    double[][] region = {
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 10, 10, 10, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1},
      {1, 1, 1, 1, 1, 1, 1}
    };
    int channel = 0;
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
    int w = 7;
    int h = 7;
    int [] region1 = {
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 81, 0, 0, 0,
        0, 0, 33, 115, 66, 25, 0,
        0, 0, 0, 40, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
    };
    
    
    int [] mser1 = {
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 1, 0, 0, 0,
        0, 0, 1, 1, 1, 1, 0,
        0, 0, 0, 1, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
   };
    int [] region2 = {
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 83, 0, 0, 0, 0,
        0, 131, 165, 90, 0, 0, 0,
        0, 0, 116, 0, 0, 0, 0,
        0, 0, 75, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
    };
    int [] mser2 = {
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 1, 0, 0, 0, 0,
        0, 1, 1, 1, 0, 0, 0,
        0, 0, 1, 0, 0, 0, 0,
        0, 0, 1, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0,
    };
    
    double [][] normalized1 = CMSERCorrelator.getNormalizedRegion(region1, mser1, w, h);
    double [][] normalized2 = CMSERCorrelator.getNormalizedRegion(region2, mser2, w, h);
    int channel1 = CMSERCorrelator.getMaxChannel(normalized1, w, h, 8, 255);
    int channel2 = CMSERCorrelator.getMaxChannel(normalized2, w, h, 8, 255);
    double [] res1 = CMSERCorrelator.rotateByChannel(normalized1, channel1, 8, w, h);
    double [] res2 = CMSERCorrelator.rotateByChannel(normalized2, channel2, 8, w, h);
    
    CCrossCorrelationMetric CCmetric = new CCrossCorrelationMetric(new BufferedImage(1,1,1), new BufferedImage(1,1,1), 7, CAreaSimilarityMetric.Shape.RECTANGULAR);
    
    double CrossCorr = CCmetric.getValue(res1, res2);
    System.out.println("CrossCorr: " + CrossCorr);
    int [] res1int = new int[w*h];
    int [] res2int = new int[w*h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        //System.out.print(res1[j*h+i] + ", ");
        res1int[j*h+i] = (int)(10000 * res1[j*h+i]);
        System.out.print(res1int[j*h+i] + ", ");
      }
      System.out.println("");
    }
    
    System.out.println("");
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        //System.out.print(res2[j*h+i] + ", ");
        res2int[j*h+i] = (int)(10000 * res2[j*h+i]);
        System.out.print(res2int[j*h+i] + ", ");
      }
      System.out.println("");
    }
    Point2D.Double COG1 = CMSERCorrelator.getCOG(res1int, w,h);
    Point2D.Double COG2 = CMSERCorrelator.getCOG(res2int, w,h);
    double[][] cov1 = CMSERCorrelator.getCovMatrix(res1int, w, h);
    double[][] cov2 = CMSERCorrelator.getCovMatrix(res2int, w, h);
    System.out.println(COG1.x + ", " + COG1.y);
    System.out.println(COG2.x + ", " + COG2.y);
    System.out.println("");
    System.out.print(cov1[0][0] + ", ");
    System.out.println(cov1[0][1] + ", ");
    System.out.print(cov1[1][0] + ", ");
    System.out.println(cov1[1][1]);
    System.out.println("");
    System.out.print(cov2[0][0] + ", ");
    System.out.println(cov2[0][1] + ", ");
    System.out.print(cov2[1][0] + ", ");
    System.out.println(cov2[1][1]);
    
  }
}
