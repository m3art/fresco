/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package image.converters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class Crgb2LabTest {
	
	public Crgb2LabTest() {
	}

	/**
	 * Test of sRgb2Xyz method, of class Crgb2Lab.
	 */
	@Test
	public void testSRgb2Xyz2Rgb() {
		System.out.println("sRgb2Xyz2Rgb");
		double[] sRgb = {235.0, 112.2, 56.3};		
		double[] result = Crgb2Lab.xyz2sRgb(Crgb2Lab.sRgb2Xyz(sRgb));
		assertArrayEquals(sRgb, result, 0.01);		
	}
}