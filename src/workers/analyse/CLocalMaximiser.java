/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 * @author Jakub
 */
public class CLocalMaximiser {
  
  int windowSize;
  
  public CLocalMaximiser(int input) {
    windowSize = input;
    
  }
  
  
  public BufferedImage runMaxima(BufferedImage input) {
    Raster inputData = input.getData();
    int w = input.getWidth();
    int h = input.getHeight();
    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    WritableRaster outputData = output.getRaster();
    
    int [] centerpx = new int[3];
    int [] testpx = new int[3];
    
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        outputData.setPixel(i, j, centerpx);
      }
    }
    
    
    
    int locmaxdim = windowSize/2;
    for (int i = locmaxdim; i < w-locmaxdim; i++) {
      for (int j = locmaxdim; j < h-locmaxdim; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        //boolean isMax = true;
        for(int is = -locmaxdim; is <= locmaxdim; is++) {
          for(int js = -locmaxdim; js <= locmaxdim; js++) {
            if ((is == 0) && (js == 0)) continue;
            testpx = inputData.getPixel(i+is, j+js, testpx);
            if (testpx[0] > centerpx[0]) {
              centerpx[1] = centerpx[2] = 0;
              outputData.setPixel(i, j, centerpx);
            }
            
            
          }
        }
      }
    }
    for (int x = 0; x < input.getWidth()-locmaxdim; x++) {
      for (int y = 0; y < input.getHeight()-locmaxdim; y++) {
        centerpx = outputData.getPixel(x, y, centerpx);
        if (centerpx[1] == 0) centerpx[0] = 0;
        outputData.setPixel(x, y, centerpx);
      }
    }
  
    return output;
  }
}
