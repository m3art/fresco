/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;



/**
 *
 * @author Jakub
 */


import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;


public class CHarris {
  
  public int windowSize = 7;
  public int shift = 0;
  public double sigma = 0;
  
  
   double [] gaussFactor;
   
           
      
  
   public CHarris(int matsize) {
    windowSize = (2*matsize)/5;
    if (windowSize % 2 == 0) windowSize++;
    shift = windowSize/2;
    sigma = shift/2;
    gaussFactor = new double [windowSize*windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift+i)*windowSize) + shift + j] = Math.exp((-(i*i)-(j*j))/(2*sigma*sigma));
        
      }
    }
    
    //logger.info("00: " + gaussFactor[0] + " sigma: " + sigma);
  }
   
  
  
   
   
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public BufferedImage execute(BufferedImage input) {
    
    //windowSize = 15;
    //shift = windowSize/2;
  
    int h = input.getHeight();
    int w = input.getWidth();
    int [] newpx = new int[3];
    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    double [] orig = new double [w*h];
    double [] xDer = new double [w*h];
    double [] yDer = new double [w*h];
    double [] A = new double [w*h];
    double [] B = new double [w*h];
    double [] C = new double [w*h];
    double [] H = new double [w*h];
    
    //logger.info("allocated stuff");
    
    Raster inRaster = input.getData();
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        inRaster.getPixel(j, i, newpx);
        orig[i*w+j] = newpx[0];
        
      }
      
    }
      
    //logger.info("got orig");
    
    
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i == 0) || i == (h-1)) xDer[i*w+j] = 0;
        else xDer[i*w+j] = (orig[(i-1)*w + j] + orig[(i+1)*w + j])/2;
        
        if ((j == 0) || j == (w-1)) yDer[i*w+j] = 0;
        else yDer[i*w+j] = (orig[i*w + j - 1] + orig[i*w + j + 1])/2;
        
      }
    }
    //logger.info("got deriv");
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i < shift) || (i >= (h-shift-1)) || (j < shift) ||  (j >= (w-shift-1))) H[i*w+j] = 0;
        else {
          for(int is = -shift; is <= shift; is++) {
            for(int js = -shift; js <= shift; js++) {
             
              A[i*w+j] += gaussFactor[((shift+is)*windowSize)+shift+js]*xDer[(i+is)*w + j + js]*xDer[(i+is)*w + j + js];
              B[i*w+j] += gaussFactor[((shift+is)*windowSize)+shift+js]*xDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
              C[i*w+j] += gaussFactor[((shift+is)*windowSize)+shift+js]*yDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
              H[i*w+j] = (A[i*w+j]*C[i*w+j]-B[i*w+j]*B[i*w+j]) - (0.04*(A[i*w+j]+C[i*w+j])*(A[i*w+j]+C[i*w+j]));
            }
          }
        }
      }
    }
    
    double maxH = 0;
    
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if (H[i*w+j] > maxH) maxH = H[i*w+j];      
      }
    } 
    int locmaxdim = 3;
    for (int i = locmaxdim; i < h-locmaxdim; i++) {
      for (int j = locmaxdim; j < w-locmaxdim; j++) {
        if (H[i*w+j] < 0) H[i*w+j] = 0;
        boolean isMax = true;
        for(int is = -locmaxdim; is <= locmaxdim; is++) {
          for(int js = -locmaxdim; js <= locmaxdim; js++) {
            if ((is == 0) && (js == 0)) continue;
            if (H[(i+is)*w + j+js] > H[i*w+j]) {
              H[i*w+j] = 0;
              isMax = false;
            }
          }
        }
        //if (H[i*w+j] > (maxH/120)) H[i*w+j] = maxH; 
       
      }
    }
    
    
    //logger.info("got H");
 
    //logger.info("got maxH: " + maxH);
    double q = 255/maxH;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        newpx[0] = newpx[1] = newpx[2] = 0;
        if (H[i*w+j] > 0) newpx[0] = newpx[1] = newpx[2] = (int)(H[i*w+j] * q);
        
        output.getRaster().setPixel(j, i, newpx);
      }
    }
    
    
    
    //logger.info("returning");
    return output;
  }
          
          
          
          
}
