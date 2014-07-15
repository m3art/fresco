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

/**
 *
 * @author Jakub
 */
public class CCornerDetectorCOGTest {

  public CCornerDetectorCOGTest() {
  }

  @Test
  public void testGetCOG() {
    int[] input = {
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 255, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };
      
    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);

    double[] retVal = instance.getCOG(input);
    instance.param.centerWhitenessW = 0.2;
    instance.param.distW = 0.2;
    instance.param.perpCW = 0.2;
    instance.param.whiteDiffW = 0.2;
    instance.param.mixW = 0.2;

    assertEquals(0.71, retVal[0], 0.01);
    
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
    System.out.println("getPerpCheck1");
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
      250, 250, 250, 0, 0,
      250, 0, 250, 0, 0,
      250, 250, 250, 0, 0,
      0, 0, 0, 0, 0,
      0, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);
    double dist = instance.getDist(CenterOfGravity);
    double result = instance.getPerpCheck(CenterOfGravity, dist, input);
    assertEquals(1.0, result, 0.1);
    
  }

  @Test
  public void testGetPerpCheck2() {
    System.out.println("getPerpCheck2");
    int[] input = {
      250, 250, 250, 0, 255,
      250, 0, 250, 255, 0,
      250, 250, 250, 0, 0,
      0, 255, 0, 0, 0,
      255, 0, 0, 0, 0
    };

    CCornerDetectorCOG instance = new CCornerDetectorCOG(5);
    Point2D.Double CenterOfGravity = instance.getWeightedCOG(input);
    double dist = instance.getDist(CenterOfGravity);
    double result = instance.getPerpCheck(CenterOfGravity, dist, input);

    assertEquals(0.15, result, 0.1);
   
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
