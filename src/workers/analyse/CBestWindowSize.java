/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.analyse;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.logging.Logger;
import workers.CImageWorker;

/**
 *
 * @author Jakub
 */
public class CBestWindowSize {
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
  public static int getBestWindowSize(Raster greyInput, int testFields, int lowerBound, int upperBound) {
    
    int bestSize = lowerBound;
    double bestMeasure = 0.0;
    int w = greyInput.getWidth();
    int h =  greyInput.getHeight();
    
    
    for(int i = lowerBound; i < upperBound; i += 2) {
      
      int maxX = 0;
    int maxY = 0;
    
      double testFieldEntropy = 0;
      double entropySum = 0;
      double measure = 0;
      int [] pixel = new int[3];
      int [] colorFrequencies = new int[256];
      
      double ratio = (w-i)/(h-i);
      int xres = (int)Math.sqrt(testFields*ratio);
      int yres = (int)(xres/ratio);
      int xdensity = ((w-i)/xres);
      int ydensity = ((h-i)/yres);
      for(int t = 0; t < testFields; t++) {
        
        //initialize variables for new test field        
        testFieldEntropy = 0;
        for (int color = 0; color < 256; color++) {
          colorFrequencies[color] = 0;
        }
        // get test field 
        
        int x = (t % xres)*xdensity;
        int y = (t / xres)*ydensity;
        if (((x + i) > w) || ((y + i) > h )) logger.info("dim overflow");
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        //int x = (int)(Math.random()*(greyInput.getWidth()-i-1));
        //int y = (int)(Math.random()*(greyInput.getHeight()-i-1));
        
        
        //fill out frequencies of colors (intensities on grey scale)
        for (int px = x; px < x+i; px++) {
          for (int py = y; py < y + i; py++) {
            greyInput.getPixel(px, py, pixel);
            colorFrequencies[pixel[0]]++;           
          }
        }
        
        //calculate the entropy itself
        for (int color = 0; color < 256; color++) {
          if (colorFrequencies[color] != 0){
            testFieldEntropy -=((double)colorFrequencies[color]/(double)(i*i)) * Math.log((double)colorFrequencies[color]/(double)(i*i));
          }
        }
        entropySum += testFieldEntropy;
        
      }
      //execution time is a factor, and entropy will hit a plateau sooner or later
      // -> add size of field as a negative factor
      //logger.info("i: " + i + " maxX: " + maxX + " maxY: " + maxY);
      measure = ((entropySum/(double)testFields)-((double)i/(2*upperBound)));
      //logger.info("size:" + i + " measure: " + measure + " entropy: "+entropySum/(double)testFields);
      //search for best window size
      if (measure >= bestMeasure) { 
        bestMeasure = measure;
        bestSize = i;
      }

    }
    
    return bestSize;
  }
  
  
  
}
