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


/**
 *
 * @author Jakub
 */
public class CInterestingPoints extends CRegistrationWorker {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int treshhold = 150;
  private BufferedImage imageA;
  

  @Override
  public String getWorkerName() {
    return "Interesting points search worker";
  }

  public CInterestingPoints(BufferedImage image) {
    this.imageA = image;
    
  }
  
  @Override
  protected BufferedImage doInBackground() {
    
    
    CCannyEdgeDetector edgeMaker  = new CCannyEdgeDetector(imageA, 0.1f, 0.9f);
    try {
      edgeMaker.execute();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(new JFrame(), "Exception in edges exec\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    BufferedImage edgedImage = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_INT_RGB);
    try {
      edgedImage = (BufferedImage) edgeMaker.get();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(new JFrame(), "Exception in image ass\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    
    Raster in1 = CNormalization.normalize((new Crgb2grey()).convert(edgedImage), 128, 64).getData();
    BufferedImage output = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster tmp = output.getRaster();
    
    int[] pixA = new int[3];
    int[] black = new int[3];
    black[0] = 0;
    black[1] = 0;
    black[2] = 0;
    int[] white = new int[3];
    white[0] = 255;
    white[1] = 255;
    white[2] = 255;
    
    /*
    for (int x = 0; x < in1.getWidth(); x++) {
      for (int y = 0; y < in1.getHeight(); y++) {
        in1.getPixel(x, y, pixA);
        if (pixA[0] < 200){
          tmp.setPixel(x, y, black);
        }
        else tmp.setPixel(x, y, white);
        tmp.setPixel(x, y, pixA);
        
      }
    } */    
    //JOptionPane.showMessageDialog(new JFrame(), "Got through thresholding edges\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    //output.setData(tmp);
    
    WritableRaster out = output.getRaster();
    
    int [] current = null;
    int [] newpx = new int[3];
    double intensity = 0;
    double [] retval = new double[5];
    
    CCornerDetectorCOG CC = new CCornerDetectorCOG(21);
    int shift = (int)(CCornerDetectorCOG.size/2);
    
    for (int x = 0; x < in1.getWidth()-CCornerDetectorCOG.size; x++) {
      for (int y = 0; y < in1.getHeight()-CCornerDetectorCOG.size; y++) {
        current = in1.getSamples(x, y, CCornerDetectorCOG.size, CCornerDetectorCOG.size, 0, current);
        retval = CC.GetCOG(current, false);
        intensity = retval[0];
        newpx[0] = newpx[1] = newpx[2] = (int)(retval[0] * 256);
        
        //newpx[0] = (int)(retval[2]*256); 
        //newpx[1] = (int)(retval[3]*256); 
        //newpx[2] = (int)(retval[4]*256); 
        out.setPixel(x+shift, y+shift, newpx);
        setProgress(100 * (x * ((int) in1.getHeight()) + y) / ((int) (in1.getWidth() * in1.getHeight())));
       
   
      }
    }
    
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
          imageA.getRaster().setPixel(a+shift, b+shift, white);
          imageA.getRaster().setPixel(a+shift+1, b+shift, black);
          imageA.getRaster().setPixel(a+shift+2, b+shift, black);
          imageA.getRaster().setPixel(a+shift-1, b+shift, black);
          imageA.getRaster().setPixel(a+shift-2, b+shift, black);
          imageA.getRaster().setPixel(a+shift, b+shift+1, black);
          imageA.getRaster().setPixel(a+shift, b+shift+2, black);
          imageA.getRaster().setPixel(a+shift, b+shift-1, black);
          imageA.getRaster().setPixel(a+shift, b+shift-2, black);
        
        }
        //newpx[1] = newint; 
        //newpx[2] = newint; 
        out.setPixel(a+shift, b+shift, newpx);
      }
    }
   
    
    
    output.setData(out);
    
    
    //return output;
    return output;
    
  }
}

