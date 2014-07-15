/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

/**
 *
 * @author Jakub
 */
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import workers.CImageWorker;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CLoGParams;

public class CImageScore extends CImageWorker<Double, Void> {

  private CInterestingPoints intPts;
  private BufferedImage reference;
  
  /**
   * ratio - How much more important are Corners than Blacks in overall score
   * default set to 1.0 - all corners will together will have same weight as all
   * blacks together
   */
  public static final double BLACKS_VS_CORNERS = 1.0;

  public CImageScore(CInterestingPoints intPts, BufferedImage reference) {
    this.intPts = intPts;
    this.reference = reference;
  }

  @Override
  public String getTypeName() {
    return "REGISTRATION";
  }

  @Override
  public String getWorkerName() {
    return "Image Scoring Worker";
  }

  @Override
  protected Double doInBackground() {
    return getScore();
  }

  public Double getScore() {
    BufferedImage input = intPts.input;
    BufferedImage ret = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    ret = intPts.getResult(input);

    WritableRaster rasterOrig = input.getRaster();
    WritableRaster rasterRef = reference.getRaster();
    WritableRaster rasterCorn = ret.getRaster();

    int[] origPx = new int[3];
    /*
     * int[] redPx = new int[3]; redPx[0] = 255; redPx[1] = 0; redPx[2] = 0;
     */
    int[] refPx = new int[3];
    int[] cornPx = new int[3];
    
    double pointDif = 0;
    double cornHits = 0;
    int blackHits = 0;
    int refCorners = 0;
    int refBlacks = 0;
    for (int i = 0; i < input.getWidth(); i++) {
      for (int j = 0; j < input.getHeight(); j++) {
        cornPx = rasterCorn.getPixel(i, j, cornPx);
        refPx = rasterRef.getPixel(i, j, refPx);
        pointDif = Math.abs(cornPx[0] - refPx[0]);
        //difference += pointDif;
        if (refPx[0] > 250) {
          refCorners++;
        }
        if (refPx[0] <= 250) {
          refBlacks++;
        }

        if ((cornPx[0] > 0) && (refPx[0]) > 0) {
          cornHits += (double) refPx[0] * (double) cornPx[0] / (255.0 * 255.0);
          //rasterRef.setPixel(i, j, redPx);
        }
        if ((refPx[0] <= 250) && (cornPx[0]) == 0) {
          blackHits++;
        }
      }
    }



    double cornweight = (refBlacks / refCorners) / BLACKS_VS_CORNERS;
    double score = cornweight * cornHits + blackHits;
    //score is normalized between 0 and 1
    score /= (refBlacks) * (1 + (1 / BLACKS_VS_CORNERS));
    rasterCorn = null;
    rasterRef = null;
    rasterOrig = null;
    return score;

  }

  @Override
  public Type getType() {
    return Type.REGISTRATION;
  }
}
