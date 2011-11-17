/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.vector;

/**
 * @author Honza Blazek
 */
public class CBasic {

	/** returns random vector from [0,1]^n */
	public static double[] random(int length, double range) {
		int i;
		double[] output = new double[length];

		for (i = 0; i < length; i++) {
			output[i] = Math.random() * range;
		}
		return output;
	}

	/** returns scalar product of x and y */
	public static double scalar(double[] x, double[] y) {
		int i;
		double out = 0;

		for (i = 0; i < x.length; i++) {
			out += x[i] * y[i];
		}

		return out;
	}

	/** returns scalar product of x and y */
	public static int scalar(int[] x, int[] y) {
		int i;
		int out = 0;

		for (i = 0; i < x.length; i++) {
			out += x[i] * y[i];
		}

		return out;
	}

	/** resize vector x aboout c koef. */
	public static double[] scalar(double c, double[] x) {
		int i;
		double[] out = new double[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] * c;
		}

		return out;
	}

	/** resize vector x aboout c koef. */
	public static int[] scalar(double c, int[] x) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = (int) (x[i] * c);
		}

		return out;
	}

	public static int[] divide(int c, int[] x) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] / c;
		}

		return out;
	}

	public static long[] divide(long c, long[] x) {
		int i;
		long[] out = new long[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] / c;
		}

		return out;
	}

	/** returns vector product */
	public static double[] vector(double[] x, double[] y) {
		int i;
		double[] out = new double[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] * y[i];
		}

		return out;
	}

	/** returns vector product */
	public static int[] vector(int[] x, int[] y) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] * y[i];
		}

		return out;
	}

	/** returns vector product */
	public static long[] vector(long[] x, long[] y) {
		int i;
		long[] out = new long[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] * y[i];
		}

		return out;
	}

	/** returns matrix product */
	public static double[] matrix(double[] x, double[] y) {
		double[] out = new double[x.length * y.length];
		int i, j;

		for (i = 0; i < x.length; i++) {
			for (j = 0; j < y.length; j++) {
				out[i + j * x.length] = x[i] * y[j];
			}
		}
		return out;
	}

	/** returns length of vector x */
	public static double norm(double[] x) {
		int i;
		double out = 0;

		for (i = 0; i < x.length; i++) {
			out += x[i] * x[i];
		}
		return out;
	}

	/** returns length of vector x */
	public static int norm(int[] x) {
		int i;
		int out = 0;

		for (i = 0; i < x.length; i++) {
			out += x[i] * x[i];
		}
		return out;
	}

	public static double[] normalize(double[] x) {
		double size = norm(x);
		double[] out = new double[x.length];
		int i;

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] / Math.sqrt(size);
		}
		return out;
	}

	/** returns x and y difference */
	public static double[] diff(double[] x, double[] y) {
		int i;
		double[] out = new double[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] - y[i];
		}

		return out;
	}

	/** returns x and y difference */
	public static int[] diff(int[] x, int[] y) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] - y[i];
		}

		return out;
	}

	/** returns x and y difference */
	public static long[] diff(long[] x, long[] y) {
		int i;
		long[] out = new long[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] - y[i];
		}

		return out;
	}

	public static int[] diffAbs(int[] x, int[] y) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = Math.abs(x[i] - y[i]);
		}

		return out;
	}

	/** returns each element of x reduced by k */
	public static double[] diff(double[] x, double k) {
		int i;
		double[] out = new double[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] - k;
		}

		return out;
	}

	/** returns each element of x reduced by k */
	public static long[] diff(long[] x, long k) {
		int i;
		long[] out = new long[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] - k;
		}

		return out;
	}

	/** returns sum of x and y */
	public static double[] sum(double[] x, double[] y) {
		int i;
		double[] out = new double[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] + y[i];
		}

		return out;
	}

	public static int[] sum(int[] x, int[] y) {
		int i;
		int[] out = new int[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] + y[i];
		}

		return out;
	}

	public static long[] sum(long[] x, int[] y) {
		int i;
		long[] out = new long[x.length];

		for (i = 0; i < x.length; i++) {
			out[i] = x[i] + y[i];
		}

		return out;
	}

	public static int[] zero(int length) {
		int i;
		int[] out = new int[length];

		for (i = 0; i < length; i++) {
			out[i] = 0;
		}

		return out;
	}

	public static void print(double[] vector) {
		int i;
		System.out.print("v = [");
		for (i = 0; i < vector.length - 1; i++) {
			System.out.print(vector[i] + ", ");
		}
		System.out.println(vector[i] + "]");
	}

	public static double max(double[] colors) {
		int i;
		double d = Double.MIN_VALUE;
		for (i = 0; i < colors.length; i++) {
			if (d < colors[i]) {
				d = colors[i];
			}
		}
		return d;
	}

	public static int max(int[] colors) {
		int i;
		int d = Integer.MIN_VALUE;
		for (i = 0; i < colors.length; i++) {
			if (d < colors[i]) {
				d = colors[i];
			}
		}
		return d;
	}

	public static double min(double[] colors) {
		int i;
		double d = Double.MAX_VALUE;
		for (i = 0; i < colors.length; i++) {
			if (d > colors[i]) {
				d = colors[i];
			}
		}
		return d;
	}

	public static int min(int[] colors) {
		int i;
		int d = Integer.MAX_VALUE;
		for (i = 0; i < colors.length; i++) {
			if (d > colors[i]) {
				d = colors[i];
			}
		}
		return d;
	}
	/* returns covariance between x and y sets */

	public static double covariance(double[] x, double[] y) {
		return mean(vector(diff(x, mean(x)), diff(y, mean(y))));
	}

	/** returns mean value form the elements of x */
	public static double mean(double[] x) {
		int i;
		double sum = 0;
		for (i = 0; i < x.length; i++) {
			sum += x[i];
		}
		return sum / x.length;
	}

	/**
	 * Product of this function are merged arrays A and B with omitting removeA
	 * and removeB. This merge is unique therefore each element is in output
	 * only once
	 * @param arrayA first array
	 * @param arrayB second array
	 * @param removeA element which will be omitted in array B (especialy id of arrayA)
	 * @param removeB element which will be omitted in array A
	 * @return merged arrays
	 */
	public static int[] mergeTwoArraysUnique(int[] arrayA, int[] arrayB, int removeA, int removeB) {
		if (arrayA != null && arrayB != null) {
			int[] pom = new int[arrayA.length + arrayB.length];
			int k = 0, l = 0, i = 0;
			while (k < arrayA.length && l < arrayB.length) {
				if (arrayA[k] == removeB) {
					k++; // preskoc oznaceni souseda
					continue;
				}
				if (arrayB[l] == removeA) {
					l++; // preskoc oznaceni souseda
					continue;
				}
				if (arrayA[k] == arrayB[l]) { //
					pom[i] = arrayA[k];
					k++;
					l++;
					i++;
				} else if (arrayA[k] < arrayB[l]) {
					pom[i] = arrayA[k];
					k++;
					i++;
				} else {
					pom[i] = arrayB[l];
					l++;
					i++;
				}
			}
			while (k < arrayA.length) {
				if (arrayA[k] == removeB) {
					k++; // preskoc oznaceni souseda
					continue;
				}
				pom[i] = arrayA[k];
				k++;
				i++;
			}
			while (l < arrayB.length) {
				if (arrayB[l] == removeA) {
					l++; // preskoc oznaceni souseda
					continue;
				}
				pom[i] = arrayB[l];
				l++;
				i++;
			}
			int[] neighs = new int[i];
			System.arraycopy(pom, 0, neighs, 0, i);
			return neighs;
		}
		return null;
	}
}
