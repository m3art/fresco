/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import utils.metrics.*;



/**
 *
 * @author Jakub
 */
public class CInterestingPoints extends CSupportWorker<CPointPairs, CPointAndTransformation[]> {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int treshhold = 195;
  private BufferedImage imageA, imageB;
  private CPointPairs pairsOut;
  private CPointsAndQualities ptqA, ptqB;
  private int[] black;
  private int[] white;  

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
    BufferedImage edgedImage = null;
    CPointsAndQualities tmpPts = new CPointsAndQualities();
    CCannyEdgeDetector edgeMaker  = new CCannyEdgeDetector(input, 0.1f, 0.9f);
    try {
      edgeMaker.execute();
      //edgedImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
      edgedImage = (BufferedImage) edgeMaker.get();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(new JFrame(), "Exception in image edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    
    Raster in1 = CNormalization.normalize((new Crgb2grey()).convert(edgedImage), 128, 64).getData();
    BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster out = output.getRaster();
    
    int [] current = null;
    int [] newpx = new int[3];
    double intensity = 0;
    double [] retval = new double[5];
    
    
    //actual corner detection
    
    CCornerDetectorCOG CC = new CCornerDetectorCOG(21);
    int shift = (int)(CCornerDetectorCOG.size/2);
    
    for (int x = 0; x < in1.getWidth()-CCornerDetectorCOG.size; x++) {
      for (int y = 0; y < in1.getHeight()-CCornerDetectorCOG.size; y++) {
        current = in1.getSamples(x, y, CCornerDetectorCOG.size, CCornerDetectorCOG.size, 0, current);
        retval = CC.GetCOG(current, false);
        intensity = retval[0];
        newpx[0] = newpx[1] = newpx[2] = (int)(intensity * 256);
        out.setPixel(x+shift, y+shift, newpx);
        setProgress(100 * (x * ((int) in1.getHeight()) + y) / ((int) (in1.getWidth() * in1.getHeight())));
      }
    }
    
    
    //normalization
    //search for max
    double maxIntensity = 0;
    int [] px = new int[3];
    for (int a = 0; a < in1.getWidth()-CCornerDetectorCOG.size; a++) {
      for (int b = 0; b < in1.getHeight()-CCornerDetectorCOG.size; b++) {
        out.getPixel(a+shift, b+shift, px);
        if (px[0]> maxIntensity) maxIntensity = px[0];
      }
    }
    double q = 256/maxIntensity;
    //q= 1;
    int newint = 0;
    newpx[1] = 0;
    newpx[2] = 0;
    for (int a = 0; a < in1.getWidth()-CCornerDetectorCOG.size; a++) {
      for (int b = 0; b < in1.getHeight()-CCornerDetectorCOG.size; b++) {
        out.getPixel(a+shift, b+shift, px);
        intensity = px[0];
        newint = (int)(q*intensity)%256;
        newpx[0] = newpx[1] = newpx[2] = newint ;
        
        if (newint > treshhold) {
          logger.log(Level.INFO, "looking at point {0} {1} with {2} ? {3}", new Object[]{a, b, newint, treshhold});
           Point2D.Double orig = new Point2D.Double(a+shift, b+shift);
           logger.info("adding a point");
           tmpPts.addPoint(orig, (double)newint/(256.0));
        }
        out.setPixel(a+shift, b+shift, newpx);
      }
    }
   
    
    
    output.setData(out);
    
    
    //return output;
    //return pairs;
    return tmpPts;
  }
  
  
  
  @Override
  protected CPointPairs doInBackground() {
    ptqA = this.getPoints(imageA);
    ptqB = this.getPoints(imageB);
    
    int requestedPairs = 30;
    double correlations[][] = new double[ptqA.size()][ptqB.size()];
    
    
    Raster rasterA = CNormalization.normalize((new Crgb2grey()).convert(imageA), 128, 64).getData();
    Raster rasterB = CNormalization.normalize((new Crgb2grey()).convert(imageB), 128, 64).getData();

    int corshift = 7;
    CCovarianceMetric CV = new CCovarianceMetric(imageA, imageB, 2*corshift+1, CAreaSimilarityMetric.Shape.RECTANGULAR);
    CCrossCorrelationMetric CC = new CCrossCorrelationMetric(imageA, imageB, 2*corshift+1, CAreaSimilarityMetric.Shape.RECTANGULAR);
    for (int i = 0; i < ptqA.size() ; i++) {
      setProgress(100 * i / ptqA.size());
      
      Point2D.Double a = ptqA.getPoint(i);
      int AX = (int)a.x;
      int AY = (int)a.y;
      logger.info("now at "+i+" out of " + ptqA.size()+" "+a.x+" x "+a.y);
      boolean AisEdge = false;
      if ((AX < (2*corshift+1)) || (AX >= rasterA.getWidth()-(2*corshift+1)) || (AY < (2*corshift+1)) || (AY >= rasterA.getHeight()-(2*corshift+1))) {
        //pairsOut.addPointPair(a, new Point2D.Double(0, 0));
        AisEdge = true;
      }
      else {
        //Point2D.Double pointA = new Point2D.Double();
        //pointA = a;
        //Point2D.Double pointB = new Point2D.Double();
        double currentA[] = null;
        double currentB[] = null;
        
        currentA = rasterA.getSamples(AX, AY, 2*corshift+1, 2*corshift+1, 0, currentA);
        
        if (ptqB.size() ==  0) break;
        for (int j = 0; j < ptqB.size() ; j++) {
          boolean BisEdge = false;
          Point2D.Double b = ptqB.getPoint(j);
          int BX = (int)b.x;
          int BY = (int)b.y;
          if ((BX < (2*corshift+1)) || (BX >= rasterA.getWidth()-(2*corshift+1)) || (BY < (2*corshift+1)) || (BY >= rasterA.getHeight()-(2*corshift+1))) {
            BisEdge = true;
          }
          
          if (AisEdge || BisEdge) {
        
            correlations[i][j] = 0.0;
          }
          else {
            
            currentB = rasterB.getSamples(BX-corshift, BY-corshift, 2*corshift+1, 2*corshift+1, 0, currentB);
            double ssd = 0;
            
            for (int k = 0; k < Math.pow(2*corshift+1, 2); k++) {
              ssd += (double)Math.pow(currentA[k] - currentB[k], 2)/65536;       
            }
            ssd /= Math.pow(2*corshift+1, 2);
            //if (ssd < 0.005) logger.info(i+"x"+j+": "+ssd);
            double cc = CC.getDistance(a, b);
            double cv = CV.getDistance(a, b);
            if (cc > 0.5) logger.info(i+"x"+j+": "+cc);
            
            correlations[i][j] = cc;
          }
        }
      }
    }
    
    
    for (int i = 0; i < requestedPairs; i++) {
      double maxCor = 0.0;
      int maxA = -1;
      int maxB = -1;
      for (int a = 0; a < ptqA.size(); a++) {
        for (int b = 0; b < ptqB.size(); b++) {
          if (correlations[a][b] > maxCor) {
            maxA = a;
            maxB = b;
            maxCor = correlations[a][b];
          }
        }
      }
      logger.info(maxA+"+"+maxB);
      if ((maxCor <= 0.0) || (maxA == -1) || (maxB == -1)) break;
      
      pairsOut.addPointPair(ptqA.getPoint(maxA), ptqB.getPoint(maxB));
      for (int a = 0; a < ptqA.size(); a++) {
        correlations[a][maxB] = 0.0;
      
      }
      for (int b = 0; b < ptqB.size(); b++) {
        correlations[maxA][b] = 0.0;
      }
    }
    
    //pairsOut.origins = ptqA.points;
    //pairsOut.projected = ptqB.points;
    //logger.info("c6464: "+correlations[63][63]);
    
    return pairsOut;
  }
}


