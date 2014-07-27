/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import workers.analyse.paramObjects.CCOGParams;

/**
 *
 * @author Jakub
 */
public class CCornerDetectorCOGTest {

  public CCornerDetectorCOGTest() {
  }

  @Test
  public void testGetCOGSmall() {
    System.out.println("getCOGSmall");
    int w = 5;
    int[] input = {
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 255, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(w);

    instance.param = new CCOGParams(CCOGParams.values.learned);
    instance.param.windowSize = w;


    double[] retVal = instance.getCOG(input);
    assertEquals(0.60, retVal[0], 0.01);

  }

  @Test
  public void testGetCOGLarge1() {
    System.out.println("getCOGLarge1");
    int w = 11;
    int[] inputEdgedC = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 132, 130, 133, 140, 144, 0,
      0, 0, 0, 0, 140, 50, 0, 0, 0, 149, 0,
      0, 0, 0, 0, 143, 0, 0, 0, 0, 142, 0,
      0, 0, 0, 0, 143, 0, 0, 0, 0, 140, 0,
      0, 0, 0, 0, 145, 0, 0, 0, 0, 141, 0,
      0, 0, 0, 0, 146, 0, 0, 0, 0, 139, 0,
    
    };


    CCornerDetectorCOG instance = new CCornerDetectorCOG(w);

    instance.param = new CCOGParams(CCOGParams.values.learned);
    instance.param.windowSize = w;


    double[] retVal = instance.getCOG(inputEdgedC);
    System.out.println(retVal[0]);
    System.out.println(retVal[1]);
    System.out.println(retVal[2]);
    System.out.println(retVal[3]);
    System.out.println(retVal[4]);
    System.out.println(retVal[5]);
    //assertEquals(0.67, retVal[0], 0.01);

  }

  @Test
  public void testGetCOGLarge2() {
    System.out.println("getCOGLarge2");
    int w = 11;
    int[] inputEdgedE = {
      0, 0, 0, 0, 0, 129, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 130, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 130, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 87, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 86, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 43, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 130, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 130, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 129, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 131, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 88, 0, 0, 0, 0, 0,
    };


    CCornerDetectorCOG instance = new CCornerDetectorCOG(w);

    instance.param = new CCOGParams(CCOGParams.values.learned);
    instance.param.windowSize = w;


    double[] retVal = instance.getCOG(inputEdgedE);
    System.out.println(retVal[0]);
    System.out.println(retVal[1]);
    System.out.println(retVal[2]);
    System.out.println(retVal[3]);
    System.out.println(retVal[4]);
    System.out.println(retVal[5]);
    //assertEquals(0.67, retVal[0], 0.01);

  }

  @Test
  public void testGetWeightedCOG() {
    System.out.println("getWeightedCOG");
    int[] input = {
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 250, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };
    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);
    Point2D.Double result = instance.getWeightedCOG(input);
    assertEquals(-0.857, result.x, 0.001);
    assertEquals(-0.857, result.y, 0.001);
  }

  @Test
  public void testGetDist() {
    System.out.println("getDist");
    int[] input = {
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 250, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);
    System.out.println("getDist");
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);
    double result = instance.getDist(CenterOfGravity);
    assertEquals(0.42, result, 0.01);
    //fail("The test case is a prototype.");
  }

  @Test
  public void testGetPerpCheck1() {
    System.out.println("getPerpCheck1");
    int[] input = {
      255, 255, 255, 255, 0, 0,0,
      255, 0, 0, 255, 0, 0,0,
      255, 0, 0, 255, 0, 0,0,
      255, 255, 255, 255, 0, 0,0,
      0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(7);
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);
    double dist = instance.getDist(CenterOfGravity);
    double result = instance.getPerpCheck(CenterOfGravity, input);
    assertEquals(1.0, result, 0.1);

  }

  @Test
  public void testGetPerpCheck2() {
    System.out.println("getPerpCheck2");
    int[] input = {
      255, 255, 255, 255, 0, 255, 255,
      255, 0, 0, 255, 255, 255, 255,
      255, 0, 0, 255, 255, 255, 0,
      255, 255, 255, 255, 127, 0, 0,
      0, 255, 255, 127, 0, 0, 0,
      255, 255, 255, 0, 0, 0, 0,
      255, 255, 0, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(7);
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);

    double dist = instance.getDist(CenterOfGravity);
    double result = instance.getPerpCheck(CenterOfGravity, input);

    assertEquals(0.1, result, 0.1);

  }

  @Test
  public void testGetWhiteDifferential() {
    System.out.println("getWhiteDifferential");
    int[] input = {
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 255, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);
    double centerWhiteness = instance.getCenterWhiteness(input);
    double result = instance.getWhiteDifferential(CenterOfGravity, centerWhiteness, input);

    assertEquals(0.86, result, 0.01);

  }
}
