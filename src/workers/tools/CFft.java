/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.tools;

/*
 * Static class for fast fourier transformation and its inverse
 */
public class CFft {

	/**
	 * Compute the FFT of x[], assuming its length is a power of 2
	 * @param x input vector of values in amplitude spectrum
	 * @return vector in wavelength spectrum
	 */
	public static CComplexNumber[] fft(CComplexNumber[] x) {
		int N = x.length;

		// base case
		if (N == 1) {
			return new CComplexNumber[]{x[0]};
		}

		// radix 2 Cooley-Tukey FFT
		if (N % 2 != 0) {
			throw new RuntimeException("N is not a power of 2");
		}

		// fft of even terms
		CComplexNumber[] even = new CComplexNumber[N / 2];
		for (int k = 0; k < N / 2; k++) {
			even[k] = x[2 * k];
		}
		CComplexNumber[] q = fft(even);

		// fft of odd terms
		CComplexNumber[] odd = even;  // reuse the array
		for (int k = 0; k < N / 2; k++) {
			odd[k] = x[2 * k + 1];
		}
		CComplexNumber[] r = fft(odd);

		// combine
		CComplexNumber[] y = new CComplexNumber[N];
		for (int k = 0; k < N / 2; k++) {
			double kth = -2 * k * Math.PI / N;
			CComplexNumber wk = new CComplexNumber(Math.cos(kth), Math.sin(kth));
			CComplexNumber wkrk = CComplexNumber.times(wk, r[k]);
			y[k] = CComplexNumber.plus(q[k], wkrk);
			y[k + N / 2] = CComplexNumber.minus(q[k], wkrk);
		}
		return y;
	}

	/**
	 * TODO: fft(fft(x).').'
	 * @param matrix
	 * @return not yet implemented
	 */
	public static CComplexNumber[][] fft2(CComplexNumber[][] matrix) {
		CComplexNumber[][] out = new CComplexNumber[matrix.length][matrix[0].length];

		return out;
	}

	/**
	 * Method computes the inverse FFT of vector @param x[], assuming its
	 * length is a power of 2. If not some iteration will fail.
	 */
	public static CComplexNumber[] ifft(CComplexNumber[] x) {
		int N = x.length;
		CComplexNumber[] y = new CComplexNumber[N];

		// take conjugate
		for (int i = 0; i < N; i++) {
			y[i] = CComplexNumber.conjugate(x[i]);
		}

		// compute forward FFT
		y = fft(y);

		// take conjugate again
		for (int i = 0; i < N; i++) {
			y[i] = CComplexNumber.conjugate(y[i]);
		}

		// divide by N
		for (int i = 0; i < N; i++) {
			y[i] = CComplexNumber.times(y[i], 1.0 / N);
		}

		return y;
	}

	/**
	 * TODO: ifft(ifft(x).').'
	 * @param matrix
	 * @return not yet implemented
	 */
	public static CComplexNumber[][] ifft2(CComplexNumber[][] matrix) {
		CComplexNumber[][] out = new CComplexNumber[matrix.length][matrix[0].length];

		return out;
	}

	/**
	 *  Method computes the circular radialConvolution of @param x and @param y
	 */
	public static CComplexNumber[] radialConvolution(CComplexNumber[] x, CComplexNumber[] y) {

		// TODO: pad x and y with zeross so that they have same length and are powers of 2
		if (x.length != y.length) {
			throw new IllegalArgumentException("Dimensions of input vectors don't agree");
		}

		int N = x.length;

		// compute FFT of each sequence
		CComplexNumber[] fx = fft(x);
		CComplexNumber[] fy = fft(y);

		// point-wise multiply
		CComplexNumber[] c = new CComplexNumber[N];
		for (int i = 0; i < N; i++) {
			c[i] = CComplexNumber.times(fx[i], fy[i]);
		}

		// compute inverse FFT
		return ifft(c);
	}

	// compute the linear convolution of x and y
	public static CComplexNumber[] convolve(CComplexNumber[] x, CComplexNumber[] y) {
		CComplexNumber ZERO = new CComplexNumber(0, 0);

		CComplexNumber[] a = new CComplexNumber[2 * x.length];
		for (int i = 0; i < x.length; i++) {
			a[i] = x[i];
		}
		for (int i = x.length; i < 2 * x.length; i++) {
			a[i] = ZERO;
		}

		CComplexNumber[] b = new CComplexNumber[2 * y.length];
		for (int i = 0; i < y.length; i++) {
			b[i] = y[i];
		}
		for (int i = y.length; i < 2 * y.length; i++) {
			b[i] = ZERO;
		}

		return radialConvolution(a, b);
	}

	/**
	 * FFT is often enumerated in complex numbers. This class implements basic
	 * operations on complex numbers
	 */
	public static class CComplexNumber {

		/** real an imagin part of complex number */
		double real, imag;

		public CComplexNumber(double real) {
			this.real = real;
			this.imag = 0;
		}

		public CComplexNumber(double real, double imag) {
			this.real = real;
			this.imag = imag;
		}

		/**
		 * Multiplication of compex number @param a and @param b
		 * @return result of multiplication
		 */
		public static CComplexNumber times(CComplexNumber a, CComplexNumber b) {
			return new CComplexNumber(a.real * b.real - a.imag * b.imag, a.imag * b.real + a.real * b.imag);
		}

		/**
		 * Multiplication of complex number @param a and real number @param b
		 * @return result of multiplication
		 */
		public static CComplexNumber times(CComplexNumber a, double b) {
			return new CComplexNumber(a.real * b, a.imag * b);
		}

		/**
		 * Conjugate of complex number @param a is equal to z with inverse
		 * value in imagine part of the number
		 * @return conjugate of complex number
		 */
		public static CComplexNumber conjugate(CComplexNumber a) {
			return new CComplexNumber(a.real, -a.imag);
		}

		/**
		 * @return Addition of two complex numbers @param a and @param b
		 */
		public static CComplexNumber plus(CComplexNumber a, CComplexNumber b) {
			return new CComplexNumber(b.real + a.real, b.imag + a.imag);
		}

		/**
		 * @return Difference of two complex numbers @param a and @param b
		 */
		public static CComplexNumber minus(CComplexNumber a, CComplexNumber b) {
			return new CComplexNumber(a.real - b.real, a.imag - b.imag);
		}
	}
}
