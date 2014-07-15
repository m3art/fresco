/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import java.awt.image.BufferedImage;
import workers.registration.CImageScore;
import workers.registration.CInterestingPoints;

/**
 *
 * @author Jakub
 */
public class CInterestingPointsThread extends Thread{
  public CInterestingPoints intPoints;
  public CImageScore scorer;
  public double result;
  
  
  public CInterestingPointsThread(CInterestingPoints input, BufferedImage reference) {
    intPoints = input;
    this.scorer = new CImageScore(input, reference);
  
  }
  
  @Override
  public void run() {
    result = scorer.getScore();
    
  
  }
  
}
