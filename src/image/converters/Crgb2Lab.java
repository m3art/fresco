/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

/**
 *
 * @author gimli
 */
public final class Crgb2Lab {

	public static enum WhitePoint {

		D50, D55, D65, D75
	};
	/**
	 * reference white in XYZ coordinates
	 */
	private final double[] D50 = {96.4212, 100.0, 82.5188};
	private final double[] D55 = {95.6797, 100.0, 92.1481};
	private final double[] D65 = {95.0429, 100.0, 108.8900};
	private final double[] D75 = {94.9722, 100.0, 122.6394};
	private final double[] whitePoint;
	/**
	 * reference white in xyY coordinates
	 */
	private final double[] chromaD50 = {0.3457, 0.3585, 100.0};
	private final double[] chromaD55 = {0.3324, 0.3474, 100.0};
	private final double[] chromaD65 = {0.3127, 0.3290, 100.0};
	private final double[] chromaD75 = {0.2990, 0.3149, 100.0};
	private final double[] chromaWhitePoint;
	/**
	 * sRGB to XYZ conversion matrix source
	 * {@link http://en.wikipedia.org/wiki/SRGB_color_space}
	 */
	private static final double[][] sRgb2Xyz = {{0.4124, 0.3576, 0.1805},
		{0.2126, 0.7152, 0.0722},
		{0.0193, 0.1192, 0.9505}};
	/**
	 * XYZ to sRGB conversion matrix source
	 * {@link http://en.wikipedia.org/wiki/SRGB_color_space}
	 */
	private final static double[][] xyz2sRgb = {{3.2406, -1.5372, -0.4986},
		{-0.9689, 1.8758, 0.0415},
		{0.0557, -0.2040, 1.0570}};

	/**
	 * @param whitePoint specify one of predefined standards {
	 * @see WhitePoint}
	 */
	public Crgb2Lab(WhitePoint whitePoint) {
		switch (whitePoint) {
			case D50:
				this.whitePoint = D50;
				chromaWhitePoint = chromaD50;
				break;
			case D55:
				this.whitePoint = D55;
				chromaWhitePoint = chromaD55;
				break;
			case D75:
				this.whitePoint = D75;
				chromaWhitePoint = chromaD75;
				break;
			default:
				this.whitePoint = D65;
				chromaWhitePoint = chromaD65;
				break;
		}
	}

	/**
	 * Convert RGB to XYZ
	 *
	 * @param sRgb color in range [0,255]^3
	 * @return XYZ in double array.
	 */
	public static double[] sRgb2Xyz(double[] sRgb) {
		double[] result = new double[3];

		// convert 0..255 into 0..1
		double red = sRgb[0] / 255.0;
		double green = sRgb[1] / 255.0;
		double blue = sRgb[2] / 255.0;

		// assume sRGB
		if (red <= 0.04045) {
			red = red / 12.92;
		} else {
			red = Math.pow(((red + 0.055) / 1.055), 2.4);
		}
		if (green <= 0.04045) {
			green = green / 12.92;
		} else {
			green = Math.pow(((green + 0.055) / 1.055), 2.4);
		}
		if (blue <= 0.04045) {
			blue = blue / 12.92;
		} else {
			blue = Math.pow(((blue + 0.055) / 1.055), 2.4);
		}

		red *= 100.0;
		green *= 100.0;
		blue *= 100.0;

		// [X Y Z] = [r g b][sRgb2Xyz]
		result[0] = (red * sRgb2Xyz[0][0]) + (green * sRgb2Xyz[0][1]) + (blue * sRgb2Xyz[0][2]);
		result[1] = (red * sRgb2Xyz[1][0]) + (green * sRgb2Xyz[1][1]) + (blue * sRgb2Xyz[1][2]);
		result[2] = (red * sRgb2Xyz[2][0]) + (green * sRgb2Xyz[2][1]) + (blue * sRgb2Xyz[2][2]);

		return result;
	}

	/**
	 * Convert XYZ to sRGB.
	 *
	 * @param xyz color in XYZ color space
	 * @return RGB in double array in range [0,255].
	 */
	public static double[] xyz2sRgb(double[] xyz) {		

		double x = xyz[0] / 100.0;
		double y = xyz[1] / 100.0;
		double z = xyz[2] / 100.0;

		// RGB = A*XYZ
		double red = (x * xyz2sRgb[0][0]) + (y * xyz2sRgb[0][1]) + (z * xyz2sRgb[0][2]);
		double green = (x * xyz2sRgb[1][0]) + (y * xyz2sRgb[1][1]) + (z * xyz2sRgb[1][2]);
		double blue = (x * xyz2sRgb[2][0]) + (y * xyz2sRgb[2][1]) + (z * xyz2sRgb[2][2]);

		// assume sRGB
		if (red > 0.0031308) {
			red = ((1.055 * Math.pow(red, 1.0 / 2.4)) - 0.055);
		} else {
			red = (red * 12.92);
		}
		if (green > 0.0031308) {
			green = ((1.055 * Math.pow(green, 1.0 / 2.4)) - 0.055);
		} else {
			green = (green * 12.92);
		}
		if (blue > 0.0031308) {
			blue = ((1.055 * Math.pow(blue, 1.0 / 2.4)) - 0.055);
		} else {
			blue = (blue * 12.92);
		}

		red = (red < 0) ? 0 : red;
		green = (green < 0) ? 0 : green;
		blue = (blue < 0) ? 0 : blue;
		
		return new double[]{red*255, green*255, blue*255};
	}

	/**
	 * Convert XYZ to LAB.
	 *
	 * @param xyz color in XYZ color space
	 * @return Lab values
	 */
	public double[] xyz2Lab(double[] xyz) {

		double x = xyz[0] / whitePoint[0];
		double y = xyz[1] / whitePoint[1];
		double z = xyz[2] / whitePoint[2];

		if (x > 0.008856) {
			x = Math.pow(x, 1.0 / 3.0);
		} else {
			x = (7.787 * x) + (16.0 / 116.0);
		}
		if (y > 0.008856) {
			y = Math.pow(y, 1.0 / 3.0);
		} else {
			y = (7.787 * y) + (16.0 / 116.0);
		}
		if (z > 0.008856) {
			z = Math.pow(z, 1.0 / 3.0);
		} else {
			z = (7.787 * z) + (16.0 / 116.0);
		}

		double[] lab = new double[3];

		lab[0] = (116.0 * y) - 16.0;
		lab[1] = 500.0 * (x - y);
		lab[2] = 200.0 * (y - z);

		return lab;
	}
	
	/**
     * Convert Lab to XYZ.
     * @param lab color in Lab color space
     * @return XYZ values
     */
    public double[] lab2Xyz(double[] Lab) {      
      double y = (Lab[0] + 16.0) / 116.0;
      double y3 = Math.pow(y, 3.0);
      double x = (Lab[1] / 500.0) + y;
      double x3 = Math.pow(x, 3.0);
      double z = y - (Lab[2] / 200.0);
      double z3 = Math.pow(z, 3.0);

      if (y3 > 0.008856) {
        y = y3;
      }
      else {
        y = (y - (16.0 / 116.0)) / 7.787;
      }
      if (x3 > 0.008856) {
        x = x3;
      }
      else {
        x = (x - (16.0 / 116.0)) / 7.787;
      }
      if (z3 > 0.008856) {
        z = z3;
      }
      else {
        z = (z - (16.0 / 116.0)) / 7.787;
      }

	  return new double[] {x * whitePoint[0], y * whitePoint[1], z * whitePoint[2]};      
    }
	
	public double[] sRgb2Lab(double[] rgb) {
		return xyz2Lab(sRgb2Xyz(rgb));
	}
	
	public double[] lab2sRgb(double[] lab) {
		return xyz2sRgb(lab2Xyz(lab));
	}
	
	public static double[] lab2Msh(double[] lab) {
		double m, s, h;

		h = Math.atan(lab[2] / lab[1]);

		double c = Math.sqrt(Math.pow(lab[1], 2) + Math.pow(lab[2], 2));

		s = Math.PI/2 - Math.atan(lab[0] / c);

		m = Math.sqrt(Math.pow(c, 2) + Math.pow(lab[0], 2));

		return new double[] {m,s,h};
	}
	
	public static double[] msh2Lab(double[] msh) {
		double l, a, b;
		
		l = msh[0] * Math.sin(Math.PI/2 - msh[1]);
		
		double c = Math.sqrt(Math.pow(msh[0],2) - Math.pow(l,2));
		
		a = c * Math.cos(msh[2]);
		
		b = c * Math.sin(msh[2]);	
		
		return new double[] {l, a, b};
	}
}
