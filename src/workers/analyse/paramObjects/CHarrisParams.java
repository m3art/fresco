/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse.paramObjects;

/**
 *
 * @author Jakub
 */
public class CHarrisParams extends CExtractorParams {
  public double sigma;
  public double sensitivity;
  public int threshold;
  
  public CHarrisParams(int windowSizeI, double sigmaI, double sensitivityI, int thresholdI) {
    this.windowSize = windowSizeI;
    this.sigma = sigmaI;
    this.sensitivity = sensitivityI;
    this.threshold = thresholdI;
  }
  
  public CHarrisParams() {
    this.windowSize = 19;
    this.sigma = 1.5;
    this.threshold = 0;
    this.sensitivity = 0.0005;
  }
  
}
