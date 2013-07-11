/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
import workers.analyse.paramObjects.CCOGParams;
/**
 *
 * @author Jakub
 */

public class CCornerDetectorCOG extends CAnalysisWorker{
 
  private int shift;
 
  public CCOGParams param;
  
  public BufferedImage image;
  public BufferedImage output;
  
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  /** Expected input: unrolled windowSize*windowSize matrix in row-major format, each cell holds image brightness in that particular pixel, 
   * VALUES OF INTENSITY BETWEEN 1 AND scale
   * output: double[5] containing values between 0 and 1 
   * [0]: the combined "cornerity",
   * [1]: a copy of the output from the edge detector
   * [2]: distance of COG from center, 
   * [3]: difference of brightness between COG and image center
   * [4]: perpendicular check - darkness of pixels on an axis perpendicular to the vector(Center, COG)
   * Negative values of output signify an error.
   **/    
  public CCornerDetectorCOG(int matsize) {
    param = new CCOGParams();
    param.windowSize = matsize;
    shift = param.windowSize/2;
  }
  
  public CCornerDetectorCOG(BufferedImage input, BufferedImage out, CCOGParams inputParams) {
    image = input;
    param = inputParams;
    shift = param.windowSize/2;
    int h = image.getHeight();
    int w = image.getWidth();
    output = out;
  }
  
  public CCornerDetectorCOG(BufferedImage input) {
    image = input;
    shift = param.windowSize/2;
    int h = image.getHeight();
    int w = image.getWidth();
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
  }
  public CCornerDetectorCOG(CCOGParams inputParams) {
    param = inputParams;
    shift = param.windowSize/2;
  }
  public CCornerDetectorCOG(BufferedImage input, CCOGParams inputParams) {
    param = inputParams;
    shift = param.windowSize/2;
    image = input;
    int h = image.getHeight();
    int w = image.getWidth();
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    
  }
  
  @Override
	public String getWorkerName() {
		return "COG corner detector";
	}

  @Override
  protected BufferedImage doInBackground() {
    int h = image.getHeight();
    int w = image.getWidth();
    Raster imageData = image.getRaster();    
    int [] current = null;
    int [] newpx = new int[3];
    double [] retval = new double[5];    
    
    WritableRaster outputData = output.getRaster();
    for (int x = 0; x < w-param.windowSize; x++) {
      for (int y = 0; y < h-param.windowSize; y++) {
        current = imageData.getSamples(x, y, param.windowSize, param.windowSize, 0, current);
         retval = getCOG(current, false);
        //set newpx to grey accordingly
        newpx[0] = newpx[1] = newpx[2] = (int)(retval[0]*256);
        outputData.setPixel(x+shift, y+shift, newpx);
      }
    }
    double maxIntensity = 0;
    int [] px = new int[3];
    for (int a = 0; a < w-param.windowSize; a++) {
      for (int b = 0; b < h-param.windowSize; b++) {
        outputData.getPixel(a+shift, b+shift, px);
        if (px[0]> maxIntensity) maxIntensity = px[0];
      }
    }
    double q = 255.0/maxIntensity;
    int newint = 0;
    newpx[1] = 0;
    newpx[2] = 0;
    for (int a = 0; a < w-param.windowSize; a++) {
      for (int b = 0; b < h-param.windowSize; b++) {
        outputData.getPixel(a+shift, b+shift, px);
        newint = (int)(q*px[0]);
        if (newint < param.threshold) newint = 0;
        newpx[0] = newpx[1] = newpx[2] = newint ;
        outputData.setPixel(a+shift, b+shift, newpx);
      }
    }
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
    //logger.info( "running with d: " + param.distW + " pc: " + param.perpCW + " cw: " + param.centerWhitenessW + " wd: " + param.whiteDiffW);
    return doInBackground();
  }
  
  public double[] getCOG(int [] input, boolean dump) {
    
    double COGx = 0;
    double COGy = 0;
    double val = 0;
    //int shift = (int)(param.windowSize/2);
    int xdim, ydim, iD, jD;
    //i, j are coordinates in input square subimage
    
    //X and j are HORIZONTAL coords
    //Y and i are VERTICAL coords
    double totalVal = 0;
    for (int i = 0; i < param.windowSize; i++) {
      for (int j = 0; j < param.windowSize; j++) {
        //ydim, xdim are coordiantes if the center pixel is considered [0, 0]
        //iD, jD are coordinates in the original input, they correspond to pixels halfway between the center pixel and i, j
        ydim = i - shift;
        iD = (int)(ydim/2) + shift;
        xdim = j - shift;
        jD = (int)(xdim/2) + shift;
         //val is a geometric average of brightness in the selected pixel and in a pixel halfway towards the center
        val = ((double)input[(i*param.windowSize)+j]/param.scale) * ((double)input[(iD*param.windowSize)+jD]/param.scale);
        totalVal += val;
        //sanity check
        if ( val>1 ) {
          logger.info("Oversize val: " + val);
          JOptionPane.showMessageDialog(new JFrame(), "Val > 1: "+ val +"\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
        }
        
        //the center of gravity is pushed toward a bright pixel, if it's on a continuous line
        //that is, each bright point is weighted into the COG calculation not only by its brightness and distance, 
        //but also by the brightness of a point halfway toward the center of the square
        COGx += xdim * val;
        COGy += ydim * val;
    
      }
     }
    
    COGx /= totalVal;
    COGy /= totalVal;
   
    double dist = Math.sqrt(COGx*COGx + COGy*COGy) / Math.sqrt(2*shift*shift);
    //perpendicular check: pixels on the axis perpendciluar to the vector ([0, 0], [COGx, COGy]) should be dark
    double COGcx = COGx;
    double COGcy = COGy;
    double perpC = 0;
    if(dist > 0){
      while ((Math.abs(COGcx) < shift-1) && (Math.abs(COGcy) < shift-1)) {
        COGcy += COGy;
        COGcx += COGx;
      }
      while ((Math.abs(COGcx) > shift) || (Math.abs(COGcy) > shift)) {
        COGcy -= COGy;
        COGcx -= COGx;
      }
      int chkAX = (int)Math.round(COGcy+shift);
      int chkAY = (int)Math.round(((-1)*(COGcx))+shift);
    
      double valA = (double)input[(chkAY*param.windowSize)+chkAX]/param.scale;
      
      int chkBX = (int)Math.round((-1*COGcy)+shift);
      int chkBY = (int)Math.round(COGcx+shift);
    
      double valB = (double)input[(chkBY*param.windowSize)+chkBX]/param.scale;

      perpC = 1-((valA+valB)/2);
          if ( perpC>1 ) {
            JOptionPane.showMessageDialog(new JFrame(), "perpC > 1: "+ perpC +"\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }
    }
 
    double centerWhiteness = (double)(input[(int)((param.windowSize*param.windowSize)/2)])/(param.scale);
    
    

    int rX = (int)Math.round(COGx) + shift;
    int rY = (int)Math.round(COGy) + shift;
    double COGWhiteness = (double)(input[rY*param.windowSize+rX])/param.scale;
    double whiteDiff = (double)(1+centerWhiteness-COGWhiteness)*256;
    whiteDiff /= 512;

    
     double [] ret = new double[6];
     
     //ret[0] = Math.pow(dist*perpC, 1-(centerWhiteness/CWI))*whiteDiff;
     ret[0] = (dist*param.distW + perpC*param.perpCW + centerWhiteness*param.centerWhitenessW + whiteDiff*param.whiteDiffW + (dist*centerWhiteness)*param.mixW);
     if (ret[0] > 1) logger.info( "too much in ret[0]: d: " + dist + "." + param.distW 
                                + " pc: " + perpC + "." + param.perpCW 
                                + " cw: " + centerWhiteness + "." + param.centerWhitenessW 
                                + " wd: " + whiteDiff + "." + param.whiteDiffW
                                + " m: " + dist*centerWhiteness + "." + param.mixW);
     ret[1] = centerWhiteness;
     ret[2] = dist;
     ret[3] = whiteDiff;
     ret[4] = perpC;
     ret[5] = dist*centerWhiteness*perpC*whiteDiff;
     return ret;
     
  }
 
  
}


  
