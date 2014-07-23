/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.colour.CBilinearInterpolation;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import workers.CImageWorker;
import workers.analyse.paramObjects.CCOGParams;
import utils.CIdxMapper;

/**
 * Detects corners using Center of Gravity displacement and other params
 *
 * @author Jakub
 */
public class CCornerDetectorCOG extends CAnalysisWorker {

  public CCOGParams param;
  public BufferedImage image;
  public BufferedImage output;
  protected static CIdxMapper map = new CIdxMapper();
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  /**
   * Expected input: unrolled windowSize*windowSize matrix in row-major format,
   * each cell holds image brightness in that particular pixel, VALUES OF
   * INTENSITY BETWEEN 1 AND scale output: double[5] containing values between 0
   * and 1 [0]: the combined "cornerity", [1]: a copy of the output from the
   * edge detector [2]: distance of COG from center, [3]: difference of
   * brightness between COG and image center [4]: perpendicular check - darkness
   * of pixels on an axis perpendicular to the vector(Center, COG) Negative
   * values of output signify an error.
   *
   */
  public CCornerDetectorCOG(int matsize) {
    param = new CCOGParams(CCOGParams.values.def);
    param.windowSize = matsize;

  }

  public CCornerDetectorCOG(BufferedImage input, BufferedImage out, CCOGParams inputParams) {
    image = input;
    param = inputParams;
    output = out;
  }

  public CCornerDetectorCOG(BufferedImage input) {
    image = input;
    int h = image.getHeight();
    int w = image.getWidth();
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
  }

  public CCornerDetectorCOG(CCOGParams inputParams) {
    param = inputParams;

  }

  public CCornerDetectorCOG(BufferedImage input, CCOGParams inputParams) {
    param = inputParams;
    image = input;
    int h = image.getHeight();
    int w = image.getWidth();
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

  }

  @Override
  public String getWorkerName() {
    return "COG corner detector";
  }

  /**
   * normalizes the input writable raster - linearly stretches it to fill full
   * range writes the result to output 0-255 
   * assumes one band image stored in the first band for 
   * lack of BufferedImage.TYPE_INT_GRAYSCALE
   *
   * Also takes care of thresholding the image (using param.threshold)
   */
  protected WritableRaster linearNormalize(WritableRaster input) {
    double maxIntensity = 0;
    int[] px = new int[3];
    int h = input.getHeight();
    int w = input.getWidth();
    WritableRaster outputData = output.getRaster();
    for (int x = (int) (param.windowSize / 2); x < w - (int) (param.windowSize / 2); x++) {
      for (int y = (int) (param.windowSize / 2); y < h - (int) (param.windowSize / 2); y++) {
        input.getPixel(x, y, px);
        if (px[0] > maxIntensity) {
          maxIntensity = px[0];
        }
      }
    }
    double q = 255.0 / maxIntensity;
    int newint = 0;
    int[] newpx = new int[3];
    newpx[0] = 0;
    newpx[1] = 0;
    newpx[2] = 0;

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        outputData.getPixel(x, y, px);
        newint = (int) (q * px[0]);

        if (newint < param.threshold) {
          newint = 0;
        }
        if (map.isNearEdge(x, y, w, h, (int) (param.windowSize / 2))) {
          newint = 0;
        }
        newpx[0] = newpx[1] = newpx[2] = newint;
        outputData.setPixel(x, y, newpx);
      }
    }
    return outputData;
  }

  @Override
  protected BufferedImage doInBackground() {
    int h = image.getHeight();
    int w = image.getWidth();
    Raster imageData = image.getRaster();
    int[] current = null;
    int[] newpx = new int[3];
    double[] retval = new double[5];
    //get cornerity
    WritableRaster outputData = output.getRaster();
    for (int x = 0; x < w - param.windowSize; x++) {
      for (int y = 0; y < h - param.windowSize; y++) {
        current = imageData.getSamples(x, y, param.windowSize, param.windowSize, 0, current);
        retval = getCOG(current);
        //set newpx to grey accordingly
        newpx[0] = newpx[1] = newpx[2] = (int) (retval[0] * 256);
        outputData.setPixel(x + (int) (param.windowSize / 2), y + (int) (param.windowSize / 2), newpx);
      }
    }
    this.linearNormalize(outputData);
    current = null;
    outputData = null;
    return this.output;
  }

  @Override
  protected void done() {
    image = null;
    output = null;
  }

  public BufferedImage publicRun() {
    return doInBackground();
  }

  /**
   * returns the position of the center of gravity, pushed (weighted) towards
   * bright pixels if they are on a cont. line from center of frame
   */
  protected Point2D.Double getWeightedCOG(int[] input) {
    Point2D.Double result = new Point2D.Double(0, 0);
    double val = 0;
    int xdim, ydim, iD, jD;
    //i, j are coordinates in input square subimage
    //X and j are HORIZONTAL coords
    //Y and i are VERTICAL coords
    double totalVal = 0;
    for (int i = 0; i < param.windowSize; i++) {
      for (int j = 0; j < param.windowSize; j++) {
        //ydim, xdim are coordiantes if the center pixel is considered [0, 0]
        //iD, jD are coordinates in the original input, they correspond to pixels halfway between the center pixel and i, j
        ydim = i - (int) (param.windowSize / 2);
        iD = (int) (ydim / 2) + (int) (param.windowSize / 2);
        xdim = j - (int) (param.windowSize / 2);
        jD = (int) (xdim / 2) + (int) (param.windowSize / 2);
        //val is a geometric average of brightness in the selected pixel and in a pixel halfway towards the center
        //val = (double) input[(i * param.windowSize) + j] / param.scale;
        val = ((double) input[(i * param.windowSize) + j] / param.scale) * ((double) input[(iD * param.windowSize) + jD] / param.scale);
        totalVal += val;
        //the center of gravity is pushed toward a bright pixel, if it's on a continuous line
        //that is, each bright point is weighted into the COG calculation not only by its brightness and distance, 
        //but also by the brightness of a point halfway toward the center of the square
        result.x += xdim * val;
        result.y += ydim * val;

      }
    }
    result.x /= totalVal;
    result.y /= totalVal;
    return result;
  }

  /**
   * calculates the distance of center of gravity from center of image region
   */
  protected double getDist(Point2D.Double CenterOfGravity) {
    double dist = Math.sqrt(CenterOfGravity.x * CenterOfGravity.x + CenterOfGravity.y * CenterOfGravity.y) / Math.sqrt(2 * (int) (param.windowSize / 2) * (int) (param.windowSize / 2));
    return dist;
  }

  /**
   * calculates the perpendicularity check that is, are pixels on the line
   * perpendicular to the center of gravity - center of region not edges?
   */
  protected double getPerpCheck(Point2D.Double CenterOfGravity, double dist, int[] input) {
    Point2D.Double CenterOfGravityCheck = CenterOfGravity;

    double perpendicularCheck = 0;
    if (dist > 0) {
      //crawl just over the edge by adding the COG vector
      while ((Math.abs(CenterOfGravityCheck.x) < (int) (param.windowSize / 2) - 1) && (Math.abs(CenterOfGravityCheck.y) < (int) (param.windowSize / 2) - 1)) {
        CenterOfGravityCheck.y += CenterOfGravity.y;
        CenterOfGravityCheck.x += CenterOfGravity.x;

      }
      //step back to "just inside the box"
      while ((Math.abs(CenterOfGravityCheck.x) > (int) (param.windowSize / 2)) || (Math.abs(CenterOfGravityCheck.y) > (int) (param.windowSize / 2))) {
        CenterOfGravityCheck.y -= CenterOfGravity.y;
        CenterOfGravityCheck.x -= CenterOfGravity.x;
      }
      //first perpendicular edge
      Point2D.Double checkPointA = new Point2D.Double(CenterOfGravityCheck.x * (-1.0) + (int) (param.windowSize / 2), CenterOfGravityCheck.y + (int) (param.windowSize / 2));
      double valA = CBilinearInterpolation.getValue(checkPointA, input, param.windowSize, param.windowSize) / param.scale;

      //second perpendicular edge
      Point2D.Double checkPointB = new Point2D.Double(CenterOfGravityCheck.x + (int) (param.windowSize / 2), (CenterOfGravityCheck.y * -1.0) + (int) (param.windowSize / 2));
      double valB = CBilinearInterpolation.getValue(checkPointB, input, param.windowSize, param.windowSize) / param.scale;
      perpendicularCheck = 1 - ((valA + valB) / 2);
    }
    return perpendicularCheck;

  }

  /**
   * returns the intensity of the pixel in the center
   */
  protected double getCenterWhiteness(int[] input) {
    return (double) (input[(int) ((param.windowSize * param.windowSize) / 2)]) / (param.scale);
  }

  /**
   * returns the difference between the intensity of the COG and the center
   * pixel value between 0 and 1 => values 0 - 0.5 imply
   */
  protected double getWhiteDifferential(Point2D.Double centerOfGravity, double centerWhiteness, int[] input) {
    Point2D.Double COGIndex = new Point2D.Double(centerOfGravity.x + (int) (param.windowSize / 2), centerOfGravity.y + (int) (param.windowSize / 2));
    if (Math.abs(COGIndex.x) >= param.windowSize - 1 || Math.abs(COGIndex.y) >= param.windowSize - 1) {
    }
    double centerOfGravityWhiteness = CBilinearInterpolation.getValue(COGIndex, input, param.windowSize, param.windowSize) / param.scale;

    double whiteDifferential = (double) (1 + centerWhiteness - centerOfGravityWhiteness) * param.scale;
    whiteDifferential /= 2 * param.scale;
    return whiteDifferential;

  }

  /**
   * returns all the desired parameters and their weighted combinations
   */
  public double[] getCOG(int[] input) {
    //Only run on circle around center - to decrease dependence on rotation.
    for (int i = 0; i < param.windowSize; i++) {
      for (int j = 0; j < param.windowSize; j++) {
        int radius = param.windowSize / 2;
        double distanceFromCenter = Math.sqrt((i - radius) * (i - radius) + (j - radius) * (j - radius));
        if (distanceFromCenter > radius) {
          input[i * param.windowSize + j] = 0;
        }
      }
    }



    Point2D.Double CenterOfGravity = getWeightedCOG(input);

    double dist = getDist(CenterOfGravity);

    double perpCheck = getPerpCheck(CenterOfGravity, dist, input);

    double centerWhiteness = getCenterWhiteness(input);

    double whiteDifferential = getWhiteDifferential(CenterOfGravity, centerWhiteness, input);

    double[] ret = new double[6];
    ret[0] = (dist * param.distW + perpCheck * param.perpCW + centerWhiteness * param.centerWhitenessW + whiteDifferential * param.whiteDiffW + (dist * centerWhiteness) * param.mixW);
    ret[1] = centerWhiteness;
    ret[2] = dist;
    ret[3] = whiteDifferential;
    ret[4] = perpCheck;
    ret[5] = dist * centerWhiteness * perpCheck * whiteDifferential;
    return ret;
  }
}
