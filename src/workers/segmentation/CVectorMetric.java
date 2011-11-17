/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

/**
 * @author Honza Blazek
 */
import utils.vector.CBasic;
import java.util.*;

public class CVectorMetric implements Comparator<CColorMean> {

	double[] ref;

	public CVectorMetric(double[] pixel) {
		ref = pixel;
	}

	public void setRef(double[] pixel) {
		ref = pixel;
	}

	public int compare(CColorMean a, CColorMean b) {
		CColorMean meanA = a;
		CColorMean meanB = b;
		double dA = getEukleid(meanA.color);
		double dB = getEukleid(meanB.color);

		if (dA > dB) {
			return 1;
		} else if (dA == dB) {
			return 0;
		} else {
			return -1;
		}
	}

	private double getEukleid(double[] pixel) {
		return CBasic.norm(CBasic.diff(ref, pixel));
	}

	private double getRadius(double[] pixel) {
		return Math.abs(CBasic.scalar(ref, pixel) / (CBasic.norm(ref) * CBasic.norm(pixel)));
	}

	public static double getRadius(double[] pixel, double[] reference) {
		return Math.abs(CBasic.scalar(reference, pixel) / (CBasic.norm(reference) * CBasic.norm(pixel)));
	}

	public static double getEukleid(double[] pixel, double[] reference) {
		return CBasic.norm(CBasic.diff(reference, pixel));
	}
}
