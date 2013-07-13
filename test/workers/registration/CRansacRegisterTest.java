/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author Jakub
 */
public class CRansacRegisterTest {
  
  public CRansacRegisterTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Test
  public void testGetMserPlus() {
    
    int w = 5;
    int h = 5;
         
  int[] testInput = {
    40, 42, 80, 51, 55,
    51, 54, 54, 60, 31,
    74, 77, 82, 85, 84,
    71, 77, 70, 60, 74,
    71, 74, 69, 68, 69
  };
    
  int[] realOutput = CRansacRegister.getMserPlus(testInput, w, h);
  int[] testOutput = {
    40, 42, 54, 51, 51,
    51, 54, 54, 60, 31,
    74, 77, 82, 82, 82,
    71, 77, 70, 60, 74,
    71, 74, 69, 68, 69
  };
   int[] tstOutput = {
    0, 0, 0, 0, 0,
    0, 0, 0, 1, 0,
    1, 1, 1, 1, 1,
    1, 1, 1, 1, 1,
    1, 1, 1, 1, 1
  };
  assertArrayEquals(tstOutput, realOutput);
  }

  @Test
  public void testDoInBackground() {
  }

  @Test
  public void testGetWorkerName() {
  }

  @Test
  public void testGetMserMinus() {
    
    int w = 5;
    int h = 5;
         
  int[] testInput = {
    60, 68, 20, 49, 45,
    49, 46, 46, 40, 69,
    26, 23, 18, 15, 16,
    29, 23, 30, 40, 26,
    29, 26, 31, 32, 31
  };
    
  int[] realOutput = CRansacRegister.getMserMinus(testInput, w, h);
  int[] testOutput = {
    
    60, 68, 46, 49, 49,
    49, 46, 46, 40, 69,
    26, 23, 18, 18, 18,
    29, 23, 30, 40, 26,
    29, 26, 31, 32, 31
  
  };
   int[] tstOutput = {
    0, 0, 0, 0, 0,
    0, 0, 0, 1, 0,
    1, 1, 1, 1, 1,
    1, 1, 1, 1, 1,
    1, 1, 1, 1, 1
  };
  assertArrayEquals(tstOutput, realOutput);
  }

  @Test
  public void testGetCOG() {
  }

  @Test
  public void testGetCovMatrix() {
    int w = 5;
    int h = 5;
    
    int[] orig = {
      0, 0, 0, 0, 1,
      0, 1, 0, 1, 0,
      0, 0, 1, 0, 0,
      0, 1, 0, 1, 0,
      1, 0, 0, 0, 0
    };
    double[][] output = CRansacRegister.getCovMatrix(orig, w, h);
    double[][] real = {
      {1.714,-1.142},
      {-1.142, 1.714}
    };
    assertArrayEquals(output[0], real[0], 0.001);
    assertArrayEquals(output[1], real[1], 0.001);
      
  }

  @Test
  public void testBilinearInterpolate() {
    int w = 5;
    int h = 5;
    double x = 2.2;
    double y = 1.8;
    double[][] orig = {
      {0, 0, 0, 0, 0},
      {0, 1, 2, 3, 4},
      {0, 1, 4, 9, 16},
      {0, 1, 8, 27, 64},
      {0, 0, 0, 0, 0}
    };
    double real = CRansacRegister.bilinearInterpolate(orig, x, y, w, h);
    double test = 4.44;
    assertEquals(real, test, 0.001);
  }

  @Test
  public void testGetNormalizedRegion() {
  }

  @Test
  public void testGetMaxChannel() {
  }

  @Test
  public void testRotateByChannel() {
  }
  
}
