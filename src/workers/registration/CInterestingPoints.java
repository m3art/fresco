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
  public static int topPts = 360; //TODO parametrize
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
    
    //logger.info("created new CIP");
  }
  
  public BufferedImage getEdgedGreyscale(BufferedImage input) {
    
    BufferedImage edgedImage = null;
    BufferedImage greyscale = null;
    
    if (edger == Edger.sobel) {
      CCannyEdgeDetector sobel  = new CCannyEdgeDetector(input, 0.1f, 0.9f);
      try {
      //sobel.execute();
      edgedImage = sobel.runPublic();
      
      //edgeMaker.runHarris();
      //edgedImage = (BufferedImage) sobel.get();
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
         
         CCannyEdgeDetector sobel  = new CCannyEdgeDetector(tmp, 0.1f, 0.9f);
         //edgedImage = sobel.runPublic();
         edgedImage = tmp;

       } catch (Exception e) {
         logger.log(Level.SEVERE, "exception in  LoG image edges, stopped");
         logger.log(Level.SEVERE, e.getMessage());
        //JOptionPane.showMessageDialog(new JFrame(), "Exception in image edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
       }
            
       LoG.input = null;
       LoG.image = null;
       LoG = null;
       
    }
    
    greyscale = (new Crgb2grey()).convert(edgedImage);
    
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
    return greyscale;
  
  }

  
  public WritableRaster getCornerity(Raster input) {
    int [] current = null;
    int [] newpx = new int[3];
    int [] centerpx = new int[3];
    int [] testpx = new int[3];
    double intensity = 0;
    double [] retval = new double[5];
    
    BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster out = output.getRaster();
    
    CCornerDetectorCOG  CornerDetector = new CCornerDetectorCOG(windowSize);
    
    //int shift = (int)(CCornerDetectorCOG.size/2);
    
    for (int x = 0; x < input.getWidth()-windowSize; x++) {
      for (int y = 0; y < input.getHeight()-windowSize; y++) {
        input.getPixel(x+shift, y+shift, newpx);
        //logger.info("found: " + newpx[0]);
        //get square from image
        current = input.getSamples(x, y, windowSize, windowSize, 0, current);
        //get it's "cornerity"
        retval = CornerDetector.getCOG(current, false);
        // save
        
        //set newpx to grey accordingly
        newpx[0] = newpx[1] = newpx[2] = (int)(retval[0]*256);
        //newpx[0] = (int)(retval[1] * 256);
        //newpx[1] = (int)(retval[2] * 256);
        //newpx[2] = (int)(retval[3] * 256);
        //set pixel in output to "cornerity"
        //if (newpx[0] > 48)  
          out.setPixel(x+shift, y+shift, newpx);
        //else out.setPixel(x+shift, y+shift, black);
        
        //update progress
        setProgress(100 * (x * ((int) input.getHeight()) + y) / ((int) (input.getWidth() * input.getHeight())));
      }
    }
    int locmaxdim = 3;
    for (int i = locmaxdim; i < input.getWidth()-locmaxdim; i++) {
      for (int j = locmaxdim; j < input.getHeight()-locmaxdim; j++) {
        centerpx = out.getPixel(i, j, centerpx);
        //boolean isMax = true;
        for(int is = -locmaxdim; is <= locmaxdim; is++) {
          for(int js = -locmaxdim; js <= locmaxdim; js++) {
            if ((is == 0) && (js == 0)) continue;
            testpx = out.getPixel(i+is, j+js, testpx);
            if (testpx[0] > centerpx[0]) {
              centerpx[1] = centerpx[2] = 0;
              out.setPixel(i, j, centerpx);
            }
          }
        }
        //if (H[i*w+j] > (maxH/120)) H[i*w+j] = maxH; 
       
      }
    }
    for (int x = 0; x < input.getWidth()-windowSize; x++) {
      for (int y = 0; y < input.getHeight()-windowSize; y++) {
        centerpx = out.getPixel(x, y, centerpx);
        if (centerpx[1] == 0) centerpx[0] = 0;
        out.setPixel(x, y, centerpx);
      }
    }
   
    double maxIntensity = 0;
    int [] px = new int[3];
    for (int a = 0; a < input.getWidth()-windowSize; a++) {
      for (int b = 0; b < input.getHeight()-windowSize; b++) {
        out.getPixel(a+shift, b+shift, px);
        if (px[0]> maxIntensity) maxIntensity = px[0];
      }
    }
    double q = 255/maxIntensity;
    int newint = 0;
    newpx[1] = 0;
    newpx[2] = 0;
    for (int a = 0; a < input.getWidth()-windowSize; a++) {
      for (int b = 0; b < input.getHeight()-windowSize; b++) {
        out.getPixel(a+shift, b+shift, px);
        intensity = px[0];
        newint = (int)(q*intensity);
        newpx[0] = newpx[1] = newpx[2] = newint ;
        out.setPixel(a+shift, b+shift, newpx);
      }
    }
    
    return out;
    
    
  }
  
  private CPointsAndQualities selectPts(WritableRaster corneredInput, int requestedPts) {
    int[] intHist = new int[256];
    int[] px = new int[3];
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    
    int subW = 4;
    int subH = 3;
    
    int ptsPerSub = requestedPts/(subW*subH);
    
    int breaks = 0;
    int wStep = (corneredInput.getWidth()/subW);
    int hStep = (corneredInput.getHeight()/subH);
    
    if ((wStep * hStep) < ptsPerSub) {
       logger.severe("Too many points requsted");
       return tmpPts;
    }
    
    for (int sub = 0; sub < (subW*subH); sub++) {
      
      
      int topLeftW = (sub % subW) * wStep;
      int topLeftH = (int)(sub / subW) * hStep;
      int origSize = tmpPts.size();


      logger.info("new it: " + topLeftW + "x" + topLeftH + " to " + (topLeftW+wStep) + "x" + (topLeftH+hStep));

      for (int i = 0; i < 256; i++) {
        intHist[i] = 0;
      }
      logger.info("getting hist");
      for (int a = topLeftW; a < topLeftW+wStep; a++) {
        for (int b = topLeftH; b < topLeftH+hStep; b++) {
          corneredInput.getPixel(a, b, px);
          intHist[px[0]]++;
        }
      }
      logger.info("got hist");
      int ptsSoFar = 0;
      int histIdx = 255;
      while (ptsSoFar < ptsPerSub) {
        //logger.info("hist Idx: " + histIdx + " pts: " + intHist[histIdx]);
        ptsSoFar += intHist[histIdx];
        histIdx--;    
      }
      logger.info("hist idx at: " + histIdx);
      if (histIdx == -1) {
        breaks++;
        continue;
        
      }
      for (int a = topLeftW; a < topLeftW+wStep; a++) {
        for (int b = topLeftH; b < topLeftH+hStep; b++) {
          corneredInput.getPixel(a, b, px);
          if (px[0] > histIdx) {
            Point2D.Double orig = new Point2D.Double(a, b);
            tmpPts.addPoint(orig, (double)(px[0])/(256.0));
          }
        }
      }
      logger.info(sub+ ": selected " + tmpPts.size());
      
      while(tmpPts.size() > ptsPerSub*(sub+1-breaks)) {
        double minq = 1;
        int minindex = 0;
        for (int i = Math.max(0, origSize-1); i < tmpPts.size(); i++) {
          
          if (tmpPts.getQuality(i) <  minq) {
            minq = tmpPts.getQuality(i);
            minindex = i;
          }
        }
        logger.info("removing" + minindex + " with quality: +" + minq );
        tmpPts.removePtq(minindex);   
      }
    }
  
    return tmpPts;
  }
  
  public CPointsAndQualities getPoints(BufferedImage input) {
    CPointsAndQualities retPts = new CPointsAndQualities();
    retPts = selectPts(getResult(input).getRaster(), topPts);
    return retPts;
    
  }
  
  
  public BufferedImage getResult(BufferedImage input) {
  BufferedImage edgedGreyscale = getEdgedGreyscale(input);
    output = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);
    
    BufferedImage ret = new BufferedImage(edgedGreyscale.getWidth(), edgedGreyscale.getHeight(), BufferedImage.TYPE_INT_RGB);
    if (this.cornerer == Cornerer.harris) {
      CHarrisParams p = (CHarrisParams) paramC;
      CHarris harris = new CHarris(edgedGreyscale, p);
      //CHarris harris = new CHarris((new Crgb2grey()).convert(imageA), p);
      try {
        logger.info("cornerer is harris");
        output = harris.publicRun();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in Harris cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
      }
      //ret = output;
      ret = maximiser.runMaxima(output);
    }
    
    else if(this.cornerer == Cornerer.COG) {
      CCOGParams p = (CCOGParams)paramC;
      //p.normalizeWeights();
      CCornerDetectorCOG cog = new CCornerDetectorCOG(edgedGreyscale, output, p);
      try{ 
        output = cog.publicRun();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "exception in COG cornering, stopped");
        logger.log(Level.SEVERE, e.getMessage());
        //JOptionPane.showMessageDialog(new JFrame(), "Exception in COG corner detecting\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }
      //ret = output;
      
      ret = maximiser.runMaxima(output);
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
      //ret = output;
      ret = maximiser.runMaxima(output);
    }
    //logger.info("returning from getResult()");
    return ret;
    
  
  
  }
  
    
  @Override
  protected BufferedImage doInBackground() {
      return getResultWithReds();
  }
  public BufferedImage getResultWithReds() {
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
          rasterRef.setPixel(i, j, redPx);
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
    
    rasterCorn = null;
    rasterRef = null;
    rasterOrig = null;
    output = null;
    
    
    //return score;
    return ret;
           
  
  
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
    
    
      /*logger.info(
                " s: " + score 
                + " cH/rC: " + cornHits + "/" + refCorners
                + " bH/rBlacks: " + blackHits + "/" + refBlacks
                //+ "c: " + cornerer.toString() + " e: " + edger.toString()
                + " cW: " + pc.centerWhitenessW 
                + " d: " + pc.distW 
                + " pC: " + pc.perpCW 
                + " wD: " + pc.whiteDiffW 
                + " mix: " + pc.mixW 
                + " thr: " + pc.threshold 
                + " gSg: " + pe.gaussSigma
                + " gSe: " + pe.gaussSize
                
                + " diff: " + difference/(rasterOrig.getHeight()*rasterOrig.getWidth()) 
                
                );*/
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
    //greyscaleImage = null;
    //edgedGreyscale = null;
    output = null;
    
    ret = null;
    return score;
    //return ret;
           
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
  
  //@Override
  protected CPointPairs _doInBackground() {
  
  //protected CPointPairs doInBackground() {
    
    //run previous method on each input image
    // = extract interesting points / corners from each image
    ptqA = this.getPoints(imageA);
    ptqB = this.getPoints(imageB);
    int requestedPairs = 160; 
    double correlations[][] = new double[ptqA.size()][ptqB.size()];
    
    Raster rasterA = CNormalization.normalize((new Crgb2grey()).convert(imageA), 128, 64).getData();
    Raster rasterB = CNormalization.normalize((new Crgb2grey()).convert(imageB), 128, 64).getData();
    int corrWindowSize = windowSize;
    CCovarianceMetric CV = new CCovarianceMetric(imageA, imageB, 2*corrWindowSize+1, CAreaSimilarityMetric.Shape.RECTANGULAR);
    CCrossCorrelationMetric CC = new CCrossCorrelationMetric(imageA, imageB, 2*corrWindowSize+1, CAreaSimilarityMetric.Shape.RECTANGULAR);
    for (int i = 0; i < ptqA.size() ; i++) {
      setProgress(100 * i / ptqA.size());
      Point2D.Double a = ptqA.getPoint(i);
      int AX = (int)a.x;
      int AY = (int)a.y;
      boolean AisEdge = false;
      if ((AX < (2*corrWindowSize+1)) || (AX >= rasterA.getWidth()-(2*corrWindowSize+1)) || (AY < (2*corrWindowSize+1)) || (AY >= rasterA.getHeight()-(2*corrWindowSize+1))) {
        AisEdge = true;
      }
      else {
        double currentA[] = null;
        double currentB[] = null;
        currentA = rasterA.getSamples(AX, AY, 2*corrWindowSize+1, 2*corrWindowSize+1, 0, currentA);
        if (ptqB.size() ==  0) break;
        for (int j = 0; j < ptqB.size() ; j++) {
          boolean BisEdge = false;
          Point2D.Double b = ptqB.getPoint(j);
          int BX = (int)b.x;
          int BY = (int)b.y;
          if ((BX < (2*corrWindowSize+1)) || (BX >= rasterA.getWidth()-(2*corrWindowSize+1)) || (BY < (2*corrWindowSize+1)) || (BY >= rasterA.getHeight()-(2*corrWindowSize+1))) {
            BisEdge = true;
          }
          
          if (AisEdge || BisEdge) {
        
            correlations[i][j] = 0.0;
          }
          else {
            
            currentB = rasterB.getSamples(BX-corrWindowSize, BY-corrWindowSize, 2*corrWindowSize+1, 2*corrWindowSize+1, 0, currentB);
            /*double ssd = 0;
            
            for (int k = 0; k < Math.pow(2*corrWindowSize+1, 2); k++) {
              ssd += (double)Math.pow(currentA[k] - currentB[k], 2)/65536;       
            }
            ssd /= Math.pow(2*corrWindowSize+1, 2);*/
            double cc = CC.getDistance(a, b);
            double cv = CV.getDistance(a, b);
            correlations[i][j] = cc*cv;
          }
        }
      }
    }
    
    logger.info("correlations filled in");
    for (int i = 0; i < requestedPairs; i++) {
      double maxCor = 0.0;
      int maxA = -1;
      int maxB = -1;
      double max2Cor = 0.0;
      int max2A = -1;
      int max2B = -1;
      
      
      
      //eager algorithm for selecting pairs
      //take the two best remaining pairs according to correlation
      //calculate their distance from pairs already in use
      //factor it into the "quality" of the pairs
      //if the one with the worse correlation score wins with the distance factored in, 
      // do not add the one with the better correlation (beacuse it's too close
      // to other already used pairs, and therefore mostly useless for registration)
      
      //NOTE: all variables with '2' in the name belong to the second best pair
      // (according to correlation)
      
      //find top two pairs
      
      for (int a = 0; a < ptqA.size(); a++) {
        for (int b = 0; b < ptqB.size(); b++) {
          if (correlations[a][b] > maxCor) {
            max2A = maxA;
            max2B = maxB;
            max2Cor = maxCor;              
            maxA = a;
            maxB = b;
            maxCor = correlations[a][b];
          }
          else if (correlations[a][b] > max2Cor) {
            max2A = a;
            max2B = b;
            max2Cor = correlations[a][b];
          }
        }
      }
      
      //test: ran out of pairs with any correlation whatsoever
      // or ran out of interesting points in either image
      if ((maxCor <= 0.0) || (maxA == -1) || (maxB == -1) || (max2A == -1) || (max2B == -1) || (max2Cor <= 0.0)) break;
      
      
      double distMax = 0;
      double dist2Max = 0;
      int j = 0;
      for (; j < pairsOut.size(); j++) {
        Point2D.Double pairPtA = pairsOut.getOrigin(j);
        Point2D.Double pairPtB = pairsOut.getProjected(j);
        Point2D.Double maxPtA = ptqA.getPoint(maxA);
        Point2D.Double maxPtB = ptqB.getPoint(maxB);
        Point2D.Double max2PtA = ptqA.getPoint(max2A);
        Point2D.Double max2PtB = ptqB.getPoint(max2B);
        distMax += Math.sqrt( (pairPtA.x-maxPtA.x)*(pairPtA.x-maxPtA.x)+(pairPtA.y-maxPtA.y)*(pairPtA.y-maxPtA.y));
        distMax += Math.sqrt( (pairPtB.x-maxPtB.x)*(pairPtB.x-maxPtB.x)+(pairPtB.y-maxPtB.y)*(pairPtB.y-maxPtB.y));
        dist2Max += Math.sqrt( (pairPtA.x-max2PtA.x)*(pairPtA.x-max2PtA.x)+(pairPtA.y-max2PtA.y)*(pairPtA.y-max2PtA.y));
        dist2Max += Math.sqrt( (pairPtB.x-max2PtB.x)*(pairPtB.x-max2PtB.x)+(pairPtB.y-max2PtB.y)*(pairPtB.y-max2PtB.y));
      }
      
      distMax /= Math.sqrt(rasterA.getHeight()*rasterA.getHeight() + rasterA.getWidth()*rasterA.getWidth());
      dist2Max /= Math.sqrt(rasterA.getHeight()*rasterA.getHeight() + rasterA.getWidth()*rasterA.getWidth());
      
      if (j!=0) {
        distMax /=2*j;
        dist2Max /=2*j;
      }
      
      // factor in the distance from other pairs - if the current pair is 
      // better then the next best one even with the distance factored in,
      // use the current one
      double scoreMax = correlations[maxA][maxB]*(distMax);
      double score2Max = correlations[max2A][max2B]*(dist2Max);
      
      if (scoreMax >= score2Max){
        
        pairsOut.addPointPair(ptqA.getPoint(maxA), ptqB.getPoint(maxB));
        for (int a = 0; a < ptqA.size(); a++) {
          correlations[a][maxB] = 0.0;

        }
        for (int b = 0; b < ptqB.size(); b++) {
          correlations[maxA][b] = 0.0;
        }
      }
      
      //otherwise dismiss the current one and carry on to the next.
      else {
        //no pair added -> therefore control cycle must repeat, but will skip 
        //the maxA-maxB pair, because with distance factored in, the max2A-max2B
        //was better
        correlations[maxA][maxB] = 0.0;      
        i -= 1;
      }
      
      
    }
    
    //pairsOut.origins = ptqA.points;
    //pairsOut.projected = ptqB.points;
    logger.info("correlations done");
    
    return pairsOut;
  }
}


