/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

/**
 * @author Honza Blazek
 */
import workers.analyse.CWaveletTransform;
import java.awt.image.*;

public class CHaarWavelet extends CRegistrationWorker {

	int depth;
	BufferedImage input1, input2;

	public CHaarWavelet(BufferedImage _input1, int _depth, BufferedImage _input2) {
		input1 = _input1;
		input2 = _input2;
		depth = _depth;
	}

	public BufferedImage waveletJoin() {
		int width = input1.getWidth(),
				height = input1.getHeight();
		int diffX = width, diffY = height;

		BufferedImage output = new BufferedImage(width, height, input1.getType());
		WritableRaster out = output.getRaster();
		Raster in1 = input1.getData(), in2 = input2.getData();
		int b, d, i, j, bands = input1.getSampleModel().getNumBands();
		int[][] pixel = new int[2][bands];
		int[] diff = new int[bands];

		for (d = 0; d < depth; d++) {
			diffX = (diffX >> 1) + diffX % 2;
			diffY = (diffY >> 1) + diffY % 2;
		}

		for (i = 0; i < width; i++) {
			for (j = 0; j < height; j++) {
				if (i >= diffX || j >= diffY) {
					in1.getPixel(i, j, pixel[0]);
					in2.getPixel(i, j, pixel[1]);
					for (b = 0; b < bands; b++) {
						diff[b] = Math.max(pixel[0][b], pixel[1][b]);
					}
					out.setPixel(i, j, diff);
				} else {
					in1.getPixel(i, j, pixel[0]);
					out.setPixel(i, j, pixel[0]);
				}
			}
		}
		output.setData(out);
		return output;
	}

	@Override
	protected BufferedImage doInBackground() throws Exception {
		input1 = CWaveletTransform.transform(input1, depth);
		input2 = CWaveletTransform.transform(input2, depth);
		return CWaveletTransform.inverse(waveletJoin(), depth);
	}

	@Override
	public String getWorkerName() {
		return "Wavelet join";
	}
}
