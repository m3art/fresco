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
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  /** Expected input: unrolled size*size matrix in row-major format, each cell holds image brightness in that particular pixel, 
   * VALUES OF INTENSITY BETWEEN 1 and SCALE
   * output: distance between center of matrix (2, 2) and center of gravity in input. 
   * Negative values of output signify an error.
   **/    
  public CCornerDetectorCOG(int matsize) {
    size = matsize;
  
  }
  public double GetCOG(int [] input, boolean dump) {
    //dump = true;
    //if (input.length != size*size) return -1;
    //JOptionPane.showMessageDialog(new JFrame(), input[0] + " " + input[24]+ "\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    double COGx = 0;
    double COGy = 0;
    int shift = 0;

    double val = 0;
    shift = (int)(size/2);
    int xdim, ydim, iD, jD;
    //i, j are coordinates in input square subimage
    String s = "";
    if (dump) {
      
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          s = s+input[i*size+j];
          s = s+" ";
        }
        s = s+"\n";
      }
      
      logger.info(s);
    };
    
    
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
    
    
    
    
    //COGx /= 
    //COGy /= size*size;
    //perpendicular check: pixels on an axis perpendciluar to the vector ([0, 0], [COGx, COGy]) should be dark
    COGx /= totalVal;
    COGy /= totalVal;
    if(dump) logger.info("got COG at "+COGx+","+COGy);
    double dist = Math.sqrt(COGx*COGx + COGy*COGy) / Math.sqrt(2*shift*shift);
    
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
      if (dump) {
        JOptionPane.showMessageDialog(new JFrame(),
                s + "\n" +
                "COGx: " + COGx + "\n" +
                "COGy: " + COGy + "\n" +
                "COGcx: " + COGcx + "\n" +
                "COGcy: " + COGcy + "\n"
                , "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }
      int chkAX = (int)COGcy+shift;
      int chkAY = (-1*(int)(COGcx))+shift;
      if (chkAX >= size || chkAX < 0 || chkAY >= size || chkAX < 0) {
          JOptionPane.showMessageDialog(new JFrame(),
                "chkA error" + s + "\n" +
                "COGx: " + COGx + "\n" +
                "COGy: " + COGy + "\n" +
                "chkAX: " + (chkAX) + "\n" +
                "chkAY: " + (chkAY) + "\n" +
                "COGcx: " + COGcx + "\n" +
                "COGcy: " + COGcy + "\n"
                , "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }

      double valA = (double)input[(chkAY*size)+chkAX]/scale;

      int chkBX = (-1*(int)(COGcy))+shift;
      int chkBY = (int)(COGcx)+shift;
      if (chkBX >= size || chkBX < 0 || chkBY >= size || chkBX < 0) {
          JOptionPane.showMessageDialog(new JFrame(),
                "chkB error" + s + "\n" +
                "COGx: " + COGx + "\n" +
                "COGy: " + COGy + "\n" +

                "chkAX: " + (chkBX) + "\n" +
                "chkAY: " + (chkBY) + "\n" +
                "COGcx: " + COGcx + "\n" +
                "COGcy: " + COGcy + "\n"
                , "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
      }


      double valB = (double)input[(chkBY*size)+chkBX]/scale;

      perpC = 1-((valA+valB)/2);
          if ( perpC>1 ) {
            JOptionPane.showMessageDialog(new JFrame(), "perpC > 1: "+ perpC +"\n", "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 

          }

    }

    
    double centerWhiteness = (double)(input[(int)((size*size)/2)])/scale;
    /*if (dist>0.5) {
      JOptionPane.showMessageDialog(new JFrame(), "dist: "+dist+" \n" + "center: " + centerWhiteness, "Interesting points stopped", JOptionPane.WARNING_MESSAGE);
    }*/
    //int iswhite = input[(size*size)/2] > (scale/2) ? 0 : 1;

    if (dump) { 
      JOptionPane.showMessageDialog(new JFrame(),
              s + "\n" +
              "dist: " + dist + "\n" +
              "COGx: " + COGx + "\n" +
              "COGy: " + COGy + "\n" +
    //          "chkAX: " + (chkAX-shift) + "\n" +
      //        "chkAY: " + (chkAY-shift) + "\n" +
          //    "chkBX: " + (chkBX-shift) + "\n" +
        //      "chkBY: " + (chkBY-shift) + "\n" +
            //  "perpC: " + perpC + "\n" +
              "cW: " + centerWhiteness + "\n"             
              , "Interesting points stopped", JOptionPane.WARNING_MESSAGE); 
    }
    
    
    
    return centerWhiteness*dist*perpC*perpC;
  }
  
}

