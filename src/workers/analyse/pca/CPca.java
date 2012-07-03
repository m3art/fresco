/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse.pca;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.vector.CBasic;

/**
 * Compute Principal Component Analysis from input set of vectors. Vectors are
 * stored in matrix where each column represents one vector.
 *
 * @author gimli
 */
public class CPca {

	/** Eigenvectors of covariance matrix of input vector array */
	private double[][] eigenVectors;
	/** Flag that eigenvectors and eigenvalues are prepared */
	private boolean dataProcessed = false;
	/** Vectors for PCA */
	private double[][] data;
	/** Number of dimensions of vectors */
	private int dimensions;

	private final static Logger logger = Logger.getLogger(CPca.class.getName());

	/**
	 * Time consuming method computes eigenvectors of covariance matrix of
	 * input image. Vectors are ordered according their eigenvalues and can be
	 * accessed {@see getEigenVectors} and {@see getEigenValues}
	 */
	public double[][] pcaTransform(double[][] vectors) {
		if (vectors.length == 0) {
			throw new IllegalArgumentException("Vectors to process cannot be empty field");
		}
		data = vectors;
		dimensions = vectors[0].length;
		// covariance matrix
		double[][] cov = computeCovarianceMatrix();
		// eigenvectors of covariance matrix
		eigenVectors = computeEigenvectors(cov);
		// conversion of input vectors into new coordinate system defined by eigenvectors
		convertData();

		dataProcessed = true;
		return data;
	}

	private void convertData() {
		for(int i=0; i<data.length; i++) {
			double[] vector = data[i];
			for(int k=0; k< dimensions; k++) {
				data[i][k] = CBasic.scalar(eigenVectors[k],vector);
			}
		}
	}

	private static double[][] computeEigenvectors(double[][] covarianceMatrix) {
		Matrix covariance = new Matrix(covarianceMatrix);
		EigenvalueDecomposition decomposition = new EigenvalueDecomposition(covariance);

		double[] eigenImaginar = decomposition.getImagEigenvalues();
		double[] eigenReal = decomposition.getRealEigenvalues();
		LinkedList<Integer> order = new LinkedList<Integer>();

		int eigenValuesReal = 0;

		for(int i=0; i<eigenImaginar.length; i++) {
			boolean added = false;
			if (eigenImaginar[i] == 0) {
				eigenValuesReal++;
				for(int index: order){
					if (eigenReal[index] < eigenReal[i]) {
						order.add(index, i);
						added = true;
						break;
					}
				}
				if (!added) {
					order.add(i);
				}
			} else {
				logger.log(Level.WARNING, "Complex eigenvalue: {0}", i);
			}
		}

		double[][] eigenVector = decomposition.getV().getArray();
		double[][] out = new double[eigenValuesReal][];

		int i=0;
		for(int index: order){
			out[i] = eigenVector[index];
		}

		return out;
	}

	/**
	 * Vector must be processed before eigenvectors can be returned
	 * @return eigenvectors of input data
	 */
	public double[][] getEigenVectors() {
		if (! dataProcessed) {
			throw new IllegalStateException("Data are not process, call processData before");
		}
		return eigenVectors;
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
