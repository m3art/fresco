/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.image.BufferedImage;
import utils.vector.CBasic;

public class Crgb2hsv extends CImageConverter {

	@Override
	protected int[] convertPixel(int[] rgb) {
		return convert(rgb);
	}

	/**
	 * Converts Buffered image in format RGB into double array with Hue Saturation
	 * and Value dimensions
	 * @param rgbImage converted image
	 * @return HSV matrix representation of image
	 */
	public static int[][][] convertImage(BufferedImage rgbImage){
		int[][][] out = new int[rgbImage.getWidth()][rgbImage.getHeight()][];

		int[][][] rgb = CBufferedImageToIntArray.convert(rgbImage);

		for(int x=0; x<rgbImage.getWidth(); x++)
			for(int y=0; y<rgbImage.getHeight(); y++) {
				out[x][y] = convert(rgb[x][y]);
			}

		return out;
	}

	public static double[][][] convertImageToDouble(BufferedImage rgbImage) {
		double[][][] out = new double[rgbImage.getWidth()][rgbImage.getHeight()][];

		double[][][] rgb = CBufferedImageToDoubleArray.convert(rgbImage);

		for(int x=0; x<rgbImage.getWidth(); x++)
			for(int y=0; y<rgbImage.getHeight(); y++) {
				out[x][y] = convert(rgb[x][y]);
			}

		return out;
	}

	/**
	 * this function converts rgb color to hsv. Input colors has range 0-255
	 * output colors are integers with range - hue 0-359
	 * saturation and value 0-255
	 * @param rgb is input color
	 * @return hsv representation of input colors
	 */
	public static int[] convert(int[] rgb) {

		double[] rgbDouble = new double[rgb.length];
		for(int i=0; i<rgb.length; i++)
			rgbDouble[i] = rgb[i];

		double[] outDouble;

		outDouble = convert(rgbDouble);
		int[] outInt = new int[outDouble.length];
		for(int i=0; i< outInt.length; i++) {
			outInt[i] = (int)Math.round(outDouble[i]);
		}

		return outInt;
	}

	/**
	 * this function converts rgb color to hsv. Input colors has range 0-255
	 * output colors are integers with range - hue 0-359
	 * saturation and value 0-255
	 * @param rgb is input color
	 * @return hsv representation of input colors
	 */
	public static double[] convert(double[] rgb) {
		double max, min;
		double[] hsv = new double[3];

		//rgb = CVectorWorker.scalar(1.0/255, rgb);
		max = CBasic.max(rgb);
		min = CBasic.min(rgb);

		hsv[2] = max; //set value
		if (max == 0) {
			hsv[1] = 0; //set saturation
		} else {
			hsv[1] = 255 * (max - min) / max; //set saturation
		}
		if (max == min) {
			hsv[0] = 0; //set hue
		} else if (max == rgb[0]) {
			hsv[0] = (60 * (rgb[1] - rgb[2]) / (max - min) + 0); //set hue
			if (hsv[0] < 0) {
				hsv[0] += 360;
			}
		} else if (max == rgb[1]) {
			hsv[0] = (60 * (rgb[2] - rgb[0]) / (max - min) + 120); //set hue
		} else if (max == rgb[2]) {
			hsv[0] = (60 * (rgb[0] - rgb[1]) / (max - min) + 240); //set hue
		}
		return hsv;
	}

	public static int[] inverse(int[] hsv) {
		double[] hsvd = {hsv[0], hsv[1], hsv[2]};
		double[] result = inverse(hsvd);

		return new int[]{(int)Math.round(result[0]), (int)Math.round(result[1]), (int)Math.round(result[2])};
	}

	/**
	 * Expected input of inverse transformation is hue [0,360], saturation [0,255]
	 * value [0,255]
	 * @param hsv
	 * @return
	 */
	public static double[] inverse(double[] hsv) {
		int hi;
		double p, q, t;
		double[] rgb = new double[rgb_bands];
		double[] hsvd = new double[hsv_bands];
		double f;

		hsvd[0] = hsv[0];
		hsvd[1] = hsv[1] / 255.0;
		hsvd[2] = hsv[2] / 255.0;

		hi = (int) ((hsvd[0]) / 60) % 6;
		f = hsvd[0] / 60 - hi;
		p = 255 * hsvd[2] * (1 - hsvd[1]);
		q = 255 * hsvd[2] * (1 - hsvd[1] * f);
		t = 255 * hsvd[2] * (1 - (1 - f) * hsvd[1]);

		switch (hi) {
			case 0:
				rgb[0] = hsv[2];
				rgb[1] = t;
				rgb[2] = p;
				break;
			case 1:
				rgb[0] = q;
				rgb[1] = hsv[2];
				rgb[2] = p;
				break;
			case 2:
				rgb[0] = p;
				rgb[1] = hsv[2];
				rgb[2] = t;
				break;
			case 3:
				rgb[0] = p;
				rgb[1] = q;
				rgb[2] = hsv[2];
				break;
			case 4:
				rgb[0] = t;
				rgb[1] = p;
				rgb[2] = hsv[2];
				break;
			case 5:
				rgb[0] = hsv[2];
				rgb[1] = p;
				rgb[2] = q;
				break;
		}
		return rgb;
	}

	/**
	 * Convert matrix width*height*band from hsv colour space into RGB
	 * @param hsvImage input matrix
	 * @return RGB image
	 */
	public static int[][][] inverse(int[][][] hsvImage) {
		int[][][] rgbImage = new int[hsvImage.length][hsvImage[0].length][];

		for(int x=0; x<hsvImage.length; x++) {
			for(int y=0; y<hsvImage[0].length; y++) {
				rgbImage[x][y] = inverse(hsvImage[x][y]);
			}
		}

		return rgbImage;
	}

	/**
	 * Convert matrix width*height*band from hsv colour space into RGB
	 * @param hsvImage input matrix
	 * @return RGB image
	 */
	public static double[][][] inverse(double[][][] hsvImage) {
		double[][][] rgbImage = new double[hsvImage.length][hsvImage[0].length][];

		for(int x=0; x<hsvImage.length; x++) {
			for(int y=0; y<hsvImage[0].length; y++) {
				rgbImage[x][y] = inverse(hsvImage[x][y]);
			}
		}

		return rgbImage;
	}

	/**
	 * Hue is defined like an angle so if you have values 30 and 330
	 * their distance is 60!
	 * @param rgb in range [0,255]
	 * @return hue in range [0,360] this value is rounded
	 */
	public static double getHue(double[] rgb) {
		double max, min;
		double out = 0;

		//rgb = CVectorWorker.scalar(1.0/255, rgb);
		max = CBasic.max(rgb);
		min = CBasic.min(rgb);

		if (max == min) {
			return out;
		}
		if (max == rgb[0]) {
			out = 60 * (rgb[1] - rgb[2]) / (max - min); //set hue
			if (out < 0) {
				out += 360;
			}
		} else if (max == rgb[1]) {
			out = (60 * (rgb[2] - rgb[0]) / (max - min) + 120); //set hue
		} else if (max == rgb[2]) {
			out = (60 * (rgb[0] - rgb[1]) / (max - min) + 240); //set hue
		}
		return out;
	}
}
