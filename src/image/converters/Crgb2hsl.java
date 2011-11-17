/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import utils.vector.CBasic;
/* static class for conversion between rgb colors and hsl */

public class Crgb2hsl extends CImageConverter {

	public static int[] convert(int[] rgb) {
		int min, max;
		int[] hsl = new int[3];

		max = CBasic.max(rgb);
		min = CBasic.min(rgb);

		hsl[2] = (max + min) / 2; //set lightness
		if (max - min < 2) {
			hsl[1] = 0; //set saturation
		} else if (hsl[2] <= 128) {
			hsl[1] = Math.min(255 * (max - min) / (2 * hsl[2]), 256); //set saturation
		} else {
			hsl[1] = 255 * (max - min) / (511 - 2 * hsl[2]); // set saturation
		}
		if (max == min) {
			hsl[0] = 0; //set hue
		} else if (max == rgb[0]) {
			hsl[0] = (60 * (rgb[1] - rgb[2]) / (max - min) + 0); //set hue
			if (hsl[0] < 0) {
				hsl[0] += 360;
			}
		} else if (max == rgb[1]) {
			hsl[0] = (60 * (rgb[2] - rgb[0]) / (max - min) + 120); //set hue
		} else if (max == rgb[2]) {
			hsl[0] = (60 * (rgb[0] - rgb[1]) / (max - min) + 240); //set hue
		}
		return hsl;
	}

	public static int[] inverse(int[] hsl) {
		int[] rgb = new int[rgb_bands];
		double[] t = new double[rgb_bands];
		double h, s, l, q, p, hk;
		int k;

		if (hsl[1] == 0) {
			return rgb;
		}

		h = hsl[0];
		s = (double) (hsl[1]) / 256;
		l = (double) (hsl[2]) / 256;

		if (l < 0.5) {
			q = l * (1 + s);
		} else {
			q = l + s - (l * s);
		}
		p = 2 * l - q;
		hk = h / 360;
		t[0] = hk + 1 / 3;
		t[1] = hk;
		t[2] = hk + 1 / 3;

		for (k = 0; k < rgb_bands; k++) {
			if (t[k] < 1.0 / 6) {
				rgb[k] = (int) (255 * (p + ((q - p) * 6 * t[k])));
			} else if (t[k] < 0.5) {
				rgb[k] = (int) (255 * q);
			} else if (t[k] < 2.0 / 3) {
				rgb[k] = (int) (255 * (p + ((q - p) * 6 * (2.0 / 3 - t[k]))));
			} else {
				rgb[k] = (int) (255 * p);
			}
		}

		return rgb;
	}

	public static int[][][] convertImage(int[][][] image) {
		int x, y;

		int[][][] out = new int[image.length][image[0].length][hsl_bands];

		for (x = 0; x < image.length; x++) {
			for (y = 0; y < image[0].length; y++) {
				out[x][y] = convert(image[x][y]);
			}
		}
		return out;
	}

	@Override
	protected int[] convertPixel(int[] pixel) {
		return convert(pixel);
	}
}
