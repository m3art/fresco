/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse.paramObjects;

import workers.analyse.CLaplacian;

/**
 *
 * @author Jakub
 */
public class CCOGParams extends CExtractorParams {

  private static final int WINDOW_SIZE_DEFAULT = 11;
  /**
   * max pixel value
   */
  public int scale;
  /**
   * extractor threshold
   */
  public int threshold;
  /**
   * threshold/scale
   */
  public double thresholdq;
  /**
   * weight of the distance parameter
   */
  public double distW;
  /**
   * weight of the center whiteness parameter
   */
  public double centerWhitenessW;
  /**
   * weight of the white difference parameter
   */
  public double whiteDiffW;
  /**
   * weight of the perpendicularity check parameter
   */
  public double perpCW;
  /**
   * weight of the mix of the above parameters
   */
  public double mixW;
  public double windowSizeq;

  public enum values {

    def,
    learned
  };

  /**
   * initializes CCOGParams w/ default values
   */
  public CCOGParams(values vals) {
    if (vals == values.def) {
      this.windowSizeq = 1.0;
      this.scale = 256;
      this.threshold = 30;
      this.distW = 0.2;
      this.centerWhitenessW = 0.2;
      this.whiteDiffW = 0.2;
      this.perpCW = 0.2;
      this.mixW = 0.2;
      this.thresholdq = (double) this.threshold / (double) this.scale;
      getWindowSizeFromQ();
    } else if (vals == values.learned) {
      this.windowSizeq = 1.0;
      this.scale = 256;
      this.threshold = 34;
      this.distW = 0.06;
      this.centerWhitenessW = 0.12;
      this.whiteDiffW = 0.26;
      this.perpCW = 0.29;
      this.mixW = 0.27;
      this.normalizeWeights();
      this.thresholdq = (double) this.threshold / (double) this.scale;
      getWindowSizeFromQ();


    }

  }

  public CCOGParams(int windowSizeI, int scaleI, int thresholdI, double distWI, double centerWhitenessWI, double whiteDiffWI, double perpCWI, double mixWI) {
    this.windowSize = windowSizeI;
    this.scale = scaleI;
    this.threshold = thresholdI;
    this.distW = distWI;
    this.centerWhitenessW = centerWhitenessWI;
    this.whiteDiffW = whiteDiffWI;
    this.perpCW = perpCWI;
    this.mixW = mixWI;
    this.thresholdq = (double) this.threshold / (double) this.scale;
    normalizeWeights();
  }

  private static int getWindowSizeDefault() {
    return WINDOW_SIZE_DEFAULT;
  }

  public double getRegularization() {
    return (distW - 0.2) * (distW - 0.2)
            + (whiteDiffW - 0.2) * (whiteDiffW - 0.2)
            + (centerWhitenessW - 0.2) * (centerWhitenessW - 0.2)
            + (perpCW - 0.2) * (perpCW - 0.2)
            + (mixW - 0.2) * (mixW - 0.2);


  }

  public void normalizeWeights() {
    if (distW < 0) {
      distW = 0.001;
    }
    if (perpCW < 0) {
      perpCW = 0.001;
    }
    if (centerWhitenessW < 0) {
      centerWhitenessW = 0.001;
    }
    if (whiteDiffW < 0) {
      whiteDiffW = 0.001;
    }
    if (mixW < 0) {
      mixW = 0.001;
    }
    double sum = distW + centerWhitenessW + whiteDiffW + perpCW + mixW;
    distW /= sum;
    centerWhitenessW /= sum;
    whiteDiffW /= sum;
    perpCW /= sum;
    mixW /= sum;
    if (thresholdq < 0) {
      this.thresholdq = 1.0 / scale;
    }
    threshold = (int) (thresholdq * scale);
  }

  public void getWindowSizeFromQ() {
    windowSize = (int) (windowSizeq * CCOGParams.getWindowSizeDefault());
  }

  public void systemPrint() {
    System.out.println(
            "base: CW: " + centerWhitenessW
            + " d: " + distW
            + " pC: " + perpCW
            + " wD: " + whiteDiffW
            + " thr: " + threshold
            + " mix: " + mixW);
  }
}
