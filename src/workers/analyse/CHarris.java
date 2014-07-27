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
import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import workers.CImageWorker;
import workers.analyse.paramObjects.CHarrisParams;

public class CHarris extends CAnalysisWorker {

  /**
   * parameters for the corner detector
   */
  public CHarrisParams param;
  /**
   * shorthand for (int) param.windowsize/2
   */
  private int shift;
  /**
   * optional - Harris can run on this image or on supplied one using
   * runHarris()
   */
  public BufferedImage image;
  /**
   * 2D Gaussian in a square matrix, side length = param.windowsize
   */
  private double[] gaussFactor;
  private static final Logger logger = Logger.getLogger(CImageWorker.class.getName());

  public CHarris(BufferedImage input, CHarrisParams inputParam) {
    param = inputParam;
    shift = param.windowSize / 2;
    gaussFactor = new double[param.windowSize * param.windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift + i) * param.windowSize) + shift + j] = (1 / (Math.sqrt(2 * Math.PI * param.sigma * param.sigma))) * Math.exp(-((i * i) + (j * j)) / (2 * param.sigma * param.sigma));
      }
    }
    this.image = input;

  }

  public CHarris(CHarrisParams inputParam) {
    param = inputParam;
    shift = param.windowSize / 2;
    gaussFactor = new double[param.windowSize * param.windowSize];
    for (int i = -shift; i <= shift; i++) {
      for (int j = -shift; j <= shift; j++) {
        gaussFactor[((shift + i) * param.windowSize) + shift + j] = (1 / (Math.sqrt(2 * Math.PI * param.sigma * param.sigma))) * Math.exp(-((i * i) + (j * j)) / (2 * param.sigma * param.sigma));

      }
    }

  }

  @Override
  public String getWorkerName() {
    return "Harris corner detector";
  }

  @Override
  protected BufferedImage doInBackground() {
    return runHarris(image);
  }

  public BufferedImage runHarris() {
    return doInBackground();
  }

  public BufferedImage runHarris(BufferedImage input) {

    int h = input.getHeight();
    int w = input.getWidth();
    int[] pixel = new int[3];
    double[] orig = new double[w * h];

    
    double[] xDer = new double[w * h];
    double[] yDer = new double[w * h];
    double[] A = new double[w * h];
    double[] B = new double[w * h];
    double[] C = new double[w * h];
    double[] H = new double[w * h];

    Raster inRaster = input.getData();

    //transforfm color input into one band greyscale image
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        inRaster.getPixel(j, i, pixel);
        orig[i * w + j] = (int) ((pixel[0] + pixel[1] + pixel[2]) / 3);
      }
    }
    //calculate derivatives
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i == 0) || i == (h - 1)) {
          xDer[i * w + j] = 0;
        } else {
          xDer[i * w + j] = (orig[(i - 1) * w + j] + orig[(i + 1) * w + j]) / 2;
        }
        if ((j == 0) || j == (w - 1)) {
          yDer[i * w + j] = 0;
        } else {
          yDer[i * w + j] = (orig[i * w + j - 1] + orig[i * w + j + 1]) / 2;
        }

      }
    }
    //calculate Harris cornerity
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if ((i < shift) || (i >= (h - shift - 1)) || (j < shift) || (j >= (w - shift - 1))) {
          H[i * w + j] = 0;
        } else {
          for (int is = -shift; is <= shift; is++) {
            for (int js = -shift; js <= shift; js++) {
              if (is * is + js * js > shift * shift) {
                continue;
              }
              A[i * w + j] += gaussFactor[((shift + is) * param.windowSize) + shift + js] * xDer[(i + is) * w + j + js] * xDer[(i + is) * w + j + js];
              B[i * w + j] += gaussFactor[((shift + is) * param.windowSize) + shift + js] * xDer[(i + is) * w + j + js] * yDer[(i + is) * w + j + js];
              C[i * w + j] += gaussFactor[((shift + is) * param.windowSize) + shift + js] * yDer[(i + is) * w + j + js] * yDer[(i + is) * w + j + js];
              H[i * w + j] = (A[i * w + j] * C[i * w + j] - B[i * w + j] * B[i * w + j]) - (this.param.sensitivity * (A[i * w + j] + C[i * w + j]) * (A[i * w + j] + C[i * w + j]));
            }
          }
        }
      }
    }
    return linearNormalize(H, w, h);
  }

  /**
   * linear normalization & thresholding
   *
   * @return greyscale BufferedImage TYPE_INT_RGB, normalized linearly and
   * thresholded (using param.threshold)
   */
  protected BufferedImage linearNormalize(double H[], int w, int h) {
    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    int[] newPx = new int[3];

    //calculate maximum
    double maxH = 0;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        if (H[i * w + j] > maxH) {
          maxH = H[i * w + j];
        }
      }
    }

    //normalize & blackout thresholds
    double q = 255 / maxH;
    int newint = 0;
    for (int i = 0; i < h; i++) {
      for (int j = 0; j < w; j++) {
        newint = 0;
        if (H[i * w + j] > 0) {
          newint = (int) (H[i * w + j] * q);
        }
        if (newint < param.threshold) {
          newint = 0;
        }
        newPx[0] = newPx[1] = newPx[2] = newint;
        output.getRaster().setPixel(j, i, newPx);
      }
    }

    return output;
  }
  
  
  private JTextField sensitivityInput = new JTextField();

  @Override
  public boolean confirmDialog() {
    this.param.sensitivity = Double.parseDouble(sensitivityInput.getText());
    return true;
  }

  @Override
  public JDialog getParamSettingDialog() {
    JPanel content = new JPanel();
    TableLayout layout = new TableLayout(new double[]{200, 100}, new double[]{20});
    content.setLayout(layout);

    content.add(new JLabel("Set sensitivity parameter: "), "0,0");
    content.add(sensitivityInput, "1,0");

    return CWorkerDialogFactory.createOkCancelDialog(this, content);
  }
}
