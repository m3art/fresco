/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import fresco.CData;
import fresco.swing.CBlendComponent;
import image.converters.CNormalization;
import image.converters.Crgb2grey;
import workers.analyse.CCannyEdgeDetector;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import workers.CImageWorker;
import fresco.action.IAction.RegID;
import workers.analyse.CCornerDetectorCOG;
import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import workers.segmentation.*;
import java.awt.image.*;
import java.awt.*;
import javax.swing.*;
import support.CSupportWorker;
import workers.registration.refpointga.CPointAndTransformation;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;
import utils.metrics.*;
import workers.analyse.CHarris;
import workers.analyse.CLaplacian;



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
public class CInterestingPoints extends CSupportWorker<Double, Void> {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int topPts = 160; //TODO parametrize
  private BufferedImage imageA, imageB;
  private CPointPairs pairsOut;
  private CPointsAndQualities ptqA, ptqB;
  private int[] black;
  private int[] white;  
  private int windowSize;
  private int shift;
  private boolean harris;
  
  
  
  @Override
	public Type getType() {
		return Type.SUPPORT;
	}

	public String getTypeName() {
		return "SUPPORT";
	}


  
  @Override
  public String getWorkerName() {
    return "Interesting points search worker";
  }

  public CInterestingPoints(BufferedImage one, BufferedImage two, boolean harris) {
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
    this.harris = harris;
    //logger.info("created new CIP");
    
    
  }
  
  public BufferedImage getEdgedGreyscale(BufferedImage input) {
    
    BufferedImage edgedImage = null;
    BufferedImage greyscale = null;
    CCannyEdgeDetector edgeMaker  = new CCannyEdgeDetector(input, 0.1f, 0.9f);
    CLaplacian LoG = new CLaplacian(input);
    try {
      edgeMaker.execute();
      edgedImage = edgeMaker.get();
      
      //edgeMaker.execute();
      //edgedImage = (BufferedImage) edgeMaker.get();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(new JFrame(), "Exception in image edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    greyscale = (new Crgb2grey()).convert(edgedImage);
    return greyscale;
  
  }
  
  public int getBestWindowSize(Raster greyInput, int testFields, int lowerBound, int upperBound) {
    int bestSize = lowerBound;
    double bestMeasure = 0.0;
    
    for(int i = lowerBound; i < upperBound; i += 2) {
      
      double testFieldEntropy = 0;
      double entropySum = 0;
      double measure = 0;
      int [] pixel = new int[3];
      int [] colorFrequencies = new int[256];
      
      for(int t = 0; t < testFields; t++) {
        
        //initialize variables for new test field        
        testFieldEntropy = 0;
        for (int color = 0; color < 256; color++) {
          colorFrequencies[color] = 0;
        }
        // get test field 
        int x = (int)(Math.random()*(greyInput.getWidth()-i-1));
        int y = (int)(Math.random()*(greyInput.getHeight()-i-1));
        
        
        //fill out frequencies of colors (intensities on grey scale)
        for (int px = x; px < x+i; px++) {
          for (int py = y; py < y + i; py++) {
            greyInput.getPixel(px, py, pixel);
            colorFrequencies[pixel[0]]++;           
          }
        }
        
        //calculate the entropy itself
        for (int color = 0; color < 256; color++) {
          if (colorFrequencies[color] != 0){
            testFieldEntropy -=((double)colorFrequencies[color]/(double)(i*i)) * Math.log((double)colorFrequencies[color]/(double)(i*i));
          }
        }
        entropySum += testFieldEntropy;
      }
      //execution time is a factor, and entropy will hit a plateau sooner or later
      // -> add size of field as a negative factor
      
      measure = ((entropySum/(double)testFields)-((double)i/38));
      //logger.info("size:" + i + " measure: " + measure + " entropy: "+entropySum/(double)testFields);
      //search for best window size
      if (measure >= bestMeasure) { 
        bestMeasure = measure;
        bestSize = i;
      }

    }
    logger.info("bestSize: " + bestSize);
    return bestSize;
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
  
  public CPointsAndQualities selectPts(WritableRaster corneredInput, int requestedPts) {
    int[] intHist = new int[256];
    int[] px = new int[3];
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    
    int subW = 10;
    int subH = 8;
    int ptsPerSub = requestedPts/(subW*subH);
    int breaks = 0;
    for (int sub = 0; sub < (subW*subH); sub++) {
      
      int wStep = (corneredInput.getWidth()/subW);
      int hStep = (corneredInput.getHeight()/subH);
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
  
  private CPointsAndQualities getPoints(BufferedImage input) {
    
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    CPointsAndQualities retPts = new CPointsAndQualities();
    
    /* Get edge detector output, greyscale it */
    logger.info("exception in edging");
    BufferedImage greyscaleEdged = getEdgedGreyscale(input);
    logger.info("exception in greyscaling");
    
    //actual corner detection
    logger.info("exception in window size selection");
    
    windowSize = 7;
    shift = windowSize/2;
    
    /* CORNER DETECTION */
    
    
    //WritableRaster cornered = getCornerity(greyscaleEdged.getData());
    logger.info("exception in pts selection");
    BufferedImage greyscaleImage = (new Crgb2grey()).convert(input);
    //Raster greyscale = ((new Crgb2grey()).convert(input)).getData();
    windowSize = getBestWindowSize(greyscaleImage.getData(), 50, 3, 21);
    
    shift = windowSize/2;
    
    CHarris cornerer = new CHarris(windowSize);
    BufferedImage cornered = cornerer.execute(greyscaleImage);
    
    retPts = selectPts(cornered.getRaster(), topPts);

   //output.setData(out);
   
    return retPts;
  }

    
  @Override
  //protected BufferedImage _doInBackground() {
  protected Double doInBackground() {
        
    //return getPoints(imageA);
    windowSize = 11;
    BufferedImage greyscaleImage = (new Crgb2grey()).convert(imageA);
    windowSize = getBestWindowSize(greyscaleImage.getData(), 200, 3, 21);
    BufferedImage tmp = getEdgedGreyscale(imageA);
    WritableRaster tmp2 = getCornerity(tmp.getData());
    BufferedImage ret = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_INT_RGB);
    
    
    //logger.info("got to new CHarris");
    CHarris cornerer = new CHarris(windowSize);
    
    if(!harris) ret.setData(tmp2);
    else ret = cornerer.execute(greyscaleImage);
    WritableRaster rasterOrig = imageA.getRaster();
    WritableRaster rasterRef = imageB.getRaster();
    WritableRaster rasterCorn = ret.getRaster();
  
    //WritableRaster rasterCorn = tmp2;
    int [] origPx = new int [3];
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
        if (refPx[0] < 20) refBlacks++;
                
        if ((refPx[0] < 20) && (cornPx[0] == 0)) blackHits++;
        if ((cornPx[0] > 10) && (refPx[0] > 20)) {
          cornHits += (double)refPx[0]/255.0;
          refPx[0] = 249;
          refPx[1] = 0;
          refPx[2] = 0;
          rasterRef.setPixel(i, j, refPx);
        }
        
      }
      
    }
    double cornweight = refBlacks / refCorners;
    double score = cornweight*((double)cornHits/(double)refCorners) + ((double)blackHits/(double)refBlacks);
    
    if (!harris)
    logger.info(" COG: difference: " + difference/(rasterOrig.getHeight()*rasterOrig.getWidth()) 
                + " score: " + score 
                + " cornHits: " + cornHits 
                + " out of refConrers: " + refCorners 
                + " blackHits/refBlacks: " + blackHits + "/" + refBlacks);
    
    else
    logger.info(" Harris: difference: " + difference/(rasterOrig.getHeight()*rasterOrig.getWidth()) 
                + " score: " + score 
                + " cornHits: " + cornHits 
                + " out of refConrers: " + refCorners 
                + " blackHits/refBlacks: " + blackHits + "/" + refBlacks);
    
    
    return score;
    //return ret;
           
  }
  
  //@Override
  protected CPointPairs _doInBackground() {
  
  //protected CPointPairs doInBackground() {
    
    //run previous method on each input image
    // = extract interesting points / corners from each image
    ptqA = this.getPoints(imageA);
    ptqB = this.getPoints(imageB);
    
    
    int requestedPairs = 160; 
    //TODO: Add to paramSettingDialog
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


