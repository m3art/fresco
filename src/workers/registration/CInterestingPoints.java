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
import utils.metrics.*;



/**
 *
 * @author Jakub
 * 
 * 
 * 
 * TODO
 * 
 * eliminovat parametry
 * zkusit benchmarky - vyrobit data a najit slaby mista. * 
 * (umoznit uzivateli dodat body a predelat transformaci).
 * 
 */

public class CInterestingPoints extends CSupportWorker<CPointPairs, CPointAndTransformation[]> {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int topPts = 200; //TODO parametrize
  private BufferedImage imageA, imageB;
  private CPointPairs pairsOut;
  private CPointsAndQualities ptqA, ptqB;
  private int[] black;
  private int[] white;  
  private int windowSize;

  @Override
  public String getWorkerName() {
    return "Interesting points search worker";
  }

  public CInterestingPoints(BufferedImage one, BufferedImage two) {
    this.imageA = one;
    this.imageB = two;
    this.ptqA = new CPointsAndQualities();
    this.ptqB = new CPointsAndQualities();
    this.pairsOut = new CPointPairs();
    
    this.windowSize = 3;
    this.black = new int[3];
    this.black[0] = 0;
    this.black[1] = 0;
    this.black[2] = 0;
    this.white = new int[3];
    this.white[0] = 255;
    this.white[1] = 255;
    this.white[2] = 255;
    
  }
  
  private CPointsAndQualities getPoints(BufferedImage input) {
    
    /* Get edge detector output, greyscale it */
    
    BufferedImage edgedImage = null;
    // = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    BufferedImage greyscale = (new Crgb2grey()).oneBandImage(input);
    
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    CPointsAndQualities retPts = new CPointsAndQualities();
    CCannyEdgeDetector edgeMaker  = new CCannyEdgeDetector(input, 0.1f, 0.9f);
    try {
      edgeMaker.execute();
      //edgedImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
      edgedImage = (BufferedImage) edgeMaker.get();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(new JFrame(), "Exception in image edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    
    //BufferedImage greyscaleEdged = (new Crgb2grey()).oneBandImage(edgedImage);
    Raster in1 = CNormalization.normalize((new Crgb2grey()).convert(edgedImage), 128, 64).getData();
    BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster out = output.getRaster();
    
    
    //actual corner detection
    
    /* ENTROPY TEST BEGINS */
    int bestSize = 3;
    double bestMeasure = 0;
    
    for(int i = 3; i < 21; i += 2) {
      
      // number of test fields 

      int testFields = 50;      
      double testFieldEntropy = 0;
      double entropySum = 0;
      double measure = 0;
      int [] pixel = new int[1];
      int [] colorFrequencies = new int[256];
      
      for(int t = 0; t < testFields; t++) {
        
        //initialize variables for new test field        
        testFieldEntropy = 0;
        for (int color = 0; color < 256; color++) {
          colorFrequencies[color] = 0;
        }
        // get test field 
        int x = (int)(Math.random()*(input.getWidth()-i-1));
        int y = (int)(Math.random()*(input.getHeight()-i-1));
        
        
        //fill out frequencies of colors (intensities on grey scale)
        for (int px = x; px < x+i; px++) {
          for (int py = y; py < y + i; py++) {
            greyscale.getData().getPixel(px, py, pixel);
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
      logger.info("size:" + i + " measure: " + measure + " entropy: "+entropySum/(double)testFields);
      //search for best window size
      if (measure >= bestMeasure) { 
        bestMeasure = measure;
        bestSize = i;
      }

    }
    logger.info("bestSize: " + bestSize);
     /* VARIANCE TEST ENDS    */
    
    
    /* CORNER DETECTION */
    
    //initialize helper variables
    int [] current = null;
    int [] newpx = new int[3];
    double intensity = 0;
    double [] retval = new double[5];
    windowSize += bestSize/2;
    
    CCornerDetectorCOG CornerDetector = new CCornerDetectorCOG(bestSize);
    int shift = (int)(CCornerDetectorCOG.size/2);
    
    for (int x = 0; x < in1.getWidth()-CCornerDetectorCOG.size; x++) {
      for (int y = 0; y < in1.getHeight()-CCornerDetectorCOG.size; y++) {
        //get square from image
        current = in1.getSamples(x, y, CCornerDetectorCOG.size, CCornerDetectorCOG.size, 0, current);
        //get it's "cornerity"
        retval = CornerDetector.getCOG(current, false);
        // save
        intensity = retval[0];
        //set newpx to grey accordingly
        newpx[0] = newpx[1] = newpx[2] = (int)(intensity * 256);
        //set pixel in output to "cornerity"
        out.setPixel(x+shift, y+shift, newpx);
        //update progress
        setProgress(100 * (x * ((int) in1.getHeight()) + y) / ((int) (in1.getWidth() * in1.getHeight())));
      }
    }
    
    //normalization
    double maxIntensity = 0;
    int [] px = new int[3];
    for (int a = 0; a < in1.getWidth()-CCornerDetectorCOG.size; a++) {
      for (int b = 0; b < in1.getHeight()-CCornerDetectorCOG.size; b++) {
        out.getPixel(a+shift, b+shift, px);
        if (px[0]> maxIntensity) maxIntensity = px[0];
      }
    }
    double q = 256/maxIntensity;
    int newint = 0;
    newpx[1] = 0;
    newpx[2] = 0;
    for (int a = 0; a < in1.getWidth()-CCornerDetectorCOG.size; a++) {
      for (int b = 0; b < in1.getHeight()-CCornerDetectorCOG.size; b++) {
        out.getPixel(a+shift, b+shift, px);
        intensity = px[0];
        newint = (int)(q*intensity)%256;
        newpx[0] = newpx[1] = newpx[2] = newint ;
        
        if (newint > 128) {
          Point2D.Double orig = new Point2D.Double(a+shift, b+shift);
          tmpPts.addPoint(orig, (double)newint/(256.0));
        }
        out.setPixel(a+shift, b+shift, newpx);
      }
    }
    output.setData(out);
    
    while (retPts.size() < topPts) {
      logger.info("choosing: " + retPts.size() + " from " + tmpPts.size());
      if (tmpPts.size() == 0) break;
      double maxq = 0;
      int maxIndex = -1;
      for(int i = 0; i < tmpPts.size(); i++) {
        if (tmpPts.getQuality(i) >= maxq) {
          maxq = tmpPts.getQuality(i);
          maxIndex = i;          
        }
      }
      tmpPts.removePtq(maxIndex);
      retPts.addPoint(tmpPts.getPoint(maxIndex), tmpPts.getQuality(maxIndex));
    
    }
    return retPts;
  }

  
  @Override
  protected CPointPairs doInBackground() {
    
    //run previous method on each input image
    // = extract interesting points / corners from each image
    ptqA = this.getPoints(imageA);
    ptqB = this.getPoints(imageB);
    
    
    int requestedPairs = 100; 
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


