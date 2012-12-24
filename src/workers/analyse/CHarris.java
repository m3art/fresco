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
  
  //public int windowSize = 7;
  private int shift = param.windowSize/2;
  //public double sigma = shift/2;
  private BufferedImage image;
  //public CLocalMaximiser maximiser;
   private double [] gaussFactor;
   //public int treshold = 20;
   
           
      
  /*
   public CHarris(int matsize) {
    windowSize = matsize/2;
    if (windowSize % 2 == 0) windowSize++;
    shift = windowSize/2;
    sigma = shift/2;
    gaussFactor = new double [windowSize*windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift+i)*windowSize) + shift + j] = Math.exp((-(i*i)-(j*j))/(2*sigma*sigma));
        
      }
    }
    //maximiser = new CLocalMaximiser(windowSize);
    //logger.info("00: " + gaussFactor[0] + " sigma: " + sigma);
  }
   */
    public CHarris(BufferedImage input, CHarrisParams inputParam) {
    this.param = inputParam;
    shift = param.windowSize/2;
    gaussFactor = new double [param.windowSize*param.windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift+i)*param.windowSize) + shift + j] = Math.exp((-(i*i)-(j*j))/(2*param.sigma*param.sigma));
        
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
        gaussFactor[((shift+i)*param.windowSize) + shift + j] = Math.exp((-(i*i)-(j*j))/(2*param.sigma*param.sigma));
        
      }
    }
    //image = input;
    
  } 
   
   
   
   
   	@Override
	public String getWorkerName() {
		return "Harris corner detector";
	}
   
   
  @Override
  protected BufferedImage doInBackground(){
    return runHarris(image);  
  }  
  
  
  
   
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public BufferedImage runHarris(BufferedImage input) {
    
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
             
              A[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*xDer[(i+is)*w + j + js]*xDer[(i+is)*w + j + js];
              B[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*xDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
              C[i*w+j] += gaussFactor[((shift+is)*param.windowSize)+shift+js]*yDer[(i+is)*w + j + js]*yDer[(i+is)*w + j + js];
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
    } /*
    int locmaxdim = 3;
    for (int i = locmaxdim; i < h-locmaxdim; i++) {
      for (int j = locmaxdim; j < w-locmaxdim; j++) {
        if (H[i*w+j] < 0) H[i*w+j] = 0;
        for(int is = -locmaxdim; is <= locmaxdim; is++) {
          for(int js = -locmaxdim; js <= locmaxdim; js++) {
            if ((is == 0) && (js == 0)) continue;
            if (H[(i+is)*w + j+js] > H[i*w+j]) {
              H[i*w+j] = 0;
    
            }
          }
        }
        
       
      }
    }
    
    */
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
    
    
    
    //logger.info("returning");
    return output;
  }
          
          
          
          
}
