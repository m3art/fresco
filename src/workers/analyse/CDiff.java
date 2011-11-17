/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */

package workers.analyse;

import image.converters.CNormalization;
import image.converters.Crgb2gray;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import utils.vector.CBasic;

/**
 *
 * @author gimli
 */
public class CDiff extends CAnalysisWorker {

	private BufferedImage imageA,imageB;
	private static final Logger logger = Logger.getLogger(CDiff.class.getName());

	public CDiff(BufferedImage imageA, BufferedImage imageB) {
		this.imageA = imageA;
		this.imageB = imageB;
	}

	@Override
	public String getWorkerName() {
		return "Diff analysis worker";
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		BufferedImage out = new BufferedImage(imageA.getWidth(), imageA.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Raster in1,in2;
		WritableRaster output = out.getRaster();
		int x,y;
		int[] pixA = new int[3], pixB = new int[3];

		if (imageA.getWidth() != imageB.getWidth() || imageA.getHeight() != imageB.getHeight()) {
            JOptionPane.showMessageDialog(new JFrame(), "Input images must have the same size!\n" +
					"Possible solution:\n" +
					"1) Resize images in external software\n" +
					"2) Use Transform > Perspective transformation for correct transform.", "Image difference stopped", JOptionPane.WARNING_MESSAGE);
			return null;
		}

        in1 = CNormalization.normalize((new Crgb2gray()).convert(imageA), 128, 64).getData();
        in2 = CNormalization.normalize((new Crgb2gray()).convert(imageB), 128, 64).getData();
		for(x=0;x<in1.getWidth(); x++)
			for(y=0;y<in1.getHeight(); y++) {
				in1.getPixel(x, y, pixA);
				in2.getPixel(x, y, pixB);
				output.setPixel(x, y, CBasic.diffAbs(pixA, pixB));
			}
		out.setData(output);
		//return CHeatMap.convert(out);
		return CNormalization.normalize(out, 128, 64);
	}

}
