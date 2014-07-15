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
//import workers.registration.CHuMoments;

/**
 *
 * @author Jakub
 */
public class CMSERCorrelatorTest extends CMSERCorrelator {
  
  public CMSERCorrelatorTest() {
  }
  
  @Test
  public void testGetMSERIntensity() {
    System.out.println("getMSERIntensity");
    int[] sizes = new int[10];
    for (int i = 0; i < 10; i++) {
      sizes[i] = 10 * i;
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
    
    int[] surround = new int[surroundWidth * surroundWidth];
    for (int i = 0; i < surroundWidth * surroundWidth; i++) {
      surround[i] = 10 * i;
    }
    int[] MSERtable = new int[surroundWidth * surroundWidth];
    for (int i = 0; i < surroundWidth * surroundWidth; i++) {
      MSERtable[i] = -1;
    }
    boolean[] isBoundary = new boolean[surroundWidth * surroundWidth];
    for (int i = 0; i < surroundWidth * surroundWidth; i++) {
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
    
    int[] expResult = {
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
    Point2D.Double result = CMSERCorrelator.getCenterOfGravity(mat, w, h);
    assertEquals(expResult, result);
  }
  
  @Test
  public void testGetNormalizedRegion() {
    //TEST: Create a mser shape to normalize. Underlying region is uniform
    System.out.println("getNormalizedRegion");
    int w = 51;
    int h = 51;
    int[] regionA = new int[w * h];
    int[] mserA = new int[w * h];
    
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        if ((i + j < 78) & (i + j > 15) && (i - j > -11) && (i - j < 33)) {
          mserA[j * w + i] = 1;
          regionA[j * w + i] = 1000;
        }
      }
    }
    
    double[][] resultA = CMSERCorrelator.getNormalizedRegion(regionA, mserA, w, h);
    //Convert to int for getCovMatrix
    int[] resA = new int[w * h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        resA[j * h + i] = (int) resultA[j][i];
      }
    }

    /*
     * Covariance matrix of normalized region should be (close to) diagonal.
     * (|A[0][0]| >> |A[0][1]|) Also A[0][0] ~ A[1][1] holds if region is
     * uniform, as getNormalizedRegion is concerned w/ shape of region only
     * -elements not on diagonal can be nonzero due to rounding errors and
     * interpolation errors -getCovMatrix accepts int [], so rounding is
     * necessary
     */

    //this should hold for any shape of mser
    double[][] covA = CMSERCorrelator.getCovMatrix(resA, w, h);
    System.out.println("A:");
    assertEquals(0, covA[0][1], 0.01 * covA[0][0]);
    assertEquals(0, covA[1][0], 0.01 * covA[0][0]);
    assertEquals(covA[0][0], covA[1][1], 0.01 * covA[0][0]);
    
    
    
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
      0, 0, 1, 0, 0,};
    double[][] result = CMSERCorrelator.getCovMatrix(mat, w, h);
    double sum = 0.0;
    int ct = 0;
    Point2D.Double COG = CMSERCorrelator.getCenterOfGravity(mat, w, h);
    for (int x1 = 0; x1 < w; x1++) {
      for (int y1 = 0; y1 < h; y1++) {
        sum += (1 / result[0][0]) * (x1 - COG.x) * (x1 - COG.x) * mat[y1 * w + x1];
        ct += mat[y1 * w + x1];
      }
    }
    double test = sum / ct;
    /*
     * Covariance matrix is used for normalization this test only tests if the
     * Cov matrix scales properly if the Cov matrix scales properly && is
     * diagonal then the covariance of the scaled region should be 1.0
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
      {1, 1, 1, 1, 1, 10, 10},};
    int w = 7;
    int h = 7;
    int channelCount = 8;
    int maxGrad = 255;
    int expResult1 = 6;
    int result1 = CMSERCorrelator.getMaxChannel(region1, w, h, channelCount, maxGrad);
    assertEquals(expResult1, result1);
    int expResult2 = 1;
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
        System.out.print(result[j * w + i] + ", ");
      }
      System.out.println("");
    }
    //assertEquals(expResult, result);
    //fail("The test case is a prototype.");
  }
  
  @Test
  public void corrTest() {
    /*
     * Comprehensive test of a big part of the MSER correlator Generates a
     * random region and using a random affine transformation, generates its
     * transofrmed counterpart. The scaling part of the transformation is
     * limited, as a significant portion of the transformed region could be
     * outside of the frame. This does not nullify the test - any real
     * transformation will be between MSERs that fit in the same frame, so no
     * radical scaling is expected The test should show that the
     * Cross-Correlation between the original regions is orders of magnitude
     * smaller than the CC of those normalized using MSERs, thus showing that
     * the MSERs help in matching regions differing by affine transformation
     * only
     */
    
    
    
    int w = 51;
    int h = 51;
    int max = 255;
    //int regionOrig[] = new int[w * h];
    int[] regionOrig = {
      133, 135, 136, 137, 137, 133, 138, 136, 135, 138, 134, 140, 137, 135, 138, 132, 136, 137, 141, 135, 138, 140, 137, 141, 142, 137, 138, 137, 141, 134, 140, 137, 136, 138, 137, 142, 136, 139, 139, 140, 143, 139, 143, 142, 145, 142, 145, 141, 145, 145, 147,
      135, 136, 138, 140, 137, 130, 134, 132, 135, 136, 134, 136, 137, 134, 134, 137, 135, 136, 140, 138, 136, 141, 133, 139, 137, 135, 139, 139, 138, 145, 143, 137, 138, 135, 136, 141, 139, 142, 141, 139, 138, 140, 143, 143, 143, 142, 142, 150, 143, 145, 150,
      137, 135, 135, 137, 137, 133, 134, 134, 137, 137, 136, 136, 138, 137, 138, 136, 137, 139, 138, 138, 138, 137, 135, 136, 140, 140, 138, 140, 142, 141, 141, 139, 141, 140, 140, 141, 140, 141, 142, 141, 140, 139, 142, 142, 143, 143, 144, 149, 147, 145, 150,
      136, 133, 131, 133, 133, 134, 135, 137, 137, 137, 136, 135, 134, 135, 138, 134, 138, 140, 135, 136, 139, 136, 139, 135, 142, 144, 136, 138, 142, 136, 138, 141, 141, 142, 141, 138, 140, 137, 141, 141, 141, 140, 142, 143, 145, 147, 146, 147, 149, 145, 149,
      137, 135, 134, 134, 136, 137, 136, 139, 138, 136, 137, 135, 133, 137, 138, 136, 138, 138, 135, 136, 139, 138, 142, 136, 140, 142, 135, 137, 141, 136, 138, 142, 141, 144, 143, 141, 142, 139, 142, 141, 142, 141, 143, 143, 145, 146, 147, 146, 147, 145, 148,
      134, 134, 135, 135, 135, 134, 137, 139, 135, 136, 139, 138, 138, 142, 139, 142, 139, 137, 137, 137, 138, 142, 139, 136, 137, 138, 138, 140, 140, 138, 138, 142, 139, 141, 141, 140, 143, 139, 142, 142, 143, 142, 142, 141, 142, 144, 146, 148, 144, 146, 148,
      133, 134, 136, 135, 134, 134, 138, 137, 135, 136, 138, 138, 138, 141, 139, 144, 140, 137, 139, 138, 137, 142, 137, 137, 136, 138, 141, 140, 137, 138, 137, 140, 139, 139, 140, 140, 142, 141, 139, 142, 142, 143, 144, 143, 144, 144, 145, 147, 144, 146, 148,
      134, 136, 138, 138, 136, 137, 137, 135, 136, 138, 135, 134, 135, 136, 137, 139, 139, 137, 138, 139, 138, 138, 138, 137, 137, 138, 139, 137, 136, 136, 138, 140, 144, 142, 141, 144, 142, 143, 139, 140, 142, 144, 144, 145, 144, 144, 146, 145, 145, 148, 149,
      132, 136, 137, 135, 135, 135, 135, 133, 137, 140, 135, 134, 138, 137, 135, 133, 136, 138, 136, 139, 140, 134, 138, 136, 136, 137, 135, 136, 137, 137, 134, 136, 143, 141, 138, 141, 138, 139, 140, 140, 141, 141, 141, 141, 141, 141, 147, 142, 149, 147, 147,
      136, 137, 136, 133, 132, 137, 135, 137, 132, 133, 137, 138, 138, 134, 139, 140, 136, 138, 133, 136, 135, 137, 136, 137, 133, 136, 134, 142, 133, 136, 134, 134, 138, 135, 142, 137, 140, 141, 140, 142, 142, 139, 147, 138, 144, 143, 142, 144, 147, 146, 151,
      136, 135, 137, 138, 136, 135, 137, 136, 139, 138, 137, 135, 133, 136, 133, 139, 135, 140, 128, 135, 133, 140, 128, 135, 133, 132, 126, 132, 126, 130, 132, 130, 136, 140, 143, 139, 144, 143, 142, 141, 140, 138, 143, 139, 143, 144, 142, 144, 146, 146, 149,
      133, 137, 141, 137, 135, 139, 136, 129, 132, 135, 134, 135, 132, 135, 136, 134, 129, 133, 129, 130, 126, 124, 118, 126, 123, 124, 113, 110, 107, 109, 96, 101, 126, 143, 137, 131, 140, 141, 140, 138, 143, 144, 144, 144, 143, 142, 142, 145, 145, 148, 147,
      131, 135, 140, 136, 135, 139, 135, 134, 133, 133, 136, 135, 128, 127, 123, 122, 126, 118, 121, 111, 112, 107, 101, 100, 90, 94, 88, 80, 76, 76, 76, 77, 113, 142, 139, 137, 142, 135, 146, 138, 141, 141, 139, 144, 145, 148, 144, 146, 144, 148, 148,
      137, 131, 134, 140, 138, 133, 127, 134, 125, 123, 123, 115, 112, 107, 99, 95, 98, 88, 93, 81, 81, 75, 79, 79, 69, 73, 77, 78, 80, 78, 76, 78, 124, 146, 135, 132, 142, 144, 144, 138, 141, 141, 138, 142, 144, 147, 144, 147, 145, 148, 149,
      137, 132, 135, 140, 136, 131, 134, 120, 100, 92, 89, 83, 80, 75, 81, 77, 75, 77, 83, 85, 83, 81, 81, 87, 84, 81, 83, 86, 83, 76, 83, 71, 121, 140, 136, 137, 141, 138, 140, 138, 144, 145, 146, 142, 140, 142, 144, 145, 144, 146, 149,
      133, 134, 137, 135, 131, 130, 134, 85, 70, 75, 76, 81, 80, 80, 81, 85, 84, 85, 79, 78, 78, 81, 76, 83, 88, 85, 87, 88, 83, 81, 76, 65, 130, 140, 136, 136, 141, 142, 142, 142, 141, 141, 145, 138, 143, 146, 145, 145, 145, 144, 150,
      134, 131, 133, 136, 132, 127, 143, 73, 72, 84, 78, 82, 79, 83, 81, 82, 88, 85, 89, 87, 88, 87, 97, 93, 94, 89, 87, 80, 75, 81, 80, 64, 131, 136, 137, 140, 142, 142, 136, 138, 141, 141, 150, 138, 144, 146, 146, 145, 146, 145, 152,
      138, 129, 135, 128, 135, 129, 141, 71, 71, 80, 67, 90, 77, 87, 95, 93, 93, 90, 95, 92, 83, 91, 85, 83, 93, 99, 103, 113, 83, 73, 73, 70, 120, 151, 142, 138, 140, 135, 142, 143, 143, 138, 134, 144, 142, 144, 148, 145, 144, 147, 150,
      139, 134, 134, 128, 130, 123, 141, 70, 68, 75, 80, 82, 93, 82, 89, 89, 97, 100, 108, 114, 115, 51, 132, 155, 167, 184, 199, 212, 89, 69, 74, 75, 126, 141, 136, 138, 142, 141, 138, 139, 141, 141, 140, 144, 143, 147, 140, 143, 148, 149, 148,
      133, 136, 134, 133, 133, 128, 139, 77, 66, 84, 70, 101, 83, 60, 137, 153, 171, 193, 195, 207, 178, 49, 187, 224, 218, 214, 210, 226, 77, 79, 73, 78, 137, 141, 140, 139, 141, 140, 139, 138, 140, 144, 141, 141, 139, 145, 143, 146, 148, 150, 150,
      135, 134, 132, 133, 133, 133, 133, 73, 64, 78, 62, 104, 87, 63, 220, 230, 218, 212, 214, 228, 170, 57, 191, 216, 205, 208, 215, 231, 91, 80, 82, 79, 140, 141, 140, 137, 141, 143, 142, 141, 140, 143, 139, 139, 137, 144, 147, 147, 148, 150, 151,
      139, 134, 135, 132, 137, 135, 128, 73, 59, 83, 66, 92, 92, 82, 224, 206, 217, 212, 211, 228, 186, 72, 211, 237, 241, 246, 248, 245, 107, 80, 84, 83, 139, 142, 140, 137, 142, 141, 143, 143, 140, 143, 140, 142, 143, 145, 143, 145, 148, 148, 147,
      137, 134, 139, 133, 137, 135, 135, 82, 64, 91, 60, 104, 81, 92, 243, 239, 238, 238, 239, 245, 193, 87, 233, 251, 250, 246, 249, 251, 104, 82, 83, 100, 141, 143, 140, 140, 143, 137, 141, 144, 141, 142, 140, 144, 145, 143, 145, 145, 145, 146, 147,
      135, 134, 138, 131, 134, 128, 140, 79, 74, 77, 59, 111, 83, 113, 249, 251, 245, 248, 241, 250, 193, 91, 233, 250, 247, 242, 249, 248, 96, 89, 80, 120, 138, 137, 138, 139, 142, 142, 142, 145, 140, 143, 140, 142, 144, 142, 149, 147, 146, 146, 149,
      132, 135, 138, 134, 135, 130, 131, 80, 77, 83, 68, 103, 86, 128, 251, 250, 251, 240, 243, 250, 190, 94, 238, 249, 242, 244, 251, 243, 96, 91, 66, 124, 135, 140, 144, 141, 138, 144, 142, 146, 140, 144, 140, 141, 145, 146, 146, 147, 150, 148, 148,
      136, 136, 134, 134, 133, 134, 141, 98, 66, 77, 66, 106, 92, 133, 250, 250, 252, 244, 244, 248, 187, 93, 239, 249, 245, 247, 247, 238, 88, 84, 70, 126, 134, 142, 142, 140, 143, 142, 141, 140, 142, 142, 145, 139, 140, 144, 144, 144, 146, 146, 144,
      140, 135, 135, 140, 137, 132, 137, 96, 69, 92, 65, 105, 73, 142, 252, 253, 246, 244, 249, 248, 183, 86, 240, 247, 245, 245, 246, 231, 78, 93, 70, 122, 146, 141, 147, 135, 143, 145, 146, 145, 145, 140, 143, 140, 141, 143, 143, 146, 145, 148, 148,
      134, 139, 140, 136, 135, 136, 140, 96, 56, 91, 61, 104, 75, 146, 251, 252, 251, 248, 243, 248, 169, 79, 230, 235, 226, 215, 208, 175, 70, 87, 74, 123, 143, 142, 142, 142, 137, 143, 143, 146, 148, 144, 146, 143, 143, 142, 142, 147, 143, 149, 148,
      140, 136, 140, 140, 140, 138, 134, 106, 66, 85, 74, 97, 64, 134, 248, 244, 237, 218, 216, 201, 117, 72, 141, 130, 115, 110, 89, 84, 79, 87, 77, 124, 145, 136, 144, 142, 140, 140, 138, 141, 145, 143, 148, 145, 143, 143, 143, 147, 143, 148, 146,
      137, 132, 134, 134, 135, 133, 147, 86, 68, 82, 80, 99, 72, 95, 172, 152, 133, 111, 109, 87, 84, 83, 75, 88, 91, 102, 108, 108, 76, 93, 72, 129, 144, 141, 140, 143, 144, 144, 143, 142, 143, 142, 145, 141, 141, 144, 146, 146, 147, 146, 145,
      139, 139, 142, 133, 136, 139, 140, 89, 67, 77, 82, 97, 83, 74, 80, 99, 113, 119, 123, 133, 106, 76, 158, 185, 199, 206, 228, 206, 64, 93, 72, 134, 139, 147, 139, 151, 142, 143, 144, 142, 144, 144, 147, 142, 141, 144, 146, 145, 147, 146, 143,
      141, 133, 138, 135, 138, 133, 139, 89, 67, 84, 76, 109, 86, 93, 204, 235, 227, 218, 232, 251, 154, 102, 251, 245, 251, 246, 247, 227, 73, 92, 80, 128, 146, 142, 146, 145, 139, 139, 141, 142, 144, 145, 149, 144, 143, 143, 143, 145, 145, 147, 145,
      138, 135, 140, 134, 137, 138, 131, 81, 78, 87, 68, 118, 73, 177, 254, 253, 255, 254, 254, 252, 176, 122, 250, 253, 251, 243, 250, 218, 79, 90, 77, 125, 148, 145, 138, 142, 132, 145, 142, 143, 144, 141, 145, 142, 142, 142, 140, 147, 144, 149, 148,
      140, 140, 136, 134, 137, 132, 140, 83, 77, 79, 80, 122, 78, 206, 254, 252, 255, 253, 251, 253, 170, 133, 251, 248, 250, 248, 250, 222, 75, 90, 72, 127, 136, 147, 143, 140, 144, 142, 141, 143, 142, 139, 139, 142, 140, 136, 142, 146, 147, 150, 149,
      136, 140, 138, 138, 138, 130, 138, 82, 68, 80, 83, 120, 85, 215, 254, 254, 254, 253, 252, 253, 173, 137, 253, 248, 249, 244, 251, 222, 76, 96, 73, 144, 147, 138, 144, 146, 139, 139, 143, 144, 145, 143, 143, 145, 145, 143, 145, 147, 147, 149, 148,
      134, 139, 135, 136, 136, 133, 137, 84, 69, 80, 86, 124, 89, 222, 254, 254, 253, 253, 254, 254, 175, 145, 249, 248, 251, 246, 250, 217, 73, 96, 80, 131, 145, 145, 141, 147, 147, 143, 145, 145, 144, 144, 142, 142, 141, 140, 147, 148, 147, 149, 150,
      137, 143, 138, 139, 139, 139, 137, 79, 79, 78, 88, 131, 91, 226, 254, 254, 253, 254, 254, 253, 174, 150, 253, 252, 250, 248, 250, 215, 80, 95, 81, 132, 142, 145, 146, 143, 138, 146, 145, 143, 143, 146, 146, 146, 146, 148, 147, 148, 147, 150, 151,
      132, 137, 135, 137, 136, 131, 139, 75, 81, 75, 90, 129, 95, 232, 254, 254, 254, 254, 252, 253, 171, 157, 252, 252, 246, 250, 251, 213, 85, 84, 82, 137, 148, 143, 141, 145, 142, 144, 143, 140, 141, 142, 143, 143, 143, 146, 146, 146, 145, 148, 151,
      137, 137, 136, 137, 138, 133, 142, 74, 75, 79, 89, 119, 97, 237, 254, 255, 255, 254, 252, 254, 170, 167, 253, 253, 252, 252, 251, 206, 86, 87, 79, 142, 146, 139, 141, 145, 143, 149, 151, 148, 146, 147, 146, 144, 145, 144, 146, 146, 144, 147, 150,
      138, 133, 134, 133, 138, 131, 141, 72, 76, 86, 89, 120, 96, 238, 253, 253, 254, 253, 252, 254, 162, 164, 251, 249, 240, 233, 235, 189, 70, 91, 87, 136, 145, 148, 138, 140, 149, 143, 143, 142, 142, 144, 144, 146, 146, 145, 146, 148, 145, 148, 150,
      139, 131, 138, 136, 141, 128, 134, 66, 82, 93, 89, 131, 94, 238, 253, 252, 254, 253, 249, 252, 149, 152, 246, 239, 231, 227, 243, 197, 63, 89, 81, 144, 145, 145, 148, 145, 144, 145, 143, 145, 144, 144, 144, 145, 145, 144, 145, 147, 146, 150, 151,
      137, 138, 135, 139, 134, 137, 137, 58, 87, 92, 88, 129, 82, 227, 254, 252, 254, 255, 252, 254, 155, 181, 254, 254, 253, 253, 252, 210, 82, 94, 85, 138, 144, 147, 145, 143, 154, 140, 146, 146, 146, 144, 141, 143, 145, 145, 142, 146, 149, 149, 149,
      140, 134, 139, 134, 137, 137, 135, 70, 85, 84, 98, 125, 100, 250, 254, 253, 255, 255, 253, 254, 169, 201, 254, 252, 254, 253, 253, 212, 82, 90, 94, 144, 146, 144, 145, 141, 142, 145, 145, 144, 146, 145, 144, 146, 148, 146, 143, 143, 146, 148, 151,
      133, 140, 133, 136, 135, 136, 130, 65, 77, 77, 97, 122, 105, 252, 254, 254, 255, 255, 253, 254, 177, 216, 255, 253, 254, 252, 253, 213, 84, 90, 88, 146, 143, 142, 145, 143, 141, 153, 141, 141, 144, 144, 142, 143, 144, 143, 145, 144, 146, 150, 152,
      135, 137, 136, 130, 136, 133, 140, 58, 84, 85, 94, 134, 113, 254, 254, 254, 254, 254, 252, 254, 177, 219, 255, 253, 255, 254, 252, 211, 82, 92, 88, 153, 139, 143, 145, 143, 143, 145, 138, 141, 146, 147, 143, 142, 145, 146, 141, 146, 148, 149, 149,
      136, 137, 138, 130, 134, 130, 141, 52, 82, 81, 90, 130, 118, 254, 254, 254, 254, 255, 254, 254, 178, 223, 255, 254, 252, 255, 253, 213, 81, 87, 100, 151, 140, 146, 143, 142, 145, 140, 139, 140, 146, 147, 144, 144, 146, 147, 138, 145, 148, 146, 145,
      134, 141, 135, 136, 132, 131, 135, 59, 85, 78, 99, 120, 123, 254, 254, 255, 255, 255, 255, 255, 177, 228, 253, 255, 251, 255, 254, 212, 81, 77, 108, 133, 140, 145, 140, 144, 143, 147, 144, 142, 143, 145, 145, 145, 145, 143, 143, 146, 146, 145, 147,
      138, 133, 139, 135, 135, 132, 127, 62, 81, 77, 105, 113, 124, 253, 255, 254, 255, 255, 254, 254, 174, 230, 254, 255, 253, 254, 254, 195, 75, 73, 107, 124, 141, 146, 139, 143, 140, 145, 150, 146, 146, 146, 146, 146, 146, 145, 147, 146, 146, 148, 150,
      133, 133, 134, 138, 130, 132, 126, 60, 75, 77, 100, 116, 126, 254, 254, 252, 254, 255, 254, 253, 173, 231, 253, 253, 253, 253, 251, 173, 65, 74, 93, 122, 140, 148, 144, 144, 145, 145, 144, 142, 144, 145, 142, 143, 146, 148, 143, 142, 145, 150, 150,
      112, 126, 121, 126, 117, 121, 117, 66, 71, 77, 87, 105, 118, 253, 252, 254, 254, 252, 254, 253, 159, 228, 252, 250, 251, 250, 228, 123, 66, 88, 72, 114, 138, 145, 141, 144, 148, 147, 146, 145, 143, 144, 144, 147, 145, 141, 142, 148, 146, 151, 151,
      132, 135, 128, 126, 123, 124, 113, 58, 70, 75, 91, 95, 99, 251, 253, 251, 251, 252, 247, 238, 97, 172, 225, 212, 181, 155, 114, 79, 85, 76, 84, 121, 127, 136, 137, 134, 138, 140, 140, 141, 141, 137, 145, 142, 142, 141, 141, 145, 144, 146, 148
    };    
    
    int mserOrig[] = null;
    
    mserOrig = CMSERCorrelator.getMserPlus(regionOrig, w, h);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        System.out.print(regionOrig[y * w + x] + ", ");
      }
      System.out.println();
    }


    Matrix scale = new Matrix(2, 2);
    //scale.set(0, 0, 0.875 + Math.random() / 4);
    //scale.set(1, 1, 0.875 + Math.random() / 4);
    
    scale.set(0, 0, 1.0);
    scale.set(1, 1, 1.0);
    
    Matrix rotation = new Matrix(2, 2);
    double angle = Math.random() * 2 * Math.PI;
    angle = 0.5*Math.PI;
    rotation.set(0, 0, Math.cos(angle));
    rotation.set(1, 1, Math.cos(angle));
    rotation.set(1, 0, -Math.sin(angle));
    rotation.set(0, 1, Math.sin(angle));
    Matrix transform = scale.times(rotation);
   //Matrix transform = scale;
    int regionTransform[] = new int[w * h];
    int mserTransform[] = new int[w * h];
    
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        //get the current point (x, y) as a JAMA Matrix
        double[][] pt = new double[2][1];
        pt[0][0] = (x - w / 2);        
        pt[1][0] = (y - h / 2);
        Matrix X = new Matrix(pt);
        //transform the point using the normalized -> original MSER transform matrix
        Matrix origCoords = transform.times(X);
        pt[0][0] = origCoords.get(0, 0);
        pt[1][0] = origCoords.get(1, 0);
        origCoords.set(0, 0, pt[0][0] + w / 2);
        origCoords.set(1, 0, pt[1][0] + h / 2);
        if (origCoords.get(0, 0) >= w - 1 || origCoords.get(1, 0) >= h - 1 || origCoords.get(0, 0) < 0 || origCoords.get(1, 0) < 0) {
          regionTransform[y * w + x] = -1;
          mserTransform[y * w + x] = 0;
        } //otherwise interpolate from original MSER using calculated coords
        else {
          regionTransform[y * w + x] = (int) CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), regionOrig, w, h);
          double mserCoin = CBilinearInterpolation.getValue(new Point2D.Double(origCoords.get(0, 0), origCoords.get(1, 0)), mserOrig, w, h);
          if (mserCoin >= 0.5) {
            
            mserTransform[y * w + x] = 1;
          } else {
            
            mserTransform[y * w + x] = 0;
          }
        }
        if (regionTransform[y * w + x] == 0) {
          System.out.println("ORIG: " + origCoords.get(0,0) + ", " + origCoords.get(1,0));
        }
      }
    }
    //mserTransform = CMSERCorrelator.getMserPlus(regionTransform, w, h);
    System.out.println();
    System.out.println();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        System.out.print(regionTransform[y * w + x] + ", ");
      }
      System.out.println();
    }
    
    double [][] dRegionOrig = new double[h][w]; 
    double [][] dRegionTransform = new double[h][w];
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        dRegionOrig[y][x] = regionOrig[y*w+x];
        dRegionTransform[y][x] = regionTransform[y*w+x];
      }
    }
    double scoreBase = 0.0;
    for (int i = 1; i < 8; i++) {
      //double origVal = CHuMoments.getHuMoment(dRegionOrig, i);
      //double transformVal = CHuMoments.getHuMoment(dRegionTransform, i);
      //System.out.println(CHuMoments.getHuMoment(dRegionOrig, i) + " vs. " + CHuMoments.getHuMoment(dRegionTransform, i));
      //scoreBase += Math.log(Math.abs(origVal - transformVal));
    }
    System.out.println("");
    double[][] normalized1 = CMSERCorrelator.getNormalizedRegion(regionOrig, mserOrig, w, h);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        System.out.format("%05.1f,", normalized1[y][x]);
      }
      System.out.println();
    }
    System.out.println("");
    double[][] normalized2 = CMSERCorrelator.getNormalizedRegion(regionTransform, mserTransform, w, h);
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        System.out.format("%05.1f,", normalized2[y][x]);
      }
      System.out.println();
    }
    
    double scoreNorm = 0.0;
    for (int i = 1; i < 8; i++) {
      //double origVal = CHuMoments.getHuMoment(normalized1, i);
      //double transformVal = CHuMoments.getHuMoment(normalized2, i);
      //System.out.println(CHuMoments.getHuMoment(normalized1, i) + " vs. " + CHuMoments.getHuMoment(normalized2, i));
      //scoreNorm += Math.log(Math.abs(origVal - transformVal));
    }
    System.out.println("norm1");
    int channel1 = CMSERCorrelator.getMaxChannel(normalized1, w, h, 32, 255);
    System.out.println("norm2");
    int channel2 = CMSERCorrelator.getMaxChannel(normalized2, w, h, 32, 255);
    double[] res1 = CMSERCorrelator.rotateByChannel(normalized1, channel1, 32, w, h);
    double[] res2 = CMSERCorrelator.rotateByChannel(normalized2, channel2, 32, w, h);
    
    
    System.out.println(channel1 + " vs. " + channel2);
    CCrossCorrelationMetric CCmetric = new CCrossCorrelationMetric(new BufferedImage(1, 1, 1), new BufferedImage(1, 1, 1), 7, CAreaSimilarityMetric.Shape.RECTANGULAR);
    System.out.println( "CC on original: " + CCmetric.getValue(regionOrig, regionTransform));
    double mserCrossCorr = CCmetric.getValue(res1, res2);
    
    
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        //System.out.format("%1.1f , ", res1[y * w + x]);
      }
      //System.out.println();
    }    
    System.out.println();
    System.out.println();
    
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        //System.out.format("%1.1f , ", res2[y * w + x]);
      }
      //System.out.println();
    }
    
    
    System.out.println("MSER CrossCorr: " + mserCrossCorr);
    double[] origDouble = new double[w * h];
    double[] transDouble = new double[w * h];
    for (int i = 0; i < regionOrig.length; i++) {
      origDouble[i] = (double) regionOrig[i] * mserOrig[i];
      transDouble[i] = (double) regionTransform[i] * mserTransform[i];
    }
    double randomCrossCorr = CCmetric.getValue(origDouble, transDouble);
    System.out.println("RANDOM CrossCorr: " + randomCrossCorr);
    assertTrue((mserCrossCorr / randomCrossCorr) > 100);
    
  }
}
