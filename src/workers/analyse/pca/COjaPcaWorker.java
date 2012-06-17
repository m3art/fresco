/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse.pca;

/**
 * @author Honza Blazek
 */
import utils.vector.CBasic;
import java.awt.image.*;
import java.awt.*;
import workers.analyse.CAnalysisWorker;

/** class for Principal Component Analysis of Color Space of the picture
 *  color space is limited to 2D vector (form 3D color space)
 */
public class COjaPcaWorker extends CAnalysisWorker {

	private int bands;
	private double[] mean, deviation; // normalization konstants
	private double[][] vector_base; // PCA vector Base
	Point size; // size of the image (maxX, maxY)
	private int[][][] original;
	private double[][][] working;
	private int dimension;

	/** Do PCA analysis on 3D color to 2D color */
	public COjaPcaWorker(BufferedImage pat, int dim) {
		int i, j;

		Raster raster = pat.getRaster();

		bands = pat.getSampleModel().getNumBands();
		size = new Point(pat.getWidth(), pat.getHeight());
		original = new int[size.x][size.y][bands];
		working = new double[size.x][size.y][bands];
		dimension = dim;

		for (i = 0; i < size.x; i++) {
			for (j = 0; j < size.y; j++) {
				raster.getPixel(i, j, original[i][j]);
			}
		}

		vector_base = new double[dimension][bands];
	}

	/** PCA from dimension to dim
	 * @param pat data k-dimensional
	 * @param dim output dimension vector
	 */
	public COjaPcaWorker(int[][][] pat, int dim) {
		original = pat;
		dimension = dim;

		bands = original[0][0].length;
		size = new Point(original.length, original[0].length);
		working = new double[size.x][size.y][bands];

		vector_base = new double[dimension][bands];
		//System.out.println(bands);
	}

	public COjaPcaWorker(BufferedImage image1, BufferedImage image2, int dim) {
		int i, j, b;
		Raster r1 = image1.getData();
		Raster r2 = image2.getData();

		size = new Point(image1.getWidth(), image1.getHeight());

		int bands1 = image1.getSampleModel().getNumBands();
		int bands2 = image2.getSampleModel().getNumBands();

		bands = bands1 + bands2;
		original = new int[size.x][size.y][bands];

		int[] pixel1 = new int[bands1],
				pixel2 = new int[bands2];

		for (i = 0; i < r1.getWidth(); i++) {
			for (j = 0; j < r1.getHeight(); j++) {
				r1.getPixel(i, j, pixel1);
				r2.getPixel(i, j, pixel2);
				for (b = 0; b < bands1; b++) {
					original[i][j][b] = pixel1[b];
				}
				for (b = 0; b < bands2; b++) {
					original[i][j][bands1 + b] = pixel2[b];
				}
			}
		}
		dimension = dim;

		//size = new Point(original.length,original[0].length);
		working = new double[size.x][size.y][bands];

		vector_base = new double[dimension][bands];
	}

	@Override
	public BufferedImage doInBackground() {
		BufferedImage output = new BufferedImage(size.x, size.y, BufferedImage.TYPE_INT_RGB);
		WritableRaster out = output.getRaster();

		setMeanDeviation(); // global field mean, deviation
		shiftColors(); // data normalization
		setBaseVectors(); // count PCA vector space

		int i, j, b, d;
		double[] origin = new double[bands];
		double[] transformed = new double[bands];
		double err = 0;
		double[] c = new double[bands]; // projection coef.
		double[] min = new double[dimension], max = new double[dimension];

		for (d = 0; d < dimension; d++) {
			min[d] = 255;
			max[d] = -255;
		}

		for (i = 0; i < size.x; i++) {
			for (j = 0; j < size.y; j++) {
				for (b = 0; b < bands; b++) {
					origin[b] = ((double) original[i][j][b] - mean[b]) / deviation[b];
					transformed[b] = 0;
					c[b] = 0;
				}
				for (d = 0; d < dimension; d++) {
					c[d] = getProjectionKoef(CBasic.diff(origin, transformed), vector_base[d]);
					transformed = CBasic.sum(transformed, CBasic.scalar(c[d], vector_base[d]));
					if (min[d] > c[d]) {
						min[d] = c[d];
					}
					if (max[d] < c[d]) {
						max[d] = c[d];
					}
				}
			}
		}
		for (i = 0; i < size.x; i++) {
			for (j = 0; j < size.y; j++) {
				c = new double[bands];
				for (b = 0; b < bands; b++) {
					origin[b] = ((double) original[i][j][b] - mean[b]) / deviation[b];
					transformed[b] = 0;
				}
				for (d = 0; d < dimension; d++) {
					c[d] = getProjectionKoef(CBasic.diff(origin, transformed), vector_base[d]);
					transformed = CBasic.sum(transformed, CBasic.scalar(c[d], vector_base[d]));
				}
				err += CBasic.norm(CBasic.diff(origin, transformed));

				if (bands > 3) {
					for (d = 0; d < dimension; d++) {
						c[d] = Math.max(Math.min(255, (c[d] - min[d]) * 255 / (max[d] - min[d])), 0);
					}
					//c = Crgb2hsv.inverse(c);
					out.setPixel(i, j, c);
				} else {
					// this part use only for color reduction, colors are not fictive but real!
					for (b = 0; b < bands; b++) {
						transformed[b] = Math.min(Math.max(0, transformed[b] * deviation[b] + mean[b]), 255);
					}
					out.setPixel(i, j, transformed);
				}
			}
		}
		System.out.println("Error:" + err / (size.x * size.y));
		output.setData(out);

		return output;
	}

	/** sets mean value of color space and standard deviation */
	private void setMeanDeviation() {
		int i, j, k;

		mean = new double[bands];
		deviation = new double[bands];
		for (k = 0; k < bands; k++) {
			mean[k] = 0;
			deviation[k] = 1;
		}

		for (i = 0; i < size.getX(); i++) {
			for (j = 0; j < size.getY(); j++) {
				for (k = 0; k < bands; k++) {
					mean[k] += original[i][j][k];
					deviation[k] += original[i][j][k] * original[i][j][k];
				}
			}
		}
		for (k = 0; k < bands; k++) {
			mean[k] /= (size.x * size.y);
			deviation[k] = Math.sqrt((deviation[k] / (size.x * size.y)) - mean[k]);
		}
	}

	/** data normalization - mean = [0,0,0], stdDeviation = 1; */
	private void shiftColors() {
		int i, j, k;

		for (i = 0; i < size.getX(); i++) {
			for (j = 0; j < size.getY(); j++) {
				for (k = 0; k < bands; k++) {
					working[i][j][k] = (original[i][j][k] - mean[k]) / deviation[k];
				}
			}
		}
	}

	/** generate bands - 1 PCA vecotrs */
	private void setBaseVectors() {
		int i;

		for (i = 0; i < dimension; i++) {
			// count next vector
			vector_base[i] = getBaseVector(i);
			// rearrange color space
			vectorProjection(vector_base[i]);
			setProgress(i * 100 / dimension);
		}
	}

	/** for given color space generate new PCA vector */
	private double[] getBaseVector(int order) {
		double g = 0.25;
		int iters = size.x * size.y * 5;
		final double max_error_pow = -5;
		double error = 5;

		int j, b, i, k, l;
		double[] w = new double[bands];
		double[] wold = new double[bands];
		double ro; // scalar w*x

		// initialize
		for (i = 0; i < bands; i++) {
			w[i] = 0.5 - Math.random();
		}
		w = CBasic.normalize(w);
		for (i = 0; i < order; i++) {
			w = CBasic.diff(w, CBasic.scalar(CBasic.scalar(vector_base[i], w), vector_base[i]));
		}

		// count first vector
		j = 2;
		while (error > Math.pow(10, max_error_pow)) {
			for (b = 0; b < bands; b++) {
				wold[b] = w[b];
			}
			for (i = 0; i < iters; i++) { // this cycle is not necessary, but have more precise result
				// random choice
				k = (int) Math.round(Math.random() * (size.x - 1));
				l = (int) Math.round(Math.random() * (size.y - 1));

				// new suboptimal w
				ro = CBasic.scalar(working[k][l], w);
				w = CBasic.sum(w, CBasic.scalar(g * ro, CBasic.diff(working[k][l], CBasic.scalar(ro, w))));

				// learning parameter decrease
				g = 1 / Math.pow(j, 0.5); // note: method with sqrt(j) is stable
				j++;
			}
			error = CBasic.norm(CBasic.diff(wold, w));

			//set progress
			if (Math.log10(error) > max_error_pow && Math.log10(error) < 0) {
				setProgress((int) (100 * Math.log10(error) / max_error_pow));
			} else if (Math.log10(error) >= 0) {
				setProgress(1);
			} else {
				setProgress(100);
			}
		}
		w = CBasic.normalize(w);
		return w;
	}

	/** color space projection to space w' */
	private void vectorProjection(double[] w) {
		int i, j;
		double c; // cos(x,w), scalar(x,w), norm(w);

		for (i = 0; i < size.x; i++) {
			for (j = 0; j < size.y; j++) {

				// project koeficient
				c = getProjectionKoef(working[i][j], w);
				// diff vector
				working[i][j] = CBasic.diff(working[i][j], CBasic.scalar(c, w));
			}
		}
	}

	private double getProjectionKoef(double[] a, double w[]) {
		return CBasic.scalar(a, w) / CBasic.norm(w);
	}

	@Override
	public String getWorkerName() {
		return "Principal Component Analysis";
	}
}
