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
import java.awt.image.WritableRaster;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.CImageWorker;
import workers.analyse.paramObjects.CHarrisParams;


public class CHarris extends CAnalysisWorker {
  
  public CHarrisParams param;
  
  
  private int shift = 0;
  
  public BufferedImage image;
  
  private double [] gaussFactor;
  
   
  
    public CHarris(BufferedImage input, CHarrisParams inputParam) {
    this.param = inputParam;
    shift = param.windowSize/2;
    gaussFactor = new double [param.windowSize*param.windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift+i)*param.windowSize) + shift + j] = (1/(Math.sqrt(2*Math.PI*param.sigma*param.sigma))) * Math.exp(-((i*i) + (j*j))/(2*param.sigma*param.sigma));
        //gaussFactor[((shift+i)*param.windowSize) + shift + j] = Math.exp((-(i*i)-(j*j))/(2*param.sigma*param.sigma));
        //gaussFactor[((shift+i)*param.windowSize) + shift + j] = 1;
      }
    }
    this.image = input;
    
  }
   
   
   public CHarris(CHarrisParams inputParam) {
    this.param = inputParam;
    shift = param.windowSize/2;
    gaussFactor = new double [param.windowSize*param.windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift+i)*param.windowSize) + shift + j] = (1/(Math.sqrt(2*Math.PI*param.sigma*param.sigma))) * Math.exp(-((i*i) + (j*j))/(2*param.sigma*param.sigma));
        
      }
    }
    
  } 
   
   
   
   
   @Override
  public String getWorkerName() {
	return "Harris corner detector";
  }
   
   
  @Override
  protected BufferedImage doInBackground(){
    return runHarris(image);  
  }  
  
  public BufferedImage publicRun() {
    return doInBackground();
   }
  
  
   
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public BufferedImage runHarris(BufferedImage input) {
   logger.info("running harris");
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
    
    Raster inRaster = input.getData();
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        inRaster.getPixel(j, i, newpx);
        orig[i*w+j] = (int)((newpx[0] + newpx[1] + newpx[2])/3);
      }
    }
    
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i == 0) || i == (h-1)) xDer[i*w+j] = 0;
        else xDer[i*w+j] = (orig[(i-1)*w + j] + orig[(i+1)*w + j])/2;
        if ((j == 0) || j == (w-1)) yDer[i*w+j] = 0;
        else yDer[i*w+j] = (orig[i*w + j - 1] + orig[i*w + j + 1])/2;
        
      }
    }
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i < shift) || (i >= (h-shift-1)) || (j < shift) ||  (j >= (w-shift-1))) H[i*w+j] = 0;
        else {
          for(int is = -shift; is <= shift; is++) {
            for(int js = -shift; js <= shift; js++) {
              A[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*xDer[(i+is)*w + j + js]*xDer[(i+is)*w + j + js];
              B[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*xDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
              C[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*yDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
              H[i*w+j] = (A[i*w+j]*C[i*w+j]-B[i*w+j]*B[i*w+j]) - (this.param.sensitivity*(A[i*w+j]+C[i*w+j])*(A[i*w+j]+C[i*w+j]));
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

    double q = 255/maxH;
    int newint = 0;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        newint = 0;
        if (H[i*w+j] > 0) {
          newint = (int)(H[i*w+j] * q);
        }
        if (newint < param.threshold) newint = 0;
        newpx[0] = newpx[1] = newpx[2] = newint;
        output.getRaster().setPixel(j, i, newpx);
      }
    }
    logger.info("harris returning");
    
    return output;
    //return max.runMaxima(output);
  }
          
          
          
          
}
