/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils;

/**
 *
 * @author gimli
 */
public class CMatrix {

	/**
	 * Gaussian elimination method.
	 * @param input input matrix which will be transformed into diagonal matrix
	 * @return diagonal matrix equivalent to input matrix
	 */
	public static double[][] GEM(double[][] input) {
		int i, j, k, l, m;
		double val, pom;

		for (k = 1; k < input.length + 1; k++) {                       // eliminuj postupne od prvniho elementy
			if (input[k - 1][k - 1] == 0) {                       // nevhodny radek, na pozici j-1 je nula
				for (l = k; l < input.length; l++) // najdi radek l, kde na pozici j-1 neni nula
				{
					if (input[l][k - 1] != 0) {                  // radek l se hodi na misto radku j-1
						for (m = 0; m < input[0].length; m++) {    // prohodi radky j-1 a l
							pom = input[l][m];                // ulozi do pomocne promenne prvek
							input[l][m] = input[k - 1][m];      // prepise pozici novym prvkem
							input[k - 1][m] = pom;              // prepise pozici novym prvkem
						}
						break;                              // prerusi cyklus radek zamenen za vhodny
					}
				}
			}
			for (j = 0; j < input.length; j++) {
				if (k - 1 != j && input[j][k - 1] != 0) {                         // eliminuj pouze pokud je treba
					val = input[j][k - 1] / input[k - 1][k - 1];        // koeficient nasobeni radku
					for (i = k - 1; i < input[0].length; i++) // zmensi kazdeho clena na radku
					{
						input[j][i] -= val * input[k - 1][i];         // zmensi ho o ho
					}
				}
			}
		}
		return input;
	}

	/**
	 * Writes matrix into standard output
	 * @param matrix matrix to dump
	 */
	public static void dump(double[][] matrix) {
		for(int i=0; i<matrix.length; i++) {
			for(int j=0; j<matrix[i].length; j++) {
				System.out.print(matrix[i][j]+", ");
			}
			System.out.println();
		}
	}

	public static double[][] reshape(double[][] A, int m, int n) {
        int origM = A.length;
        int origN = A[0].length;
        if(origM*origN != m*n){
            throw new IllegalArgumentException("New matrix must be of same area as matix A");
        }
        double[][] B = new double[m][n];
        double[] A1D = new double[A.length * A[0].length];

        int index = 0;
        for(int i = 0;i<A.length;i++){
            for(int j = 0;j<A[0].length;j++){
                A1D[index++] = A[i][j];
            }
        }

        index = 0;
        for(int i = 0;i<n;i++){
            for(int j = 0;j<m;j++){
                B[j][i] = A1D[index++];
            }

        }
        return B;
    }
}
