/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse.pca;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Compute Principal Component Analysis from input set of vectors. Vectors are
 * stored in matrix where each column represents one vector.
 *
 * @author gimli
 */
class CPca {

	/** Eigenvalues of covariance matrix of input vector array */
	private double[] eigenValues;
	/** Eigenvectors of covariance matrix of input vector array */
	private double[][] eigenVectors;
	/** Flag that eigenvectors and eigenvalues are prepared */
	private boolean dataProcessed = false;
	/** Vectors for PCA */
	private final double[][] data;
	/** Number of dimensions of vectors */
	private final int dimensions;

	/**
	 * Constructor
	 * @param vectors input data each column of input matrix represents vector
	 */
	public CPca(double[][] vectors) {
		data = vectors;
		if (vectors.length == 0) {
			throw new IllegalArgumentException("Vectors to process cannot be empty field");
		}
		dimensions = vectors[0].length;
	}

	/**
	 * Time consuming method computes eigenvectors of covariance matrix of
	 * input image. Vectors are ordered according their eigenvalues and can be
	 * accessed {@see getEigenVectors} and {@see getEigenValues}
	 */
	public void processData() {

		double[][] cov = computeCovarianceMatrix();

		Matrix covariance = new Matrix(cov);
		EigenvalueDecomposition decomposition = new EigenvalueDecomposition(covariance);

		eigenValues = decomposition.getRealEigenvalues();
		eigenVectors = decomposition.getV().getArray();

		dataProcessed = true;
	}

	/**
	 * Vector must be processed before eigenvectors can be returned
	 * @return eigenvectors of input data
	 */
	public double[][] getEigenVectors() {
		return eigenVectors;
	}

	public double[] getEigenValues() {
		return eigenValues;
	}

	private double[][] computeCovarianceMatrix() {
		double[] mean = new double[dimensions];
		double[][] cov = new double[dimensions][dimensions];

		// compute mean in each dimension O(m*n)
		for(int i=0; i<data.length; i++) {
			for(int d=0; d<dimensions; d++) {
				mean[d] += data[i][d];
			}
		}
		for(int d=0; d<dimensions; d++) {
			mean[d] /= data.length;
		}

		// compute covariance matrix O(m*n)
		for(int i=0; i<data.length; i++) {
			for(int d1=0; d1<dimensions; d1++) {
				for(int d2=0; d2<dimensions; d2++) {
					cov[d1][d2] += (data[i][d1]-mean[d1])*(data[i][d2]-mean[d2]);
				}
			}
		}
		for (int d1 = 0; d1 < dimensions; d1++) {
			for (int d2 = 0; d2 < dimensions; d2++) {
				cov[d1][d2] /= data.length - 1;
			}
		}

		return cov;
	}



}
