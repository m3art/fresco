/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.segmentation;

import fresco.CImageContainer;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import support.CSupportWorker;

/**
 * Worker creates new bitmap according to segment map and input image. Both informations
 * should be contained in input ImageContainer. As an output averaged segments are generated.
 *
 * @author gimli
 */
public class CMeanBySegmentWorker extends CSupportWorker<BufferedImage, String> {

	/** Input data - segment map and input image */
	CImageContainer container;
	private static final Logger logger = Logger.getLogger(CMeanBySegmentWorker.class.getName());

	/**
	 * @param cont must contain initialized SegmentMap and input image
	 */
	public CMeanBySegmentWorker(CImageContainer cont) {
		container = cont;
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		CSegmentMap map = container.getSegmentMap();
		BufferedImage original = container.getImage();

		if (map == null || original == null
				|| map.getWidth() != original.getWidth()
				|| map.getHeight() != original.getHeight()) {
			throw new IOException("Bad initialization of worker");
		}

		BufferedImage out = new BufferedImage(original.getWidth(),
				original.getHeight(),
				original.getType()); // hold the same type
		WritableRaster raster = out.getRaster();
		CSegment[] list = null;
		int[][] mapa = null;
		int i, j;
		int maxProgress = out.getWidth() * out.getHeight();

		list = new CSegment[map.getNumSegments()];
		mapa = map.getSegmentMask();
		map.getSegments().toArray(list);

		for (i = 0; i < out.getWidth(); i++) {
			for (j = 0; j < out.getHeight(); j++) {
				raster.setPixel(i, j, list[mapa[i][j]].getColor());
				setProgress((i * out.getHeight() + j) * 100 / maxProgress);
			}
		}
		out.setData(raster);
		return out;
	}

	@Override
	public void done() {
		try {
			container.setMeaned(get());
		} catch (InterruptedException ex) {
			logger.log(Level.FINE, "Conversion interrupted: ", ex);
		} catch (ExecutionException ex) {
			logger.log(Level.INFO, "Exec problem: ", ex.toString());
		}
	}

	@Override
	public String getWorkerName() {
		return "Mean by Segments Worker";
	}
}
