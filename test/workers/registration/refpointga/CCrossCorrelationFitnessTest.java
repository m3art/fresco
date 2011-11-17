/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration.refpointga;

import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CCrossCorrelationFitnessTest {

	public CCrossCorrelationFitnessTest() {
	}

	/**
	 * Test of getFitness method, of class CCrossCorrelationFitness.
	 */
	@Test
	public void testGetFitness() {
		System.out.println("getFitness");
		BufferedImage src = new BufferedImage(3, 3, BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster srcRaster = src.getRaster();
		BufferedImage ref = new BufferedImage(3, 3, BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster refRaster = ref.getRaster();

		double[] refData = {10, 10, 10,
			20, 20, 20,
			30, 30, 30,
			40, 40, 40,
			50, 50, 50,
			60, 60, 60,
			70, 70, 70,
			80, 80, 80,
			90, 90, 90};

		double[] srcData = {90, 90, 90,
			80, 80, 80,
			70, 70, 70,
			60, 60, 60,
			50, 50, 50,
			40, 40, 40,
			30, 30, 30,
			20, 20, 20,
			10, 10, 10};

		refRaster.setPixels(0, 0, 3, 3, refData);
		srcRaster.setPixels(0, 0, 3, 3, srcData);

		CCrossCorrelationFitness instance = new CCrossCorrelationFitness(src, ref, 1);
		CPointAndTransformation individual = new CPointAndTransformation(instance);
		individual.setValues(1, 1, 0, 0, 0, 0.999999);

		double expResult = 1.0;
		double result = individual.getFitness();

		assertEquals(expResult, result, 0.001);
	}
}
