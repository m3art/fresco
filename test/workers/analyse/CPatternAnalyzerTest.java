/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.analyse;

import java.awt.image.BufferedImage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author gimli
 */
public class CPatternAnalyzerTest {

	BufferedImage imageA, imageB;
	CPatternAnalyzer instance;

	public CPatternAnalyzerTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
		imageA = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);
		imageB = new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR);

		int rgb;

		for (int x = 0; x < imageA.getWidth(); x++) {
			for (int y = 0; y < imageA.getHeight(); y++) {
				rgb = ((((imageA.getHeight() * x + y) << 8) | (imageA.getHeight() * x + y)) << 8) | (imageA.getHeight() * x + y);
				imageA.setRGB(x, y, rgb);
				imageB.setRGB(y, x, rgb);
			}
		}

		instance = new CPatternAnalyzer(imageA, imageB, 5, 3, 10);
		instance.init();
	}

	@Test
	public void testInit() {
		System.out.println("init");
		boolean expResult = true;
		boolean result = instance.init();
		assertEquals(expResult, result);
	}

	@Test
	public void testConvertMeshToImage() {
		System.out.println("convertMeshToImage");
		double[][] mesh = new double[16][16];
		double min = 0.0;
		double max = 1.0;

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				mesh[x][y] = (double) (x * 16 + y) / 256;
			}
		}

		BufferedImage expResult = imageA;
		BufferedImage result = instance.convertMeshToImage(mesh, min, max);
		assertEquals(expResult.getWidth(), result.getWidth());
		assertEquals(expResult.getHeight(), result.getHeight());
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				assertEquals(expResult.getRGB(x, y), result.getRGB(x, y));
			}
		}
	}

	@Test
	public void testConvertImageToMesh() {
		System.out.println("convertImageToMesh");
		BufferedImage input = imageA;
		double[][] expResult = new double[imageA.getWidth()][imageA.getHeight()];

		for (int x = 0; x < imageA.getWidth(); x++) {
			for (int y = 0; y < imageA.getHeight(); y++) {
				expResult[x][y] = (double) (x * imageA.getHeight() + y) / 256;
			}
		}

		double[][] result = CPatternAnalyzer.convertImageToMesh(input);
		assertArrayEquals(expResult, result);
	}

	@Test
	public void testSetSampleAtIntoPattern() {
		System.out.println("setSampleAtIntoPattern");
		double[][] mesh = CPatternAnalyzer.convertImageToMesh(imageA);
		int x = 10;
		int y = 10;
		double[] expPattern = {170.0 / 256, 171.0 / 256, 172.0 / 256, 186.0 / 256, 187.0 / 256, 188.0 / 256, 202.0 / 256, 203.0 / 256, 204.0 / 256};

		instance.init();
		instance.setSampleAtIntoPattern(mesh, x, y);
		for (int i = 0; i < expPattern.length; i++) {
			assertEquals(instance.pattern[i], expPattern[i], 0.0000001);
		}
	}

	@Test
	public void testSelectBestSample() {
		System.out.println("selectBestSample");

		instance.init();
		double[][] mesh = CPatternAnalyzer.convertImageToMesh(imageA);
		instance.setSampleAtIntoPattern(mesh, 10, 10);

		instance.alpha = 1.0;
		instance.sample[0].learn(instance.pattern);

		instance.setSampleAtIntoPattern(mesh, 0, 0);
		instance.sample[1].learn(instance.pattern);
		assertEquals(0, instance.sample[1].getValue(instance.pattern), 0.0000001);

		instance.setSampleAtIntoPattern(mesh, 1, 1);
		instance.sample[2].learn(instance.pattern);
		assertEquals(0, instance.sample[2].getValue(instance.pattern), 0.0000001);

		instance.setSampleAtIntoPattern(mesh, 2, 2);
		instance.sample[3].learn(instance.pattern);
		assertEquals(0, instance.sample[3].getValue(instance.pattern), 0.0000001);

		instance.setSampleAtIntoPattern(mesh, 3, 3);
		instance.sample[4].learn(instance.pattern);
		assertEquals(0, instance.sample[4].getValue(instance.pattern), 0.0000001);

		instance.setSampleAtIntoPattern(mesh, 10, 10);
		double[] pattern = instance.pattern;

		int expResult = 0;
		int result = instance.selectBestSample(pattern);
		assertEquals(expResult, result);
	}
}