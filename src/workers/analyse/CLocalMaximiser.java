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
    int [] blackpx = new int[3];
    blackpx[0] = 0;
    blackpx[1] = 0;
    blackpx[2] = 0;
    
    //init
    boolean[] isMax = new boolean[w*h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        outputData.setPixel(i, j, centerpx);
        isMax[j*w+i] = false;
      }
    }
    
    //test if max in region
    int locmaxdim = windowSize/2;
    for (int i = locmaxdim; i < w-locmaxdim; i++) {
      for (int j = locmaxdim; j < h-locmaxdim; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        isMax[j*w+i] = true;
        
        for(int is = -locmaxdim; is <= locmaxdim; is++) {
          for(int js = -locmaxdim; js <= locmaxdim; js++) {
            if ((is == 0) && (js == 0)) continue;
            testpx = inputData.getPixel(i+is, j+js, testpx);
            if (testpx[0] > centerpx[0]) {
               isMax[j*w+i] = false;
            }
          }
        }
      }
    }
    
    //black out all non-maxima
    for (int x = 0; x < input.getWidth(); x++) {
      for (int y = 0; y < input.getHeight(); y++) {
        if (isMax[y*w+x]) {
          int[] origpx = new int[3];
          origpx = inputData.getPixel(x, y, origpx);
          outputData.setPixel(x, y, origpx);
        }
        else {
          outputData.setPixel(x, y, blackpx);
        }
      }
    }
    
    
    //test if multiple equal maxima are adjacent
    //only keep those to the bottom right (arbitrary, can keep any)
    for (int i = locmaxdim; i < w-locmaxdim; i++) {
      for (int j = locmaxdim; j < h-locmaxdim; j++) {
        centerpx = inputData.getPixel(i, j, centerpx);
        for(int is = 0; is <= 1; is++) {
          for(int js = 0; js <= 1; js++) {
           testpx = outputData.getPixel(i+is, j+js, testpx);
            if ((is == 0) && (js == 0)) continue;
            else if (testpx[0] == centerpx[0]) {
               isMax[j*w+i] = false;
            }
           
          }
        }
      }
    }
    //black out all but bottom right of contiguous maxima region
    for (int x = 0; x < input.getWidth(); x++) {
      for (int y = 0; y < input.getHeight(); y++) {
        if (isMax[y*w+x]) {
          int[] origpx = new int[3];
          origpx = inputData.getPixel(x, y, origpx);
          outputData.setPixel(x, y, origpx);
        }
        else {
          outputData.setPixel(x, y, blackpx);
        }
      }
    }
    
    output.setData(outputData);
    return output;
  }
}

