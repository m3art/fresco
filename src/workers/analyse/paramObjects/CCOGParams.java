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
public class CCOGParams extends CExtractorParams{
    //public int windowSize = 13;
    public int scale;
    public int threshold; 
    public double thresholdq;
    public double distW;
    public double centerWhitenessW;
    public double whiteDiffW;
    public double perpCW;
  
    
    public CCOGParams() {
      this.windowSize = 13;
      this.scale = 256;
      this.threshold = 20; 
      this.distW = 0.25;
      this.centerWhitenessW = 0.25;
      this.whiteDiffW = 0.25;
      this.perpCW = 0.25;
      this.thresholdq = (double)this.threshold/(double)this.scale;

    }
    
    public CCOGParams(int windowSizeI, int scaleI, int thresholdI, double distWI, double centerWhitenessWI, double whiteDiffWI, double perpCWI) {
      this.windowSize = windowSizeI;
      this.scale = scaleI;
      this.threshold = thresholdI; 
      this.distW = distWI;
      this.centerWhitenessW = centerWhitenessWI;
      this.whiteDiffW =  whiteDiffWI;
      this.perpCW = perpCWI;
      this.thresholdq = (double)this.threshold/(double)this.scale;
      normalizeWeights();
    }
    
    public double getRegularization() {
      return (distW-0.25)*(distW-0.25) + (whiteDiffW-0.25)*(whiteDiffW-0.25) + (centerWhitenessW-0.25)*(centerWhitenessW-0.25) + (perpCW-0.25)*(perpCW-0.25);
    
    
    }
    
    public void normalizeWeights() {
      if (distW < 0) distW = 0.001;
      if (perpCW < 0) perpCW = 0.001;
      if (centerWhitenessW < 0) centerWhitenessW = 0.001;
      if (whiteDiffW < 0) whiteDiffW = 0.001;
      double sum = distW + centerWhitenessW + whiteDiffW + perpCW;
      distW /= sum;
      centerWhitenessW /= sum;
      whiteDiffW /= sum;
      perpCW /= sum;
      if (thresholdq < 0) this.thresholdq = 1.0/scale;
      threshold = (int)(thresholdq * scale);
    }
  }
