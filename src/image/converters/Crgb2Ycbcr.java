package image.converters;

/**
 * @author gimli
 * @version Aug 2, 2012
 */
public class Crgb2Ycbcr {

	/**
	 * Conversion from RGB colour space into YCbCr colour space
	 * @param rgb values between [0;255]
	 * @return YCbCr in range [0;255]
	 */
	public static double[] convert(double[] rgb) {
		double[] yCbCr = new double[3];

		yCbCr[0] = + 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
		yCbCr[1] = 128 - 0.168736 * rgb[0] - 0.331264 * rgb[1] + 0.5 * rgb[2];
		yCbCr[2] = 128 + 0.5 * rgb[0] - 0.418688 * rgb[1] - 0.081312 * rgb[2];

		return yCbCr;
	}

	public static double[] inverse(double[] yCbCr) {
		double[] rgb = new double[3];

		rgb[0] = yCbCr[0] + 1.402 * (yCbCr[2] -128);
		rgb[1] = yCbCr[0] - 0.34414 * (yCbCr[1] - 128) + 0.71414 * (yCbCr[2] -128);
		rgb[2] = yCbCr[0] + 1.772 * (yCbCr[1] -128);

		return rgb;
	}

}
