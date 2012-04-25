/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workers.analyse;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
/**
 *
 * @author Jakub
 */

public class CCornerDetectorCOG {
  public static int size = 7;
  public static int scale = 256;
  public static double CWI = 1.7;
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  /** Expected input: unrolled size*size matrix in row-major format, each cell holds image brightness in that particular pixel, 
   * VALUES OF INTENSITY BETWEEN 1 and SCALE
   * output: double[5] containing values between 0 and 1 
   * [0]: the combined "cornerity",
   * [1]: a copy of the output from the edge detector
   * [2]: distance of COG from center, 
   * [3]: difference of brightness between COG and image center
   * [4]: perpendicular check - darkness of pixels on an axis perpendicular to the vector(Center, COG)
   * Negative values of output signify an error.
   **/    
  public CCornerDetectorCOG(int matsize) {
    size = matsize;
  
  }
  public double[] GetCOG(int [] input, boolean dump) {
    
    double COGx = 0;
    double COGy = 0;
    int shift = 0;

    double val = 0;
    shift = (int)(size/2);
    int xdim, ydim, iD, jD;
    //i, j are coordinates in input square subimage
    String s = "";
    
      
    if (dump) {
      logger.info(s);
    };
    
    //X and j are HORIZONTAL coords
    //Y and i are VERTICAL coords
    
    
    double totalVal = 0;
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        //ydim, xdim are coordiantes if the center pixel is considered [0, 0]
        //iD, jD are coordinates in the original input, they correspond to pixels halfway between the center pixel and i, j
        ydim = i - shift;
        iD = (int)(ydim/2) + shift;
        xdim = j - shift;
        jD = (int)(xdim/2) + shift;
         //val is a geometric average of brightness in the selected pixel and in a pixel halfway towards the center
        val = ((double)input[(i*size)+j]/scale) * ((double)input[(iD*size)+jD]/scale);
        totalVal += val;
        //sanity check
        if ( val>1 ) {
          logger.info("Oversize val: " + val);
          JOptionPane.showMessageDialog(new JFrame(), "Val > 1: "+ val +"\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
        }
        
        //the center of gravity is pushed toward a bright pixel, if it's on a continuous line
    
        COGx += xdim * val;
        COGy += ydim * val;
    
      }
     }
    
    
    COGx /= totalVal;
    COGy /= totalVal;
    if(dump) logger.info("got COG at "+COGx+","+COGy);
    
    
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
      int chkAY = (int)Math.round((-1*(COGcx))+shift);
    
      double valA = (double)input[(chkAY*size)+chkAX]/scale;
      
      int chkBX = (int)Math.round((-1*COGcy)+shift);
      int chkBY = (int)Math.round(COGcx+shift);
    
      double valB = (double)input[(chkBY*size)+chkBX]/scale;

      perpC = 1-((valA+valB)/2);
          if ( perpC>1 ) {
            JOptionPane.showMessageDialog(new JFrame(), "perpC > 1: "+ perpC +"\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 

          }

    }

    
    double centerWhiteness = (double)(input[(int)((size*size)/2)])/(scale);
    /*
    if (centerWhiteness < 0.6) centerWhiteness = 0.0001;
    else centerWhiteness = (centerWhiteness - 0.6) * 2.5;
    */
    

    int rX = (int)Math.round(COGx) + shift;
    int rY = (int)Math.round(COGy) + shift;
    double COGWhiteness = (double)(input[rY*size+rX])/scale;
    double WhiteDiff = (double)(1+centerWhiteness-COGWhiteness)*256;
    WhiteDiff /= 512;

    
     double [] ret = new double[5];
     
     ret[0] = Math.pow(dist*perpC, 1-(centerWhiteness/CWI))*WhiteDiff;
     ret[1] = centerWhiteness;
     ret[2] = dist;
     ret[3] = WhiteDiff;
     ret[4] = perpC;
     return ret;
     
  }
  
}

