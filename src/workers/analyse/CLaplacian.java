/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workers.CImageWorker;

/**
 * Basic edge detector. Uses Laplacian of Gaussian as a filter. Two different
 * sizes are accepted - matrix 5x5 or 3x3
 *
 * @author gimli
 * @version 10.6.2009
 */
public class CLaplacian extends CAnalysisWorker {
        private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());
	private final static int FILTER_SIZE_DEFAULT = 5;
        private final static int GAUSS_SIZE_DEFAULT = 13;
        private final static double GAUSS_SIGMA_DEFAULT = 1;
        
	/** size of input image */
	//private final Dimension size;
	/** output image - with edges */
	public BufferedImage image;
	/** input raster */
	public Raster input;
	/** Number of colour bans */
	private final int bands;
	/** Size of filter (3 or 5)*/
	int filterSize;
        /** Size of gaussian smoothing*/
        int gaussSize;
        /** variance of gaussian smoothing*/
        double gaussSigma ;
        
        int LoGSize;
        double LoGSum;
        
  private int w;
  private int h;
	/** Precomputed filters */
	public static int[][] M3 = {{1, 1, 1}, {1, -8, 1}, {1, 1, 1}};
	public static int[][] M5 = {{0, 0, 1, 0, 0},
		{0, 1, 2, 1, 0},
		{1, 2, -16, 2, 1},
		{0, 1, 2, 1, 0},
		{0, 0, 1, 0, 0}};
        
        public static double[][] G;
        public static double[][] LoG;

        public static int getFilterSizeDefault() {return FILTER_SIZE_DEFAULT;};
        public static double getGaussSigmaDefault() {return GAUSS_SIGMA_DEFAULT;};
        public static int getGaussSizeDefault() {return GAUSS_SIZE_DEFAULT;};
        
        
public CLaplacian(BufferedImage original) {
    
    input = original.getData();
    filterSize = FILTER_SIZE_DEFAULT;
    gaussSize = GAUSS_SIZE_DEFAULT;
    gaussSigma = GAUSS_SIGMA_DEFAULT;
    w = original.getWidth();
    h = original.getHeight();
    image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    bands = original.getSampleModel().getNumBands();
    if (gaussSize % 2 == 0) gaussSize+=1;
    G = InitGaussian(gaussSize, gaussSigma);
    LoG = InitLoG();
    
}

public CLaplacian(BufferedImage original, int fSize, double gSigma, int gSize) {
    
    input = original.getData();
    filterSize = fSize;
    gaussSize = gSize;
    gaussSigma = gSigma;
    w = original.getWidth();
    h = original.getHeight();
    image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    bands = original.getSampleModel().getNumBands();
    if (gaussSize % 2 == 0) gaussSize+=1;
    G = InitGaussian(gaussSize, gaussSigma);
    LoG = InitLoG();
    
    if (filterSize != 3 && filterSize != 5) filterSize = 3;
}


public BufferedImage runPublic() {
    return doInBackground();
}

private double[][] InitGaussian(int size, double sigma) {
    if (size  % 2 == 0) size+=1;
    
    double[][] res = new double[size][size];
    int width = (size-1)/2;
    for (int i = -width; i <= width; i++) {
        for (int j = -width; j <= width; j++) {
            res[i+width][j+width] = (1/(Math.sqrt(2*Math.PI*sigma*sigma))) * Math.exp(-((i*i) + (j*j))/(2*sigma*sigma));
        }
    }
    return res;
}

private double [][] InitLoG() {
    int LoGWidth = ((filterSize-1)/2) + ((gaussSize-1)/2);   //full convolution
    
    LoGSize = 2*LoGWidth + 1;
    LoGSum = 0.0;
    int filterWidth = (filterSize-1)/2;
    //int LoGWidth = (LoGSize-1) /2;
    double[][] res = new double[LoGSize][LoGSize];
    for (int LoGi = 0; LoGi < LoGSize; LoGi++) {
        for (int LoGj = 0; LoGj < LoGSize; LoGj++) {
            for (int Li = -filterWidth; Li <= filterWidth; Li++) {
                for (int Lj = -filterWidth; Lj <= filterWidth; Lj++) {
                    double val = 0;
                    int Gi = LoGi + Li - filterWidth;
                    int Gj = LoGj + Lj - filterWidth;
                    if (
                          Gi < 0 ||
                          Gj < 0 ||
                          Gi >= gaussSize ||
                          Gj >= gaussSize
                    ) {
                        val = 0.0;
                    }
                    else {
                        if (filterSize == 3) {
                            val = G[Gi][Gj] * M3[Li+filterWidth][Lj+filterWidth];
                        }
                        else if (filterSize == 5) {
                            val = G[Gi][Gj] * M5[Li+filterWidth][Lj+filterWidth];
                        }
                    }
                    res[LoGi][LoGj] += val;
                }
            }
            if (res[LoGi][LoGj] >= 0) LoGSum += res[LoGi][LoGj];
            else LoGSum -= res[LoGi][LoGj];
        }
    }
    return res;
}   


@Override
protected BufferedImage doInBackground() {
    int x, y;
    int[] A = null;
    int[] sum = new int[bands];
    WritableRaster raster = image.getRaster();
    double _max = 0.0;
    for (x = 0; x < w; x++) {
        for (y = 0; y < h; y++) {
            if (x - (LoGSize - 1) / 2 < 0
                || x + (LoGSize - 1) / 2 > w - 1
                || y - (LoGSize - 1) / 2 < 0
                || y + (LoGSize - 1) / 2 > h - 1) {
               for (int b = 0; b < bands; b++) {
                 sum[b] = 0;
               }
             } else {
                for (int b = 0; b < bands; b++) {
                    A = input.getSamples(x - (LoGSize - 1) / 2, y - (LoGSize - 1) / 2, LoGSize, LoGSize, b, A);
                    sum[b] = getLoG(A);
                }
                for (int b = 0; b < bands; b++) {
                    //if (Math.abs(sum[b]) > _max) _max = Math.abs(sum[b]);
                    //if (Math.abs(sum[b]) < 1) sum[b] = 0;
                    sum[b] = 128 - sum[b];
                    //if (sum[b] <= 128) sum[b] = 0;
                    
                }
            }
            raster.setPixel(x, y, sum);
            setProgress(100 * (x * h + y) / (w * h));
        }
        
    }
    
    BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    WritableRaster outRaster = output.getRaster();
    
    for (x = 1; x < w-1; x++) {
        for (y = 1; y < h-1; y++) {
            int out[] = new int[bands];
            out[0] = 0;
            int B[] = new int[9];
            
            for (int b = 0; b < bands; b++) {
              raster.getSamples(x-1, y-1, 3, 3, b, B);
              for (int i = 0; i < 9; i++) {
                  //if ((i == 4) || (i == 0) || (i == 2) || (i == 6) || (i == 8))continue;
                  if (i == 4)continue;
                  else if (B[i] < 128 && B[4] > 128 ) {
                      out[b] = B[4];
                  }
              }
            }
            outRaster.setPixel(x, y, out);
            setProgress(100 * (x * h + y) / (w * h));
        }
    }
   for (x = 1; x < w-1; x++) {
        for (y = 1; y < h-1; y++) {
            int out[] = new int[bands];
            outRaster.getPixel(x, y, out);
            int sumb = 0;
            int maxb = 0;
            for (int b = 0; b < bands; b++) {
                sumb += out[b];
                //if (out[b] > maxb) {
                //    maxb = out[b];
                //}
            }
            for (int c = 0; c < bands; c++) {
                out[c] = (int)(sumb/3);
                //out[c] = maxb;
            }
            outRaster.setPixel(x, y, out);
            out = null;
        }
    }
    
            
    
    
    sum = null;
    A = null;
    
    image.setData(outRaster);
    //image.setData(raster);
    raster = null;

    
    
    
    return image;
}

@Override
public String getWorkerName() {
        return "Laplacian of Gaussian Edge Detector";
}

private int getLoG(int[] A) {
        int i, j;
        int out = 0;
        //int[][] M = (filterSize == 5) ? M5 : M3;

        for (i = 0; i < LoGSize; i++) {
          for (j = 0; j < LoGSize; j++) {
                  out += LoG[i][j] * A[i + j * LoGSize];
          }
        }

        return (int)(out/LoGSum);
}

@Override
public boolean hasDialog() {
        return true;
}

private JComboBox filterSizeInput = new JComboBox();
private JTextField gaussSigmaInput = new JTextField();
private JTextField gaussSizeInput = new JTextField();

@Override
public boolean confirmDialog() {
        filterSize = (Integer)filterSizeInput.getSelectedItem();
        gaussSize = Integer.parseInt(gaussSizeInput.getText());
        gaussSigma = Double.parseDouble(gaussSigmaInput.getText());
        
        G = InitGaussian(gaussSize, gaussSigma);
        LoG = InitLoG();
        
        return true;
}

@Override
public JDialog getParamSettingDialog() {
        JPanel content = new JPanel();

        filterSizeInput.addItem((Integer) 3);
        filterSizeInput.addItem((Integer) 5);

        TableLayout layout = new TableLayout(new double[]{200,100}, new double[]{20,20,20});
        content.setLayout(layout);

        content.add(new JLabel("Set filter size: "),"0,0");
        content.add(filterSizeInput, "1,0");
        
        content.add(new JLabel("Set gauss Sigma: "),"0,1");
        content.add(gaussSigmaInput, "1,1");
        
        content.add(new JLabel("Set gauss size: "),"0,2");
        content.add(gaussSizeInput, "1,2");
        

        return CWorkerDialogFactory.createOkCancelDialog(this, content);
}
}
