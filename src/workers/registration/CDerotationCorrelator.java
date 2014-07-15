/*
 *
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 *
 */
package workers.registration;

import image.colour.CBilinearInterpolation;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.logging.Logger;
import utils.CIdxMapper;
import workers.CImageWorker;
import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.analyse.paramObjects.CLoGParams;

/**
 * Correlates points supplied by CInterestingPoints in images 
 * uses the
 * surroundings of the supplied points, and the correlation is rotationally
 * invariant
 *
 * @author Jakub
 */
public class CDerotationCorrelator extends CImageWorker<double[][], Void> {

  public CPointsAndQualities ptsA, ptsB;
  private static BufferedImage inputA, inputB;
  private static final int WINDOW_SIZE = 11;
  
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  public CDerotationCorrelator(BufferedImage reference, BufferedImage sensed) {
    inputA = reference;
    inputB = sensed;
  }

  public CDerotationCorrelator() {
  }

  /**
   * For internal purposes - holds index of point in ptsA/ptsB and their
   * surroundings in the images
   */
  protected class PointAndSurround {

    public boolean valid;
    public int index;
    public double[][] surround;

    public PointAndSurround(int index, double[][] surround, boolean valid) {
      this.valid = valid;
      this.surround = surround;
      this.index = index;
    }
  }

  @Override
  public String getWorkerName() {
    return "Derotation Correlator";
  }

  /**
   * Used for testing purposes only
   *
   * @param angle between zero and 2*Math.PI
   */
  protected static double[][] rotate(double[][] region, int w, int h, double angle) {
    //angle *= -1.0 + 2 * Math.PI;
    double[][] ret = new double[w][h];
    double a0 = Math.cos(angle);
    double a1 = Math.sin(angle);
    double b0 = -1 * Math.sin(angle);
    double b1 = a0;
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        int relI = i - w / 2;
        int relJ = j - h / 2;
        double newI = a0 * relI + b0 * relJ + w / 2;
        double newJ = a1 * relI + b1 * relJ + h / 2;
        if ((newI > 0) && (newI < w - 1) && (newJ > 0) && (newJ < h - 1)) {
          ret[i][j] = CBilinearInterpolation.getValue(new Point2D.Double(newI, newJ), region);
        } else {
          ret[i][j] = 0.0;
        }
      }
    }

    return ret;
  }

  /**
   * Calculates the energy (sum of values) on evenly spaced rings around the
   * center
   */
  public static double[] energyRings(double[][] region, int w, int h, int rings, int measurementsPerRing) {
    double[] energy = new double[rings];
    double totalEnergy = 0.0;
    double ringRadiusUnit = (int) ((w / 2)) / (double) rings;
    for (int r = 0; r < rings; r++) {
      energy[r] = 0.0;
      for (int mpr = 0; mpr < measurementsPerRing; mpr++) {
        double angle = ((double) mpr / (double) measurementsPerRing) * Math.PI * 2.0;
        double a0 = Math.cos(angle);
        double a1 = Math.sin(angle);
        double b0 = -1 * Math.sin(angle);
        double b1 = a0;
        double x1 = 0;
        double x2 = (double) r * ringRadiusUnit;

        double newX1 = a0 * x1 + b0 * x2 + w / 2;
        double newX2 = a1 * x1 + b1 * x2 + h / 2;

        if ((newX1 > 0) && (newX1 < w - 1) && (newX2 > 0) && (newX2 < h - 1)) {

          energy[r] += CBilinearInterpolation.getValue(new Point2D.Double(newX1, newX2), region);
        } else {
          energy[r] += 0.0;
        }
      }
      totalEnergy += energy[r];
    }
    for (int r = 0; r < rings; r++) {
      //energy[r] /= totalEnergy;
    }
    return energy;
  }

  /**
   * Calculates the correlation (value between 0 and 1) of region1 and region2
   * given the parameters (rings, mpr)
   */
  public static double getEnergyRingCorrelation(double[][] region1, double[][] region2, int w, int h, int rings, int measurementsPerRing) {

    double[] energyVector1 = energyRings(region1, w, h, rings, measurementsPerRing);
    double[] energyVector2 = energyRings(region2, w, h, rings, measurementsPerRing);

    double diff = 0.0;
    double maxDiff = 255;
    for (int i = 0; i < rings; i++) {
      diff += Math.abs(energyVector1[i] - energyVector2[i]);
    }
    diff /= (rings * measurementsPerRing);

    diff += 1;

    return 1 - (Math.log10(diff) / Math.log10(maxDiff));
  }

  /**
   * Returns ArrayList of surroundings of points in pts from raster
   */
  protected ArrayList<PointAndSurround> getSurrounds(CPointsAndQualities pts, WritableRaster raster) {
    ArrayList<PointAndSurround> List = new ArrayList<PointAndSurround>();
    int surroundDim = WINDOW_SIZE / 2;
    for (int i = 0; i < pts.size(); i++) {
      Point2D.Double a = pts.getPoint(i);
      int AX = (int) a.x;
      int AY = (int) a.y;
      int[] currentA = null;
      double[][] currentAdouble = new double[WINDOW_SIZE][WINDOW_SIZE];
      if (!CIdxMapper.isNearEdge(a, raster.getWidth(), raster.getHeight(), surroundDim)) {
        currentA = raster.getSamples(AX - surroundDim, AY - surroundDim, WINDOW_SIZE, WINDOW_SIZE, 0, currentA);
        for (int x = 0; x < WINDOW_SIZE; x++) {
          for (int y = 0; y < WINDOW_SIZE; y++) {
            currentAdouble[y][x] = currentA[y * WINDOW_SIZE + x];
          }
        }
        List.add(new PointAndSurround(i, currentAdouble, true));
      } else {
        List.add(new PointAndSurround(i, null, false));
      }
    }
    return List;
  }

  @Override
  public double[][] doInBackground() {
    //Initialize extractor
    CCOGParams pc = new CCOGParams(CCOGParams.values.learned);
    //pc.centerWhitenessW = 0.13;
    //pc.distW = 0.05;
    //pc.perpCW = 0.4;
    //pc.whiteDiffW = 0.2;
    //pc.mixW = 0.21;
    //pc.normalizeWeights();
    //pc.windowSize = 23;
    CLoGParams pl = new CLoGParams();
    CInterestingPoints extractor;
    CHarrisParams ph = new CHarrisParams();
    extractor = new CInterestingPoints(inputA, CInterestingPoints.Cornerer.harris, CInterestingPoints.Edger.LOG, ph, pl, 0, 0);

    //get points from extractor
    logger.info("getting pts from A");
    this.ptsA = extractor.getPoints(inputA);

    extractor.input = inputB;
    logger.info("getting pts from B");
    this.ptsB = extractor.getPoints(inputB);
    logger.info("getting correlation");

    //look at surroundings of extracted points and calculate correlation
    //based on energy rings
    WritableRaster rasterA = inputA.getRaster();
    WritableRaster rasterB = inputB.getRaster();
    //int dim = WINDOW_SIZE / 2;
    double[][] correlations = new double[ptsA.size()][ptsB.size()];
    ArrayList<PointAndSurround> AList = getSurrounds(ptsA, rasterA);
    ArrayList<PointAndSurround> BList = getSurrounds(ptsB, rasterB);
    for (int i = 0; i < AList.size(); i++) {
      for (int j = 0; j < BList.size(); j++) {
        if (AList.get(i).valid && BList.get(j).valid) {
          correlations[AList.get(i).index][BList.get(j).index] = getEnergyRingCorrelation(AList.get(i).surround, BList.get(j).surround, WINDOW_SIZE, WINDOW_SIZE, WINDOW_SIZE / 2, 4 * WINDOW_SIZE);
        }
      }
    }

    return correlations;
  }

  @Override
  public String getTypeName() {
    return "REGISTRATION";
  }

  @Override
  public Type getType() {
    return Type.REGISTRATION;
  }
}
