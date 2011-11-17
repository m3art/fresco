/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import workers.segmentation.CSegment;
import workers.segmentation.CSegmentMap;
import utils.vector.CBasic;

/**
 * Class for image resizing
 */
public class CZoom {

	/**
	 * Basic scaling of input image
	 */
	public static BufferedImage resizeImage(double focus, BufferedImage original) {
		BufferedImage out = new BufferedImage((int) (focus * original.getWidth()),
				(int) (focus * original.getHeight()),
				original.getType()); // hold the same type
		Graphics2D g2 = (Graphics2D) out.getGraphics();

		g2.drawImage(out, 0, 0, out.getWidth(), out.getHeight(), null);

		return out;
	}

	/**
	 * This function resize the input image by the param focus. The output color is defined
	 * as a average of inherited original pixels in the area for particular output pixel. The
	 * average isn't weighted.
	 * @param focus the size output image towards original
	 * @param original the input image
	 * @return resized image ...
	 * @deprecated use plain resizeImage for meaned image
	 */
	@Deprecated
	public static BufferedImage resizeImage(double focus, BufferedImage original, CSegmentMap map) {
		boolean mapLoaded = (map != null);
		BufferedImage out = new BufferedImage((int) (focus * original.getWidth()),
				(int) (focus * original.getHeight()),
				original.getType()); // hold the same type
		WritableRaster raster = out.getRaster();
		Raster orig = original.getData();
		CSegment[] list = null;
		int[][] mapa = null;
		int i, j, k, x, y, comps;
		int[] sum = new int[raster.getNumBands()], pixel = new int[raster.getNumBands()];

		if (mapLoaded) {
			list = new CSegment[map.getNumSegments()];
			mapa = map.getSegmentMask();
			map.getSegments().toArray(list);
		}
		for (i = 0; i < out.getWidth(); i++) {
			for (j = 0; j < out.getHeight(); j++) {
				for (k = 0; k < sum.length; k++) {
					sum[k] = 0;
				}
				comps = 0;
				for (x = (int) (i / focus); x < ((i + 1) / focus) && x < original.getWidth(); x++) {
					for (y = (int) (j / focus); y < ((j + 1) / focus) && y < original.getHeight(); y++) {
						if (mapLoaded) {
							pixel = list[mapa[x][y]].getColor();
						} //pixel = map.getSegmentByNumber(map.getNumberAt(x, y)).getColor();
						else {
							orig.getPixel(x, y, pixel);
						}
						sum = CBasic.sum(sum, pixel);
						comps++;
					}
				}
				raster.setPixel(i, j, CBasic.scalar(1.0 / comps, sum));
			}
		}
		out.setData(raster);
		return out;
	}
}
