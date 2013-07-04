/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.geom.Point2D;
import Jama.*;
import java.util.LinkedList;

/**
 * @author gimli
 * @version Oct 16, 2011
 */
public class CPerspectiveTransformation implements ITransformation2D {

	private static final int NUMBER_OF_PARAMS = 9;
	// transformation matrix
	public double[] a = new double[NUMBER_OF_PARAMS];

	/**
	 * Change transformation matrix basically delta and lambda parameters
	 * @param delta difference parameter
	 * @param lambda learning parameter
	 */
	public void update(CPerspectiveTransformation delta, double lambda) {
		int i;

		for (i = 0; i < NUMBER_OF_PARAMS; i++) {
			a[i] += lambda * Math.signum(delta.a[i]) * Math.max(Math.abs(delta.a[i]), 0.01);
		}
	}

	public void copy(CPerspectiveTransformation value) {
		int j;
		for (j = 0; j < NUMBER_OF_PARAMS; j++) {
			a[j] = value.a[j];
		}
	}

	/**
	 * Defines transformation from 4 pair of corresponding ponts
	 */
        
        public void set(Point2D.Double[] ptsRef, Point2D.Double[] ptsSensed){
          if (ptsRef.length < 4 || ptsSensed.length < 4 || ptsRef.length != ptsSensed.length) return;
          int pts = ptsRef.length;
          double[][] matrix = new double[2*pts][8];
          double[] b = new double [2*pts];;
          for (int i = 0; i < pts; i++) {
            matrix[2*i][0] = ptsRef[i].x;
            matrix[2*i][1] = ptsRef[i].y;
            matrix[2*i][2] = 1;
            matrix[2*i][3] = 0;
            matrix[2*i][4] = 0;
            matrix[2*i][5] = 0;
            matrix[2*i][6] = ptsRef[i].x * ptsSensed[i].x;
            matrix[2*i][7] = ptsRef[i].y * ptsSensed[i].x;
            matrix[2*i+1][0] = 0;
            matrix[2*i+1][1] = 0;
            matrix[2*i+1][2] = 0;
            matrix[2*i+1][3] = ptsRef[i].x;
            matrix[2*i+1][4] = ptsRef[i].y;;
            matrix[2*i+1][5] = 1;
            matrix[2*i+1][6] = ptsRef[i].x * ptsSensed[i].y;
            matrix[2*i+1][7] = ptsRef[i].y * ptsSensed[i].y;
            
            b[2*i] = ptsSensed[i].x;
            b[2*i+1] = ptsSensed[i].y;
          }
          Matrix A = new Matrix(matrix);
          Matrix B = new Matrix(b, 1);
          Matrix AT = A.transpose();
          Matrix C = AT.times(A);
          Matrix D = C.inverse();
          Matrix E = D.times(AT);
          Matrix X = E.times(B.transpose());
          for (int j = 0; j < NUMBER_OF_PARAMS - 1; j++) {
	      a[j] = X.get(j, 0);
            }
          a[NUMBER_OF_PARAMS - 1] = 1;
          
        }
        
        public void set(LinkedList<Point2D.Double> ptsRef, LinkedList<Point2D.Double> ptsSensed) {
          if (ptsRef.size() != ptsSensed.size()) return;
          Point2D.Double[] pr = new Point2D.Double[ptsRef.size()];
          Point2D.Double[] ps = new Point2D.Double[ptsSensed.size()];
          for (int i = 0; i < ptsRef.size(); i++) {
            pr[i] = ptsRef.get(i);
            ps[i] = ptsSensed.get(i);
          }
            this.set(pr,ps);
        };
        
	public void set(Point2D.Double x1, Point2D.Double t1, Point2D.Double x2, Point2D.Double t2,
			Point2D.Double x3, Point2D.Double t3, Point2D.Double x4, Point2D.Double t4) {
		int i, j;
                
		double[][] matrix = {{x1.x, x1.y, 1, 0, 0, 0, -x1.x * t1.x, -x1.y * t1.x, 1, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, x1.x, x1.y, 1, -x1.x * t1.y, -x1.y * t1.y, 0, 1, 0, 0, 0, 0, 0, 0},
			{x2.x, x2.y, 1, 0, 0, 0, -x2.x * t2.x, -x2.y * t2.x, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, x2.x, x2.y, 1, -x2.x * t2.y, -x2.y * t2.y, 0, 0, 0, 1, 0, 0, 0, 0},
			{x3.x, x3.y, 1, 0, 0, 0, -x3.x * t3.x, -x3.y * t3.x, 0, 0, 0, 0, 1, 0, 0, 0},
			{0, 0, 0, x3.x, x3.y, 1, -x3.x * t3.y, -x3.y * t3.y, 0, 0, 0, 0, 0, 1, 0, 0},
			{x4.x, x4.y, 1, 0, 0, 0, -x4.x * t4.x, -x4.y * t4.x, 0, 0, 0, 0, 0, 0, 1, 0},
			{0, 0, 0, x4.x, x4.y, 1, -x4.x * t4.y, -x4.y * t4.y, 0, 0, 0, 0, 0, 0, 0, 1}};
                Matrix A = new Matrix(matrix);
                Matrix B = A.getMatrix(0, A.getRowDimension()-1, 0, 7);
                System.out.println("size of B:" + B.getRowDimension() + " rows, " + B.getColumnDimension() + " cols." );
		double[] t = {t1.x, t1.y, t2.x, t2.y, t3.x, t3.y, t4.x, t4.y};
                Matrix T = new Matrix(t, 1);
                Matrix BT = B.transpose();
                Matrix C = BT.times(B);
                Matrix D = C.inverse();
                Matrix E = D.times(BT);
                Matrix X = E.times(T.transpose());
                System.out.println("size of X:" + X.getRowDimension() + " rows, " + X.getColumnDimension() + " cols." );
		double b[] = new double[8];
                //GEM(matrix);
		for (i = 0; i < NUMBER_OF_PARAMS - 1; i++) {
			/*for (j = NUMBER_OF_PARAMS - 1; j < 2 * NUMBER_OF_PARAMS - 2; j++) {
				a[i] += matrix[i][j] * t[j - NUMBER_OF_PARAMS + 1];
                                
                                
			}*/
                        a[i] = X.get(i, 0);
                        //System.out.println(i + ".a: " + a[i]);
                        //System.out.println(i + ".b: " + b[i]);
		}
		a[NUMBER_OF_PARAMS - 1] = 1;
                
	}

	/**
	 * Defines transformation from 4 pair of corresponding points
	 */
	public void setParameters(Point2D.Double x1, Point2D.Double t1, Point2D.Double x2, Point2D.Double t2, Point2D.Double x3, Point2D.Double t3, Point2D.Double x4, Point2D.Double t4) {
		set(new Point2D.Double(x1.x, x1.y), new Point2D.Double(t1.x, t1.y),
				new Point2D.Double(x2.x, x2.y), new Point2D.Double(t2.x, t2.y),
				new Point2D.Double(x3.x, x3.y), new Point2D.Double(t3.x, t3.y),
				new Point2D.Double(x4.x, x4.y), new Point2D.Double(t4.x, t4.y));
	}

	private double Tx(double x, double y) {
		return (a[0] * x + a[1] * y + a[2]) / (a[6] * x + a[7] * y + a[8]);
	}

	private double Ty(double x, double y) {
		return (a[3] * x + a[4] * y + a[5]) / (a[6] * x + a[7] * y + a[8]);
	}

	public Point2D.Double getProjected(double x, double y) {
		return new Point2D.Double(Tx(x, y), Ty(x, y));
	}

	public Point2D.Double getProjected(Point2D.Double point) {
		return new Point2D.Double(Tx(point.x, point.y), Ty(point.x, point.y));
	}

	public Point2D.Double getInversion(Point2D.Double point) {
		double u, v;

		u = -((-a[2] * a[4] + a[1] * a[5]
				+ a[4] * point.x - a[5] * a[7] * point.x
				- a[1] * point.y + a[2] * a[7] * point.y)
				/ (a[1] * a[3] - a[0] * a[4]
				+ a[4] * a[6] * point.x - a[3] * a[7] * point.x
				- a[1] * a[6] * point.y + a[0] * a[7] * point.y));
		v = -((-a[2] * a[3] + a[0] * a[5]
				+ a[3] * point.x - a[5] * a[6] * point.x
				- a[0] * point.y + a[2] * a[6] * point.y)
				/ (-a[1] * a[3] + a[0] * a[4]
				- a[4] * a[6] * point.x + a[3] * a[7] * point.x
				+ a[1] * a[6] * point.y - a[0] * a[7] * point.y));

		return new Point2D.Double(u, v);
	}

	public void randomize() {
		int i;

		setIdentity();

		for (i = 0; i < NUMBER_OF_PARAMS - 1; i++) {
			a[i] += (Math.random() - 0.5) * 0.1;
		}
		a[NUMBER_OF_PARAMS - 1] = 1;
		a[5] = 20;
	}

	public void setIdentity() {
		a[0] = a[4] = a[8] = 1;
		a[1] = a[2] = a[3] = a[5] = a[6] = a[7] = 0;
	}

	public void print() {
		int i;

		System.out.print("Parametry transformace: ");
		for (i = 0; i < NUMBER_OF_PARAMS; i++) {
			System.out.print(a[i] + " ");
		}
		System.out.println();
	}

	private double[][] GEM(double[][] input) {
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
			pom = input[k - 1][k - 1];
			for (m = 0; m < input[0].length; m++) {
				input[k - 1][m] /= pom;
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
}
