/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.statiscics;

import java.util.ArrayList;

/**
 * @author gimli
 * @version May 1, 2012
 */
public class CHistogramND {

	public final static double EPSILON = 0.001;
	/** Number of dimensions */
	public int dimensions;
	/** Number of histogram bins in each dimension*/
	public int[] bins;
	/** Real size of histogram bin in each dimension */
	public double[] binSize;
	/** Minimal and maximal value stored in histogram*/
	public double[] min, max;
	/** histogram bin values */
	public int[] binContent;
	/** histograms for each dimension separately */
	public ArrayList<int[]> histForOneDimension;
	/** Number of values stored in histogram */
	public int values;
	/** Entropy of stored values */
	public double entropy;


	private CHistogramND(int[] binsNo) {
		dimensions = binsNo.length;
		this.bins = binsNo;
		min = new double[dimensions];
		max = new double[dimensions];
		binSize = new double[dimensions];
		histForOneDimension = new ArrayList<int[]>(dimensions);

		int binsCount = 1;
		for(int b=0; b<dimensions; b++) {
			binsCount *= bins[b];
			histForOneDimension.add(b, new int[bins[b]]);
		}

		binContent = new int[binsCount];

		for(int i=0; i<dimensions; i++) {
			min[i] = Double.MAX_VALUE;
			max[i] = Double.MIN_VALUE;
		}

		values = 0;
		entropy = 0;
	}

	/**
	 * For multidimensional histogram are bins stored in one dimensional array.
	 * This method transcript multidimensional coords of bin {@param binNumbers}
	 * according to number of {@param bins} in each dimension and return index
	 * in internal field
	 * @return transforms i.e.: [3,8,5] -> 38
	 */
	public static int getBinNumber(int[] binNumbers, int[] bins) {
		int binNumber = 0;
		for(int i = bins.length - 1; i >= 0; i--) {
			binNumber = binNumber * bins[i] + binNumbers[i];
		}
		return binNumber;
	}

	/**
	 * Creates multidimensional histogram
	 *
	 * @param inputValues contains in each column one dimension of values, number of
	 * rows must correspond with length of bins
	 * @param bins number of histogram bins in each dimension
	 * @return created multidimensional histogram
	 */
	public static CHistogramND createHistogram(double[][] inputValues, int[] bins) {
		CHistogramND out = new CHistogramND(bins);

		for(int i=0; i<out.dimensions; i++) {
			for(double value: inputValues[i]) {
				if (value < out.min[i]) {
					out.min[i] = value;
				}
				if (value > out.max[i]) {
					out.max[i] = value + EPSILON;
				}
			}
			out.binSize[i] = (out.max[i]-out.min[i])/bins[i];
		}

		out.values = inputValues[0].length;
		for(int v=0; v<out.values; v++) {
			int[] histValue = new int[out.dimensions];
			for(int i=0; i<out.dimensions; i++) {
				histValue[i] = (int)((inputValues[i][v] - out.min[i])/out.binSize[i]);
				out.histForOneDimension.get(i)[histValue[i]]++;
			}
			out.binContent[getBinNumber(histValue, out.bins)]++;
		}

		for(int bin: out.binContent) {
			double pBin = ((double)bin)/out.values;
			if (pBin != 0) {
				out.entropy -= pBin * Math.log(pBin) * bin;
			}
		}

		return out;
	}
}
