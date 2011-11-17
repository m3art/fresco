/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.correction;

import image.converters.Crgb2uvY;
import image.converters.IConstants;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import utils.vector.CBasic;

/**
 * By the article - Virtual Restoration of Ancient Chinese Paintings
 * Using Color Contrast Enhancement and
 * Lacuna Texture Synthesis
 * Soo-Chang Pei, Fellow, IEEE, Yi-Chong Zeng, and Ching-Hua Chang
 * @author Gimli
 */
public class CContrastEnhancement extends CCorrectionWorker {

	Raster original;
	static double[] //Cw = {0.1978,   0.4683},
			Cw = {4.0 / 19, 9.0 / 19},
			Cr = {0.4507, 0.5229},
			Cg = {0.1250, 0.5625},
			Cb = {0.1754, 0.1579},
			RW = getLineParametres(Cr, Cw), RG = getLineParametres(Cr, Cg), RB = getLineParametres(Cr, Cb),
			GW = getLineParametres(Cg, Cw), GB = getLineParametres(Cg, Cb),
			BW = getLineParametres(Cb, Cw),
			RGp = {RG[0], (Cw[1] + Cr[1]) / 2 - RG[0] * (Cw[0] + Cr[0]) / 2},
			RBp = {RB[0], (Cw[1] + Cr[1]) / 2 - RB[0] * (Cw[0] + Cr[0]) / 2},
			GBp = {GB[0], (Cw[1] + Cb[1]) / 2 - GB[0] * (Cw[0] + Cb[0]) / 2};
	static int AHE_width = 8;
	static double k = 0.1;
	double Yw;
	private int region_size;

	public CContrastEnhancement(BufferedImage input, int region_size) {
		original = input.getData();
		this.region_size = region_size;
	}

	public CContrastEnhancement(BufferedImage input) {
		original = input.getData();
		this.region_size = 150;
	}

	@Override
	protected BufferedImage doInBackground() {
		double[][][] uvY = new double[original.getWidth()][original.getHeight()][];
		double[] rgb = new double[IConstants.rgb_bands];
		int x, y, change = 0;
		BufferedImage output = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster out = output.getRaster();


		Yw = 0; // clear mean luminiscence
		// translate colors to uvY color space
		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				original.getPixel(x, y, rgb);
				rgb = CBasic.scalar(1.0 / 255, rgb);          // scale colors between 0,1
				uvY[x][y] = Crgb2uvY.convert(rgb);          // count uvY colors
				Yw += uvY[x][y][2];                         // update mean luminiscence
				setProgress((x * original.getHeight() + y) * 33 / (original.getHeight() * original.getWidth()));
			}
		}
		Yw /= original.getHeight() * original.getHeight();
		Yw *= k;

		// saturation and desaturation algorithm
		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				if (isUp(RGp, uvY[x][y]) || !isUp(GBp, uvY[x][y]) || !isUp(RBp, uvY[x][y])) {
					saturate(uvY[x][y]);
					desaturate(uvY[x][y]);
					change++;
				}
				uvY[x][y][2] = Math.min(Yw + uvY[x][y][2], 1.0);
				setProgress(((x * original.getHeight() + y) * 33) / (original.getHeight() * original.getWidth()) + 33);
			}
		}

		// AHE algorithm
		uvY = (new CAdaptiveHistogramEnhancing(uvY, 2, region_size)).AHE();

		// transfer colors back to RGB
		for (x = 0; x < original.getWidth(); x++) {
			for (y = 0; y < original.getHeight(); y++) {
				out.setPixel(x, y, CBasic.scalar(255, Crgb2uvY.inverse(uvY[x][y])));
				setProgress(((x * original.getHeight() + y) * 33) / (original.getHeight() * original.getWidth()) + 67);
			}
		}
		output.setData(out);
		System.out.format("Zmeneno %.1f%c pixelů", (double) (change) * 100 / (original.getHeight() * original.getWidth()), '%');
		return output;
	}

	private static double[] getLineParametres(double[] y1, double[] y2) {
		double[] out = {(y2[1] - y1[1]) / (y2[0] - y1[0]), (y1[1] * y2[0] - y1[0] * y2[1]) / (y2[0] - y1[0])};
		return out;
	}

	private static double[] getIntersectionPoint(double[] y1, double[] y2) {
		double[] out = {(y2[1] - y1[1]) / (y1[0] - y2[0]), (y2[1] * y1[0] - y2[0] * y1[1]) / (y1[0] - y2[0])};
		return out;
	}

	private double[] saturate(double[] uvY) {
		// TODO: pridat jako treti slozku vystupu uvY[2]!!!
		double[] pB = getLineParametres(uvY, Cw);

		if (isUp(RW, uvY) && isUp(GW, uvY)) {
			System.arraycopy(getIntersectionPoint(pB, RG), 0, uvY, 0, 2);
		} else if (!isUp(RW, uvY) && !isUp(BW, uvY)) {
			System.arraycopy(getIntersectionPoint(pB, RB), 0, uvY, 0, 2);
		} else {
			System.arraycopy(getIntersectionPoint(pB, GB), 0, uvY, 0, 2);
		}

		return uvY;
	}

	private boolean isUp(double[] line, double[] point) {
		return line[0] * point[0] + line[1] - point[1] < 0;
	}

	private double[] desaturate(double[] uvY) {
		uvY[0] = (uvY[0] * uvY[2] / uvY[1] + Cw[0] * Yw / Cw[1]) / (uvY[2] / uvY[1] + Yw / Cw[1]);
		uvY[1] = (uvY[2] + Yw) / (uvY[2] / uvY[1] + Yw / Cw[1]);

		return uvY;
	}

	@Override
	public String getWorkerName() {
		return "Contrast Enhancement";
	}
}
