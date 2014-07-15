/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Jakub
 */
public class CDerotationCorrelatorTest extends CDerotationCorrelator {

  public CDerotationCorrelatorTest() {
  }

  @Test
  public void testEnergyRings() {
    System.out.println("energyRings");
        int w = 5;
    int h = 5;
    double[][] region1 = {
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0},
      {127, 127, 255, 127, 127},
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0}
    };

    int rings = 2;
    
    
    double[] result = CDerotationCorrelator.energyRings(region1, w, h, rings, 4);
    for (int r = 0; r < rings; r++) {
      System.out.println(result[r]);
    }
    assertEquals(1020.0, result[0], 0.01);
    assertEquals(764.0, result[1], 0.01);
  }

  @Test
  public void testGetEnergyRingCorrelation1() {
    System.out.println("getEnergyRingCorrelation");
    int w = 5;
    int h = 5;
    double[][] region1 = {
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0},
      {127, 127, 255, 127, 127},
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0}
    };
    double[][] region2 = {
      {0, 0, 0, 0, 0},
      {0, 255, 255, 255, 0},
      {0, 255, 0, 255, 0},
      {0, 255, 255, 255, 0},
      {0, 0, 0, 0, 0}
    };
    int rings = w;
    int measurementsPerRing = 4 * w;
    double result = CDerotationCorrelator.getEnergyRingCorrelation(region1, region2, w, h, rings, measurementsPerRing);
    assertTrue(result < 0.5);
  }

  @Test
  public void testGetEnergyRingCorrelation2() {
    System.out.println("getEnergyRingCorrelation");
    int w = 5;
    int h = 5;
    double[][] region1 = {
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0},
      {127, 127, 255, 127, 127},
      {0, 0, 255, 0, 0},
      {0, 0, 255, 0, 0}
    };
    double[][] region2 = {
      {0, 0, 127, 0, 0},
      {0, 0, 127, 0, 0},
      {255, 255, 255, 255, 255},
      {0, 0, 127, 0, 0},
      {0, 0, 127, 0, 0}
    };
    double[][] region3 = {
      {0, 0, 0, 0, 0},
      {0, 255, 255, 255, 0},
      {0, 255, 0, 255, 0},
      {0, 255, 255, 255, 0},
      {0, 0, 0, 0, 0}
    };

    int rings = w;
    int measurementsPerRing = 4 * w;

    double result = CDerotationCorrelator.getEnergyRingCorrelation(region1, region2, w, h, rings, measurementsPerRing);
    assertEquals(1.0, result, 0.05);
  }

  
}
