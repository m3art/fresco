
import image.statiscics.CHistogramND;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author gimli
 */
public class CHistogramNDTest {

	@Test
	public void testGetBinNumber() {
		System.out.println("getBinNumber");
		int[] binNumbers = {5, 7, 0};
		int[] bins = {6,8,5};
		int result = CHistogramND.getBinNumber(binNumbers,bins);
		assertEquals(47, result);

		int[] binNumbers2 = {5, 7, 1};
		result = CHistogramND.getBinNumber(binNumbers2,bins);
		assertEquals(95, result);

		int[] binNumbers3 = {5, 7, 2};
		result = CHistogramND.getBinNumber(binNumbers3,bins);
		assertEquals(143, result);
	}

	public void testCreateHistogram() {

		double[][] inputValues = {
			{0, 1, 2, 3, 4, 5}, // will be split into {0,1,2} {3,4,5}
			{0, 1, 2, 3, 4, 5}, // {0,1}, {2,3}, {4,5}
			{-1000, -500, 0, 500, 1000, 1500}}; // ...

		int[] bins = {2, 3, 2};

		CHistogramND instance = CHistogramND.createHistogram(inputValues, bins);

		assertEquals(6, instance.values);
		assertEquals(3, instance.dimensions);
		assertEquals(1.0/3*Math.log(1.0/3)*4+1.0/6*Math.log(1.0/6)*2, instance.entropy, 0.01);
		assertEquals(2, instance.binContent[0]);
		assertEquals(0, instance.binContent[1]);
		assertEquals(1, instance.binContent[2]);
		assertEquals(0, instance.binContent[3]);
		assertEquals(0, instance.binContent[4]);
		assertEquals(0, instance.binContent[5]);
		assertEquals(0, instance.binContent[6]);
		assertEquals(0, instance.binContent[7]);
		assertEquals(0, instance.binContent[8]);
		assertEquals(1, instance.binContent[9]);
		assertEquals(0, instance.binContent[10]);
		assertEquals(2, instance.binContent[11]);
	}
}
