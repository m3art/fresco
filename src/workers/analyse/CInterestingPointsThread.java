/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import workers.registration.CInterestingPoints;

/**
 *
 * @author Jakub
 */
public class CInterestingPointsThread extends Thread{
  public CInterestingPoints intPoints;
  public double result;
  
  
  public CInterestingPointsThread(CInterestingPoints input) {
    intPoints = input;
    result = 0.0;
  
  }
  
  @Override
  public void run() {
    result = intPoints.publicRun();
  
  
  }
    
  
  
  
  
  
}
