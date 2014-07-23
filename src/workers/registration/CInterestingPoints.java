/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import fresco.swing.CWorkerDialogFactory;
import image.converters.CNormalization;
import image.converters.Crgb2grey;
import info.clearthought.layout.TableLayout;
import workers.analyse.CCannyEdgeDetector;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
import workers.analyse.CCornerDetectorCOG;
import java.awt.geom.Point2D;
import javax.swing.*;
import utils.metrics.*;
import workers.analyse.*;

import workers.analyse.paramObjects.CCOGParams;
import workers.analyse.paramObjects.CCOGParams.values;
import workers.analyse.paramObjects.CEdgerParams;
import workers.analyse.paramObjects.CExtractorParams;
import workers.analyse.paramObjects.CHarrisParams;
import workers.analyse.paramObjects.CLoGParams;

/**
 *
 * @author Jakub
 *
 * TODO
 *
 * eliminovat parametry zkusit benchmarky - vyrobit data a najit slaby mista. *
 * (umoznit uzivateli dodat body a predelat transformaci).
 *
 *
 * regularizzace (n bodu v obdelnikach (nad prahem)]) RANSAC na transformaci
 * evaluace podle -chyby transformace -CC vybranych dvojic
 *
 * nahradit haldovy vyber dvojic backtrackingem podle transformace, tj.
 * backtrackuj, pokud bod je v dosud spoctene tansformaci nerealny
 *
 *
 * TESTY
 *
 * priste: konec rijna / zacatek listopadu
 *
 *
 */
public class CInterestingPoints extends CAnalysisWorker {

  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int topPts = 600; //TODO parametrize
  public BufferedImage input, output;
  /**
   * Class used to find corners
   */
  private Cornerer cornerer;
  /**
   * Class used to find edges
   */
  private Edger edger;
  /**
   * parameters for corners
   */
  public CExtractorParams paramC;
  /**
   * parameters for edger
   */
  public CEdgerParams paramE;
  /**
   * when used by learning algorithm (CCOGLearning.java) id of thread running
   * for text output purposes
   */
  private int threadID;
  /**
   * when used by learning algorithm (CCOGLearning.java) id of image for text
   * output purposes
   */
  private int imageID;
  /**
   * To ensure separation for learning purposes (easier scoring)
   *
   * Using interesting points so close to each other leads to an unstable
   * transformation anyway
   */
  private final int LOC_MAX_DIM = 3;
  /**
   * Points to be found should be distributed over image i. e. if a grid is
   * overlaid, every grid cell should have a similar amount of points if
   * possible (enough interesting points exist) This variable specifies number
   * of cells horizontally number of cells vertically is calculated to have a
   * similar ratio to overall dimensions
   */
  private int GRID_CELL_WIDTH = 100;
  private int GRID_CELL_HEIGHT = 100;

  @Override
  public Type getType() {
    return Type.ANALYSIS;
  }

  public String getTypeName() {
    return "ANALYSIS";
  }

  public static enum Edger {

    sobel,
    LOG
  }

  public static enum Cornerer {

    harris,
    COG,
    random
  }

  @Override
  public String getWorkerName() {
    return "Interesting points search worker";
  }

  public CInterestingPoints(BufferedImage one, Cornerer c, Edger e, CExtractorParams inputParamC, CEdgerParams inputParamE, int tid, int iid) {
    this.input = one;
    this.cornerer = c;
    this.edger = e;
    this.paramC = inputParamC;
    this.paramE = inputParamE;
    this.threadID = tid;
    this.imageID = iid;


  }

  /**
   * Returns the output of and edge detector, converted to greyscale
   */
  private BufferedImage getEdgedGreyscale(BufferedImage input) {

    BufferedImage edgedImage = null;
    BufferedImage greyscale = null;

    if (edger == Edger.sobel) {
      CCannyEdgeDetector sobel = new CCannyEdgeDetector(input, 0.1f, 0.9f);
      try {

        edgedImage = sobel.runPublic();

      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in sobel image edges, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
      sobel.image = null;
      sobel.input = null;
      sobel = null;
    } else if (edger == Edger.LOG) {
      BufferedImage tmp = null;
      CLoGParams p = (CLoGParams) paramE;
      CLaplacian LoG = new CLaplacian(input);
      try {
        tmp = LoG.runPublic();
        edgedImage = tmp;
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in  LoG image edges, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
      LoG.input = null;
      LoG.image = null;
      LoG = null;
    }
    greyscale = (new Crgb2grey()).convert(edgedImage);
    return greyscale;

  }

  /**
   * take image and only return local maxima in it, rest of image is black
   */
  private BufferedImage reduceToLocalMaxima(BufferedImage input) {
    Raster inputData = input.getData();
    int w = input.getWidth();
    int h = input.getHeight();
    BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster tempData = output.getRaster();
    int[] centerpx = new int[3];
    int[] testpx = new int[3];
    int[] blackpx = new int[3];
    blackpx[0] = 0;
    blackpx[1] = 0;
    blackpx[2] = 0;

    //init
    boolean[] isMax = new boolean[w * h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        tempData.setPixel(i, j, centerpx);
        isMax[j * w + i] = false;
      }
    }
    //test if max in region
    for (int i = LOC_MAX_DIM; i < w - LOC_MAX_DIM; i++) {
      for (int j = LOC_MAX_DIM; j < h - LOC_MAX_DIM; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        isMax[j * w + i] = true;

        for (int is = -LOC_MAX_DIM; is <= LOC_MAX_DIM; is++) {
          for (int js = -LOC_MAX_DIM; js <= LOC_MAX_DIM; js++) {
            if ((is == 0) && (js == 0)) {
              continue;
            }
            testpx = inputData.getPixel(i + is, j + js, testpx);
            if (testpx[0] > centerpx[0]) {
              isMax[j * w + i] = false;
            }
          }
        }
      }
    }

    //black out all non-maxima
    for (int x = 0; x < input.getWidth(); x++) {
      for (int y = 0; y < input.getHeight(); y++) {
        if (isMax[y * w + x]) {
          int[] origpx = new int[3];
          origpx = inputData.getPixel(x, y, origpx);
          tempData.setPixel(x, y, origpx);
        } else {
          tempData.setPixel(x, y, blackpx);
        }
      }
    }
    unifyBorderingMaxima(inputData, tempData, isMax);
    temp.setData(tempData);
    return temp;
  }

  /**
   * sub-method of reduceToLocalMaxima in case two local maxima borders, this
   * method blacks out all but one
   */
  private WritableRaster unifyBorderingMaxima(Raster inputData, WritableRaster tempData, boolean isMax[]) {
    int w = inputData.getWidth();
    int h = inputData.getHeight();
    int[] centerpx = new int[3];
    int[] testpx = new int[3];
    int[] blackpx = new int[3];
    blackpx[0] = 0;
    blackpx[1] = 0;
    blackpx[2] = 0;

    //test if multiple equal maxima are adjacent
    //only keep those to the bottom right (arbitrary, can keep any)
    for (int i = LOC_MAX_DIM; i < w - LOC_MAX_DIM; i++) {
      for (int j = LOC_MAX_DIM; j < h - LOC_MAX_DIM; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        for (int is = 0; is <= 1; is++) {
          for (int js = 0; js <= 1; js++) {
            testpx = tempData.getPixel(i + is, j + js, testpx);
            if ((is == 0) && (js == 0)) {
              continue;
            } else if (testpx[0] == centerpx[0]) {
              isMax[j * w + i] = false;
            }

          }
        }
      }
    }
    //black out all but bottom right of contiguous maxima region
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        if (isMax[y * w + x]) {
          int[] origpx = new int[3];
          origpx = inputData.getPixel(x, y, origpx);
          tempData.setPixel(x, y, origpx);
        } else {
          tempData.setPixel(x, y, blackpx);
        }
      }
    }
    return tempData;
  }

  /**
   * adds the best n points from a specified section of the image to the output
   *
   * @param requestedPts number of points to be found in specified section (n)
   * @param TLW: width (X) coord of top left corner of specified section in
   * original image
   * @param TLH: height (Y) coord of top left corner of specified section in
   * original image
   *
   */
  private void addTopPtsToPTQ(WritableRaster corneredInput, int requestedPts, CPointsAndQualities PTQ, int TLW, int TLH) {
    CPointsAndQualities tmp = new CPointsAndQualities();
    int w = corneredInput.getWidth();
    int h = corneredInput.getHeight();
    int intHist[] = new int[256];
    int[] px = new int[3];
    for (int i = 0; i < 256; i++) {
      intHist[i] = 0;
    }
    //logger.info("getting hist");
    for (int a = 0; a < w; a++) {
      for (int b = 0; b < h; b++) {
        corneredInput.getPixel(a, b, px);
        intHist[px[0]]++;
      }
    }
    //logger.info("got hist");
    int ptsSoFar = 0;
    int histIdx = 255;
    while (ptsSoFar < requestedPts) {
      ptsSoFar += intHist[histIdx];
      histIdx--;
    }
    //logger.info("hist idx at: " + histIdx);
    if (histIdx == -1) {
      histIdx = 1;
    }
    for (int a = 0; a < w; a++) {
      for (int b = 0; b < h; b++) {
        corneredInput.getPixel(a, b, px);
        if (px[0] > histIdx) {
          Point2D.Double orig = new Point2D.Double(a, b);
          tmp.addPoint(orig, (double) (px[0]) / (256.0));
        }
      }
    }
    //logger.info(sub+ ": selected " + tmpPts.size());

    while (tmp.size() > requestedPts) {
      double minq = 1;
      int minindex = 0;
      for (int i = 0; i < tmp.size(); i++) {
        if (tmp.getQuality(i) < minq) {
          minq = tmp.getQuality(i);
          minindex = i;
        }
      }
      //logger.info("removing" + minindex + " with quality: +" + minq );
      tmp.removePtq(minindex);
    }
    for (int pt = 0; pt < tmp.size(); pt++) {
      PTQ.addPoint(new Point2D.Double(tmp.getPoint(pt).x + TLW, tmp.getPoint(pt).y + TLH), tmp.getQuality(pt));
    }

  }

  /**
   * Takes the cornered input and selects appropriate points for matching
   */
  private CPointsAndQualities selectPts(WritableRaster corneredInput, int requestedPts, int gridW, int gridH) {
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    int ptsPerSub = requestedPts / (gridW * gridH);
    int wStep = (corneredInput.getWidth() / gridW);
    int hStep = (corneredInput.getHeight() / gridH);
    if ((wStep * hStep) < ptsPerSub) {
      logger.severe("Too many points requsted");
      return tmpPts;
    }
    for (int sub = 0; sub < (gridW * gridH); sub++) {
      int topLeftW = (sub % gridW) * wStep;
      int topLeftH = (int) (sub / gridW) * hStep;
      addTopPtsToPTQ(corneredInput.createWritableChild(topLeftW, topLeftH, wStep, hStep, 0, 0, null), ptsPerSub, tmpPts, topLeftW, topLeftH);
    }
    return tmpPts;
  }

  /**
   * runs the whole thing, including the selection of points for matching i. e.
   * runs the cornering algorithm chooses points according to "cornerity"
   */
  public CPointsAndQualities getPoints(BufferedImage input) {
    return selectPts(getResult(input).getRaster(), topPts, input.getWidth() / GRID_CELL_WIDTH, input.getHeight() / GRID_CELL_HEIGHT);
  }

  /**
   * runs the cornering algorithm on an image cornering algorithm - the one
   * chosen when building the class
   */
  public BufferedImage getResult(BufferedImage input) {
    BufferedImage edgedGreyscale = getEdgedGreyscale(input);
    output = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);

    BufferedImage ret = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);
    if (this.cornerer == Cornerer.harris) {
      CHarrisParams p = (CHarrisParams) paramC;
      //CHarris harris = new CHarris(edgedGreyscale, p);
      CHarris harris = new CHarris(input, p);
      try {
        logger.info("cornerer is harris");
        output = harris.runHarris();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in Harris cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
    } else if (this.cornerer == Cornerer.COG) {
      CCOGParams p = (CCOGParams) paramC;
      CCornerDetectorCOG cog = new CCornerDetectorCOG(edgedGreyscale, output, p);
      try {
        output = cog.publicRun();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in COG cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
      cog.image = null;
      cog.output = null;
      cog = null;
    } else if (this.cornerer == Cornerer.random) {
      int w = input.getWidth();
      int h = input.getHeight();
      int[] newpx = new int[3];
      output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      WritableRaster outputData = output.getRaster();
      for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
          newpx[0] = newpx[1] = newpx[2] = (int) (255.0 * Math.random());
          outputData.setPixel(i, j, newpx);
        }
      }
    }
    ret = reduceToLocalMaxima(output);
    return output;

  }

  @Override
  protected BufferedImage doInBackground() {
    return getResult(input);
  }

  public Double getScore(BufferedImage reference) {

    BufferedImage ret = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    ret = getResult(input);

    WritableRaster rasterOrig = input.getRaster();
    WritableRaster rasterRef = reference.getRaster();
    WritableRaster rasterCorn = ret.getRaster();

    int[] origPx = new int[3];
    /*
     * int[] redPx = new int[3]; redPx[0] = 255; redPx[1] = 0; redPx[2] = 0;
     */
    int[] refPx = new int[3];
    int[] cornPx = new int[3];
    double difference = 0;
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
        difference += pointDif;
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


    /*
     * ratio - How much more important are Corners than Blacks in overall score
     * default set to 1.0 - all corners will together will have same weight as
     * all blacks together
     */
    double blacksVsCorners = 1.0;
    double cornweight = (refBlacks / refCorners) / blacksVsCorners;
    double score = cornweight * cornHits + blackHits;
    //score is normalized between 0 and 1
    score /= (refBlacks) * (1 + (1 / blacksVsCorners));

    if (this.cornerer == Cornerer.COG) {
      CCOGParams pc = (CCOGParams) paramC;
      CLoGParams pe = (CLoGParams) paramE;
      logger.info(
              " s: " + score
              + " cH/rC: " + cornHits + "/" + refCorners
              + " bH/rBlacks: " + blackHits + "/" + refBlacks
              + " tid: " + threadID
              + " iid: " + imageID);
    } else if (this.cornerer == Cornerer.harris) {
      logger.info(" Harris: difference: " + difference / (rasterOrig.getHeight() * rasterOrig.getWidth())
              + " score: " + score
              + " cornHits: " + cornHits
              + " out of refConrers: " + refCorners
              + " blackHits/refBlacks: " + blackHits + "/" + refBlacks);
    }
    rasterCorn = null;
    rasterRef = null;
    rasterOrig = null;
    output = null;
    ret = null;
    return score;

  }

  @Override
  protected void done() {
    this.input = null;
    //this.imageB = null;
    this.output = null;



  }

  public Double publicRun(BufferedImage taggedImage) {
    return getScore(taggedImage);
  }
  private JComboBox cornererInput = new JComboBox();
  private JComboBox edgerInput = new JComboBox();
  private JComboBox cogParamsInput = new JComboBox();
    private JTextField sensitivityInput = new JTextField();


  @Override
  public boolean confirmDialog() {
    String cornererString = (String) cornererInput.getSelectedItem();
    if (cornererString == "Harris") {
      this.cornerer = Cornerer.harris;
      this.paramC = new CHarrisParams(Double.parseDouble(sensitivityInput.getText()));
    }
    if (cornererString == "COG") {
      this.cornerer = Cornerer.COG;
      String cogParamsString = (String) cogParamsInput.getSelectedItem();
      if (cogParamsString == "Learned") {
        this.paramC = new CCOGParams(values.learned);
      }
      if (cogParamsString == "Default") {
        this.paramC = new CCOGParams(values.def);
      }
    }

    String edgerString = (String) edgerInput.getSelectedItem();
    if (edgerString == "Laplacian of Gaussian") {
      this.edger = Edger.LOG;
    }
    if (edgerString == "Sobel") {
      this.edger = Edger.sobel;
    }

    return true;
  }

  @Override
  public JDialog getParamSettingDialog() {
    JPanel content = new JPanel();


    cornererInput.addItem((String) "COG");
    cornererInput.addItem((String) "Harris");

    edgerInput.addItem((String) "Laplacian of Gaussian");
    edgerInput.addItem((String) "Sobel");

    cogParamsInput.addItem((String) "Learned");
    cogParamsInput.addItem((String) "Default");

    TableLayout layout = new TableLayout(new double[]{200, 100}, new double[]{20, 20, 20, 20});
    content.setLayout(layout);

    content.add(new JLabel("Set cornerer: "), "0,0");
    content.add(cornererInput, "1,0");

    content.add(new JLabel("Set edger: "), "0,1");
    content.add(edgerInput, "1,1");

    content.add(new JLabel("Parameters for COG cornerer: "), "0,2");
    content.add(cogParamsInput, "1,2");

        content.add(new JLabel("Set sensitivity parameter for Harris: "), "0,3");
    content.add(sensitivityInput, "1,3");


    return CWorkerDialogFactory.createOkCancelDialog(this, content);
  }
}
