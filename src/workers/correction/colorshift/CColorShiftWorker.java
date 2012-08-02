/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction.colorshift;

import fresco.CImageContainer;
import fresco.swing.CWorkerDialogFactory;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;
import image.statistics.CHistogram;
import info.clearthought.layout.TableLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.Raster;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import workers.correction.CCorrectionWorker;

/**
 *
 * @author gimli
 */
public class CColorShiftWorker extends CCorrectionWorker {

	BufferedImage shifted;
	Raster image;
	BufferedImage pattern;
	double[] dataMean, dataVar, targetMean, targetVar;
	static final Logger logger = Logger.getLogger(CColorShiftWorker.class.getName());

	public CColorShiftWorker(CImageContainer original) {
		image = original.getImage().getData();

		dataMean = CHistogram.getMean(image);
		dataVar = CHistogram.getVar(image, dataMean);
		targetMean = new double[3];
		targetVar = new double[3];
	}

	@Override
	public String getWorkerName() {
		return "Color shift worker";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		if (targetMean == null && pattern == null) {
			throw new CancellationException("Dialog cancelled");
		}

		setProgress(50);

		WritableRaster outRaster = shifted.getRaster();
		int size = outRaster.getWidth() * outRaster.getHeight() * outRaster.getNumBands();
		int[] pixels = new int[size];

		image.getPixels(0, 0, image.getWidth(), image.getHeight(), pixels);

		for (int i = 0; i < size; i += outRaster.getNumBands()) {
			for (int b = 0; b < outRaster.getNumBands(); b++) {
				pixels[i + b] = (int) (((double) pixels[i + b] - dataMean[b]) / dataVar[b] * targetVar[b] + targetMean[b]);
				pixels[i + b] = Math.min(Math.max(0, pixels[i + b]), 255);
			}
			setProgress(50 + i * 50 / size);
		}

		outRaster.setPixels(0, 0, outRaster.getWidth(), outRaster.getHeight(), pixels);
		shifted.setData(outRaster);

		return shifted;
	}

	@Override
	public boolean confirmDialog() {
		try {
			targetMean[0] = Double.valueOf(redValue.getText());
			targetMean[1] = Double.valueOf(greenValue.getText());
			targetMean[2] = Double.valueOf(blueValue.getText());

			targetVar[0] = Double.valueOf(redVarValue.getText());
			targetVar[1] = Double.valueOf(greenVarValue.getText());
			targetVar[2] = Double.valueOf(blueVarValue.getText());

			shifted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			return true;
		} catch (NumberFormatException nfe) {
			logger.warning("Input value for region size is not a valid integer.");
			return false;
		}
	}

	private JTextField redValue, redVarValue, greenValue, greenVarValue, blueValue, blueVarValue;

	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();
		JPanel inputs = new JPanel();

		//inputs.setPreferredSize(new Dimension(200, 400));
		JLabel targetLabel = new JLabel("Target mean value (range 0-255):");

		JLabel redLabel = new JLabel("Red colour: ");
		redValue = new JTextField(String.format("%.2f", dataMean[0]));
		redValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel redVarLabel = new JLabel("Red colour variance: ");
		redVarValue = new JTextField(String.format("%.2f", dataVar[0]));
		redVarValue.setHorizontalAlignment(JTextField.RIGHT);

		JLabel greenLabel = new JLabel("Green colour mean: ");
		greenValue = new JTextField(String.format("%.2f", dataMean[1]));
		greenValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel greenVarLabel = new JLabel("Green colour variance: ");
		greenVarValue = new JTextField(String.format("%.2f", dataVar[1]));
		greenVarValue.setHorizontalAlignment(JTextField.RIGHT);

		JLabel blueLabel = new JLabel("Blue colour mean: ");
		blueValue = new JTextField(String.format("%.2f", dataMean[2]));
		blueValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel blueVarLabel = new JLabel("Blue colour variance: ");
		blueVarValue = new JTextField(String.format("%.2f", dataVar[2]));
		blueVarValue.setHorizontalAlignment(JTextField.RIGHT);

		TableLayout layout = new TableLayout(new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5},new double[]{5, TableLayout.FILL, 5, TableLayout.FILL, 5, TableLayout.FILL, 5, TableLayout.FILL, 5, TableLayout.FILL, 5, TableLayout.FILL, 5});

		inputs.setLayout(layout);
		inputs.add(redLabel, "1, 1");
		inputs.add(redValue, "3, 1");
		inputs.add(redVarLabel, "1, 3");
		inputs.add(redVarValue, "3, 3");
		inputs.add(greenLabel, "1, 5");
		inputs.add(greenValue, "3, 5");
		inputs.add(greenVarLabel, "1, 7");
		inputs.add(greenVarValue, "3, 7");
		inputs.add(blueLabel, "1, 9");
		inputs.add(blueValue, "3, 9");
		inputs.add(blueVarLabel, "1, 11");
		inputs.add(blueVarValue, "3, 11");

		content.setLayout(new BorderLayout(5,5));
		content.add(targetLabel, BorderLayout.NORTH);
		content.add(inputs, BorderLayout.CENTER);

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}
}
