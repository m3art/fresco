/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import java.awt.image.BufferedImage;
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
     int [] inputArray = {99, 115, 132, 107, 99, 103, 103, 111, 119, 107, 119, 99, 111, 111, 103, 115, 115, 107, 99, 115, 111, 111, 99, 115, 111, 103, 99, 107, 107, 107, 123, 107, 119, 99, 119, 111, 103, 107, 128, 111, 115, 103, 119, 111, 103, 115, 103, 107, 103, 115, 103, 119, 103, 115, 115, 111, 115, 107, 103, 107, 119, 111, 119, 107, 123, 115, 123, 119, 107, 115, 103, 119, 111, 115, 107, 115, 107, 107, 111, 103, 107, 107, 119, 111, 123, 107, 107, 111, 99, 111, 99, 123, 111, 107, 115, 115, 115, 107, 115, 107, 115, 103, 115, 99, 107, 111, 107, 107, 115, 119, 99, 103, 99, 115, 119, 107, 107, 107, 111, 115, 128};
    CCornerDetectorCOG COG = new CCornerDetectorCOG(11);
    
    double [] actRetVal = COG.getCOG(inputArray, false);
    double [] expRetVal = {0.02123204161973389, 0.46484375, 0.022832728679430793, 0.5, 0.56640625};  
    for (int i = 0; i < actRetVal.length; i++) {
			assertEquals(actRetVal[i], expRetVal[i], 0.0000001);
		}
  }
          
          
          
          
  
}
