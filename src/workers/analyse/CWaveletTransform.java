/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import image.converters.CBufferedImageToIntArray;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 *
 * @author gimli
 */
public class CWaveletTransform {

	public static BufferedImage transform(BufferedImage original, int depth) {
		int[][][] data = CBufferedImageToIntArray.convert(original);
		int x, y, b;
		int[][] band_data = new int[original.getWidth()][original.getHeight()];

		for (b = 0; b < original.getData().getNumBands(); b++) {
			for (x = 0; x < original.getWidth(); x++) {
				for (y = 0; y < original.getHeight(); y++) {
					band_data[x][y] = data[x][y][b];
				}
			}
			band_data = transform(band_data, depth);
			for (x = 0; x < original.getWidth(); x++) {
				for (y = 0; y < original.getHeight(); y++) {
					data[x][y][b] = band_data[x][y];
				}
			}
		}
		return CBufferedImageToIntArray.inverse(data);
	}

	public static BufferedImage inverse(BufferedImage transformed, int depth) {
		int[][][] data = CBufferedImageToIntArray.convert(transformed);
		int x, y, b;
		int[][] band_data = new int[transformed.getWidth()][transformed.getHeight()];

		for (b = 0; b < transformed.getData().getNumBands(); b++) {
			for (x = 0; x < transformed.getWidth(); x++) {
				for (y = 0; y < transformed.getHeight(); y++) {
					band_data[x][y] = data[x][y][b];
				}
			}
			band_data = inverse(band_data, depth);
			for (x = 0; x < transformed.getWidth(); x++) {
				for (y = 0; y < transformed.getHeight(); y++) {
					data[x][y][b] = band_data[x][y];
				}
			}
		}
		return CBufferedImageToIntArray.inverse(data);
	}

	public static int[][] transform(int[][] input, int depth) {
		int b, d, i, j, width = input.length, height = input[0].length, sum;

		int[][] pom = new int[width][height], out = new int[width][height];
		int[] pixel = new int[4];


		for (d = 0; d < depth; d++) {				// number of levels
			for (i = 0; i < width; i += 2) // step by two
			{
				for (j = 0; j < height; j += 2) {		// step by two
					sum = 0;
					for (b = 0; b < 4; b++) {
						if (d == 0) {
							if (i + (b % 2) < width && j + b / 2 < height) {
								pixel[b] = input[i + (b % 2)][j + (b / 2)];
							} else {
								pixel[b] = pixel[0];
							}
						} else if (i + (b % 2) < width && j + b / 2 < height) {
							pixel[b] = pom[i + b % 2][j + b / 2];
						} else {
							pixel[b] = pixel[0];
						}
						sum += pixel[b];
					}

					pom[i / 2][j / 2] = sum / 4;

					out[i / 2][j / 2] = pom[i / 2][j / 2];
					if (i / 2 + width / 2 + width % 2 < input.length) {
						out[i / 2 + width / 2 + width % 2][j / 2] = absolute(pixel[1] - pom[i / 2][j / 2]);
					}
					if (j / 2 + height / 2 + height % 2 < input[0].length) {
						out[i / 2][j / 2 + height / 2 + height % 2] = absolute(pixel[2] - pom[i / 2][j / 2]);
					}
					if (j / 2 + height / 2 + height % 2 < input[0].length && i / 2 + width / 2 + width % 2 < input.length) {
						out[i / 2 + width / 2 + width % 2][j / 2 + height / 2 + height % 2] = absolute(pixel[3] - pom[i / 2][j / 2]);
					}
				}
			}
			width = width / 2 + width % 2;
			height = height / 2 + height % 2;
		}

		return out;
	}

	public static int[][] inverse(int[][] transformed, int depth) {
		int b, d, i, j, k;
		Dimension size = new Dimension(transformed.length, transformed[0].length);

		int width = transformed.length,
				height = transformed[0].length;
		int[] pixel = new int[4];
		int[][] pom = new int[width][height], out = new int[width][height];
		int diff;

		for (d = 0; d < depth; d++) {
			width = (width >> 1) + width % 2;
			height = (height >> 1) + height % 2;
		}

		for (d = 0; d < depth; d++) {
			for (i = width - 1; i >= 0; i--) {
				for (j = height - 1; j >= 0; j--) {
					pixel[1] = (i + width < size.width) ? transformed[i + width][j] : 0;
					pixel[2] = (j + height < size.height) ? transformed[i][j + height] : 0;
					pixel[3] = (j + height < size.height
							&& i + width < size.width) ? transformed[i + width][j + height] : 0;
					pixel[0] = (d == 0) ? transformed[i][j] : pom[i][j];

					for (k = 1; k < 4; k++) {
						pixel[k] = relative(pixel[k]);
					}
					diff = Math.min(Math.max(0, pixel[0] - pixel[1] - pixel[2] - pixel[3]), 255);

					for (k = 1; k < 4; k++) {
						pixel[k] += pixel[0];
						pixel[k] = Math.min(Math.max(0, pixel[k]), 255);
					}

					pixel[0] = pixel[0] - (pixel[0] - diff);

					if (d == depth - 1) {
						if (2 * i < size.width && j * 2 < size.height) {
							out[2 * i][2 * j] = pixel[0];
						}
						if (2 * i + 1 < size.width && j * 2 < size.height) {
							out[2 * i + 1][2 * j] = pixel[1];
						}
						if (2 * i < size.width && j * 2 + 1 < size.height) {
							out[2 * i][2 * j + 1] = pixel[2];
						}
						if (2 * i + 1 < size.width && j * 2 + 1 < size.height) {
							out[2 * i + 1][2 * j + 1] = pixel[3];
						}
					} else {
						pom[2 * i][2 * j] = pixel[0];
						pom[2 * i + 1][2 * j] = pixel[1];
						pom[2 * i][2 * j + 1] = pixel[2];
						pom[2 * i + 1][2 * j + 1] = pixel[3];
					}
				}
			}
			width = (size.width >> (depth - 1 - d));
			width += (size.width % (1 << (depth - d - 1)) > 0 ? 1 : 0);
			height = (size.height >> (depth - 1 - d)) + (size.height % (1 << (depth - d - 1)) > 0 ? 1 : 0);
		}

		return out;
	}

	private static int absolute(int data) {
		if (data < 0) {
			if (data % 2 == 0) {
				return -data + 1;
			} else {
				return -data;
			}
		} else {
			if (data % 2 == 1) {
				return data - 1;
			} else {
				return data;
			}
		}
	}

	private static int relative(int data) {
		if (data % 2 == 0) {
			return data;
		} else {
			return -data;
		}

	}
}
