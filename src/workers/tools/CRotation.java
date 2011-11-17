/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.tools;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 *
 * @author gimli
 */
public class CRotation extends CEditWorker {

	public static enum Rotation {

		left, right, mirror, verticalFlip
	};
	BufferedImage image;
	Rotation direction;

	public CRotation(BufferedImage image, Rotation dir) {
		this.image = image;
		direction = dir;
	}

	@Override
	public String getWorkerName() {
		return "Image rotator - " + direction.name() + " rotation";
	}

	private void rotateLeft(Raster input, WritableRaster raster, int width, int height) {
		int x, y;
		int[] pixel = new int[input.getNumBands()];

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				input.getPixel(x, y, pixel);
				raster.setPixel(y, width - 1 - x, pixel);
			}
		}
	}

	private void rotateRight(Raster input, WritableRaster raster, int width, int height) {
		int x, y;
		int[] pixel = new int[input.getNumBands()];

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				input.getPixel(x, y, pixel);
				raster.setPixel(height - 1 - y, x, pixel);
			}
		}
	}

	private void mirror(Raster input, WritableRaster raster, int width, int height) {
		int x, y;
		int[] pixel = new int[input.getNumBands()];

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				input.getPixel(x, y, pixel);
				raster.setPixel(width - 1 - x, y, pixel);
			}
		}
	}

	private void verticalFlip(Raster input, WritableRaster raster, int width, int height) {
		int x, y;
		int[] pixel = new int[input.getNumBands()];

		for (x = 0; x < width; x++) {
			for (y = 0; y < height; y++) {
				input.getPixel(x, y, pixel);
				raster.setPixel(x, height - 1 - y, pixel);
			}
		}
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		int width = image.getWidth(), height = image.getHeight();

		BufferedImage out = new BufferedImage(height, width, image.getType());
		WritableRaster raster = out.getRaster();
		Raster input = image.getData();

		switch (direction) {
			case left:
				rotateLeft(input, raster, width, height);
				break;
			case right:
				rotateRight(input, raster, width, height);
				break;
			case mirror:
				mirror(input, raster, width, height);
				break;
			case verticalFlip:
				verticalFlip(input, raster, width, height);
				break;
		}

		out.setData(raster);
		return out;
	}
}
