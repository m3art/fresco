/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import utils.vector.CBasic;

public class Crgb2hsv extends CImageConverter {

	protected int[] convertPixel(int[] rgb) {
		return convert(rgb);
	}

	/**
	 * this function converts rgb color to hsv. Input colors has range 0-255
	 * output colors are integers with range - hue 0-359
	 * saturation and value 0-255
	 * @param rgb is input color
	 * @return hsv representation of input colors
	 */
	public static int[] convert(int[] rgb) {
		int max, min;
		int[] hsv = new int[3];

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
		int hi, p, q, t;
		int[] rgb = new int[rgb_bands];
		double[] hsvd = new double[hsv_bands];
		double f;

		hsvd[0] = (double) hsv[0];
		hsvd[1] = (double) (hsv[1]) / 255;
		hsvd[2] = (double) (hsv[2]) / 255;

		hi = (hsv[0] / 60) % 6; //2
		f = hsvd[0] / 60 - hi;
		p = (int) ((hsvd[2] * (1 - hsvd[1])) * 255);
		q = (int) ((hsvd[2] * (1 - hsvd[1] * f)) * 255);
		t = (int) ((hsvd[2] * (1 - (1 - f) * hsvd[1])) * 255);

		switch (hi) {
			case 0:
				rgb[0] = (int) (hsvd[2] * 255);
				rgb[1] = t;
				rgb[2] = p;
				break;
			case 1:
				rgb[0] = q;
				rgb[1] = (int) (hsvd[2] * 255);
				rgb[2] = p;
				break;
			case 2:
				rgb[0] = p;
				rgb[1] = (int) (hsvd[2] * 255);
				rgb[2] = t;
				break;
			case 3:
				rgb[0] = p;
				rgb[1] = q;
				rgb[2] = (int) (hsvd[2] * 255);
				break;
			case 4:
				rgb[0] = t;
				rgb[1] = p;
				rgb[2] = (int) (hsvd[2] * 255);
				break;
			case 5:
				rgb[0] = (int) (hsvd[2] * 255);
				rgb[1] = p;
				rgb[2] = q;
				break;
		}
		return rgb;
	}

	public static double[] inverse(double[] hsv) {
		int hi, p, q, t;
		double[] rgb = new double[rgb_bands];
		double[] hsvd = new double[hsv_bands];
		double f;

		hsvd[0] = hsv[0] * 360 / 256;
		hsvd[1] = hsv[1] / 255;
		hsvd[2] = hsv[2] / 255;

		hi = (int) (hsvd[0]) / 60;
		f = hsvd[0] / 60 - hi;
		p = (int) (255 * hsvd[2] * (1 - hsvd[1]));
		q = (int) (255 * hsvd[2] * (1 - hsvd[1] * f));
		t = (int) (255 * hsvd[2] * (1 - (1 - f) * hsvd[1]));

		switch (hi) {
			case 0:
				rgb[0] = (int) (255 * hsvd[2]);
				rgb[1] = t;
				rgb[2] = p;
				break;
			case 1:
				rgb[0] = q;
				rgb[1] = (int) (255 * hsvd[2]);
				rgb[2] = p;
				break;
			case 2:
				rgb[0] = p;
				rgb[1] = (int) (255 * hsvd[2]);
				rgb[2] = t;
				break;
			case 3:
				rgb[0] = p;
				rgb[1] = q;
				rgb[2] = (int) (255 * hsvd[2]);
				break;
			case 4:
				rgb[0] = t;
				rgb[1] = p;
				rgb[2] = (int) (255 * hsvd[2]);
				break;
			case 5:
				rgb[0] = (int) (255 * hsvd[2]);
				rgb[1] = p;
				rgb[2] = q;
				break;
		}
		return rgb;
	}

	/**
	 * Hue is defined like an angle so if you have values 30 and 330
	 * their distance is 60!
	 * @param rgb in range [0,255]
	 * @return hue in range [0,360] this value is rounded
	 */
	public static int getHue(int[] rgb) {
		int max, min;
		int out = 0;

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
