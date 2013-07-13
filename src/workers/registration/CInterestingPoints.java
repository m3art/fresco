/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import image.converters.CNormalization;
import image.converters.Crgb2grey;
import workers.analyse.CCannyEdgeDetector;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
import workers.analyse.CCornerDetectorCOG;
import java.awt.geom.Point2D;
import utils.metrics.*;
import workers.analyse.*;

import workers.analyse.paramObjects.CCOGParams;
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
 * eliminovat parametry
 * zkusit benchmarky - vyrobit data a najit slaby mista. * 
 * (umoznit uzivateli dodat body a predelat transformaci).
 * 
 * 
 * regularizzace (n bodu v obdelnikach (nad prahem)])
 * RANSAC na transformaci
 * evaluace podle
 *  -chyby transformace
 *  -CC vybranych dvojic
 * 
 * nahradit haldovy vyber dvojic backtrackingem podle transformace,
 * tj. backtrackuj, pokud bod je v dosud spoctene tansformaci nerealny
 * 
 * 
 * TESTY
 * 
 * priste: konec rijna / zacatek listopadu
 * 
 * 
 */

//public class CInterestingPoints extends CImageWorker<Double, Void> {
public class CInterestingPoints extends CAnalysisWorker {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int topPts = 1200; //TODO parametrize
  private BufferedImage imageA, imageB, output;
  private CPointPairs pairsOut;
  private CPointsAndQualities ptqA, ptqB;
  private CLocalMaximiser maximiser;
  private int[] black;
  private int[] white;  
  private int windowSize;
  private int shift;
  private Cornerer cornerer;
  private Edger edger;
  public CExtractorParams paramC;
  public CEdgerParams paramE;
  private int tid;
  private int iid;
  
  
  
  
  
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

  public CInterestingPoints(BufferedImage one, BufferedImage two, Cornerer c, Edger e, CExtractorParams inputParamC, CEdgerParams inputParamE, int tid, int iid) {
    this.imageA = one;
    this.imageB = two;
    this.ptqA = new CPointsAndQualities();
    this.ptqB = new CPointsAndQualities();
    this.pairsOut = new CPointPairs();
    
    this.windowSize = 7;
    this.shift = 0;
    this.black = new int[3];
    this.black[0] = 0;
    this.black[1] = 0;
    this.black[2] = 0;
    this.white = new int[3];
    this.white[0] = 255;
    this.white[1] = 255;
    this.white[2] = 255;
    this.cornerer = c;
    this.edger = e;
    this.maximiser = new CLocalMaximiser(7);
    this.paramC = inputParamC;
    this.paramE = inputParamE;
    this.tid = tid;
    this.iid = iid;
    
   
  }
  
  public BufferedImage getEdgedGreyscale(BufferedImage input) {
    
    BufferedImage edgedImage = null;
    BufferedImage greyscale = null;
    
    if (edger == Edger.sobel) {
      CCannyEdgeDetector sobel  = new CCannyEdgeDetector(input, 0.1f, 0.9f);
      try {

      edgedImage = sobel.runPublic();

      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in sobel image edges, stopped");
        logger.log(Level.SEVERE, e.getMessage());
        //JOptionPane.showMessageDialog(new JFrame(), "Exception in image edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }
      sobel.image = null;
      sobel.input = null;
      sobel = null;
    }
    else if (edger == Edger.LOG) {
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
    /*
    int [] origPx = new int[3];
    int [] black = new int[3];
    black[0] = black[1] = black[2] = 0;
    int threshold = 30;
    for (int i = 0; i < imageA.getWidth(); i++) {
        for (int j = 0; j < imageA.getHeight(); j++) {
           origPx  = greyscale.getRaster().getPixel(i, j, origPx);
           if (origPx[0] < threshold) {
              greyscale.getRaster().setPixel(i, j, black);
           }
        }
      }
      
    */
      
    return greyscale;
    
  }

  
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
            tmp.addPoint(orig, (double)(px[0])/(256.0));
          }
        }
      }
      //logger.info(sub+ ": selected " + tmpPts.size());
      
      while(tmp.size() > requestedPts) {
        double minq = 1;
        int minindex = 0;
        for (int i = 0; i < tmp.size(); i++) {
          if (tmp.getQuality(i) <  minq) {
            minq = tmp.getQuality(i);
            minindex = i;
          }
        }
        //logger.info("removing" + minindex + " with quality: +" + minq );
        tmp.removePtq(minindex);   
      }
      for (int pt = 0; pt < tmp.size(); pt++) {
        PTQ.addPoint(new Point2D.Double(tmp.getPoint(pt).x+TLW, tmp.getPoint(pt).y+TLH), tmp.getQuality(pt));
      }
  
  }
  
  private CPointsAndQualities selectPts(WritableRaster corneredInput, int requestedPts, int gridW, int gridH) {
    int[] intHist = new int[256];
    int[] px = new int[corneredInput.getNumBands()];
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    
    int ptsPerSub = requestedPts/(gridW*gridH);
    
    int breaks = 0;
    int wStep = (corneredInput.getWidth()/gridW);
    int hStep = (corneredInput.getHeight()/gridH);
    
    if ((wStep * hStep) < ptsPerSub) {
       logger.severe("Too many points requsted");
       return tmpPts;
    }
    
    for (int sub = 0; sub < (gridW*gridH); sub++) {
      int topLeftW = (sub % gridW) * wStep;
      int topLeftH = (int)(sub / gridW) * hStep;
      
      
      addTopPtsToPTQ(corneredInput.createWritableChild(topLeftW, topLeftH, wStep, hStep, 0, 0, null), ptsPerSub, tmpPts, topLeftW, topLeftH);
      
    }
  
   
    return tmpPts;
  }
  
  public CPointsAndQualities getPoints(BufferedImage input) {
    CPointsAndQualities retPts = new CPointsAndQualities();
    retPts = selectPts(getResult(input).getRaster(), topPts, 8, 6);
  
    return retPts;
    
  }
  
  
  public BufferedImage getResult(BufferedImage input) {
  BufferedImage edgedGreyscale = getEdgedGreyscale(input);
    output = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);
    
    BufferedImage ret = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);
    if (this.cornerer == Cornerer.harris) {
      CHarrisParams p = (CHarrisParams) paramC;
      CHarris harris = new CHarris(edgedGreyscale, p);
      try {
        logger.info("cornerer is harris");
        output = harris.publicRun();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in Harris cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
    }
    
    else if(this.cornerer == Cornerer.COG) {
      CCOGParams p = (CCOGParams)paramC;
      CCornerDetectorCOG cog = new CCornerDetectorCOG(edgedGreyscale, output, p);
      try{ 
        output = cog.publicRun();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in COG cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
      cog.image = null;
      cog.output = null;
      cog = null;
    }
    
    else if(this.cornerer == Cornerer.random) {
      int w = imageA.getWidth();
      int h = imageA.getHeight();
      int [] newpx = new int[3];
      output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      WritableRaster outputData = output.getRaster();
      for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
          newpx[0] = newpx[1] = newpx[2] = (int)(255.0*Math.random());
          outputData.setPixel(i, j, newpx);
        }
      }
    }
    ret = maximiser.runMaxima(output);
    return ret;
  
  }
  
    
  @Override
  protected BufferedImage doInBackground() {
      return getResult(imageA);
  }
  
  
  public Double getScore() {
    
    BufferedImage ret = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_INT_RGB);
    ret = getResult(imageA);
    
    WritableRaster rasterOrig = imageA.getRaster();
    WritableRaster rasterRef = imageB.getRaster();
    WritableRaster rasterCorn = ret.getRaster();
  
    //WritableRaster rasterCorn = tmp2;
    int [] origPx = new int [3];
    int [] redPx = new int [3];
    redPx[0] = 255;
    redPx[1] = 0;
    redPx[2] = 0;
    int [] refPx = new int [3];
    int [] cornPx = new int [3];
    double difference = 0;
    double pointDif = 0;
    double cornHits = 0;
    int blackHits = 0;
    int refCorners = 0;
    int refBlacks = 0;
    for (int i = 0; i < imageA.getWidth(); i++) {
      for (int j = 0; j < imageA.getHeight(); j++) {
        cornPx = rasterCorn.getPixel(i, j, cornPx);
        refPx = rasterRef.getPixel(i, j, refPx);
        pointDif = Math.abs(cornPx[0] - refPx[0]);
        difference += pointDif;
        if (refPx[0] > 250) refCorners++;
        if (refPx[0] <= 250) refBlacks++;
                
        if ((cornPx[0] > 0) && (refPx[0]) > 0) {
          cornHits += (double)refPx[0]*(double)cornPx[0]/(255.0*255.0);
          //rasterRef.setPixel(i, j, redPx);
        }
        if ((refPx[0] <= 250) && (cornPx[0]) == 0) {
          blackHits++;
        }
      }
    }
    
    double BlacksVsCorners = 1.0;
    double cornweight = (refBlacks / refCorners)/BlacksVsCorners;
    double score = cornweight*cornHits + blackHits;
    score /= (refBlacks)* (1 + (1/BlacksVsCorners)) ;
    
    if (this.cornerer == Cornerer.COG) {
    CCOGParams pc = (CCOGParams) paramC;
    CLoGParams pe = (CLoGParams) paramE;
    logger.info(
                " s: " + score 
                + " cH/rC: " + cornHits + "/" + refCorners
                + " bH/rBlacks: " + blackHits + "/" + refBlacks
                + " tid: " + tid
                + " iid: " + iid);
    }
    
    else if(this.cornerer == Cornerer.harris) {
    logger.info(" Harris: difference: " + difference/(rasterOrig.getHeight()*rasterOrig.getWidth()) 
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
    this.imageA = null;
    this.imageB = null;
    this.output = null;
    this.maximiser = null;
    
  
  }
  
  
  public Double publicRun() {
    return getScore();
  }
}
  