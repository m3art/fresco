/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import utils.vector.CBasic;

/**
 * @author HonzaBlazek
 */
public class CColorMean {

	double[] color;
	int weight;

	public CColorMean(int COLOR_SPACE) {
		color = new double[COLOR_SPACE];
		// initialize
		color = CBasic.random(COLOR_SPACE, 255);
		weight = 1;
	}

	public int[] getColor() {
		int[] out = new int[color.length];
		int i;
		for (i = 0; i < out.length; i++) {
			out[i] = (int) color[i];
		}

		return out;
	}
}
