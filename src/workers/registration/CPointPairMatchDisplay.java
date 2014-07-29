/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import fresco.CData;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * Stitches two images together draws CData.CPointPairs onto them draws lines
 * between corresponding points
 *
 * @author Jakub
 */
public class CPointPairMatchDisplay extends CRegistrationWorker {

	protected BufferedImage imageA, imageB;
	protected CPointPairs pairs;

	@Override
	public String getWorkerName() {
		return "Display of point pair matches";
	}

	public CPointPairMatchDisplay(CPointPairs pairs, BufferedImage inputA, BufferedImage inputB) {
		this.pairs = pairs;
		this.imageA = inputA;
		this.imageB = inputB;
	}

	/**
	 * majority of code taken from CImagePanel.paintRegMarks()
	 */
	private void drawLineAndBoxes(Point2D.Double ptA, Point2D.Double ptB, BufferedImage output, int pairIndex) {
		Graphics2D g = output.createGraphics();
		g.setColor(Color.WHITE);
		BasicStroke bs = new BasicStroke(2);
		g.setStroke(bs);
		int cross_size = 4;
		Color[] markFg = {Color.BLACK, Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW},
				markBg = {Color.WHITE, Color.YELLOW, Color.PINK, Color.BLACK, Color.BLUE};
		g.setColor(Color.BLACK);
		g.drawLine((int) ptA.x, (int) ptA.y, (int) ptB.x + imageA.getWidth(), (int) ptB.y);

		Point2D.Double mark = ptA;
		int x = (int) Math.round(mark.x * CData.getFocus() / 100);
		int y = (int) Math.round(mark.y * CData.getFocus() / 100);
		bs = new BasicStroke(1);
		g.setStroke(bs);
		g.setColor(markBg[pairIndex % markBg.length]);
		g.drawLine(x - cross_size - 1, y - cross_size - 1, x + cross_size + 1, y - cross_size - 1);
		g.drawLine(x - cross_size - 1, y - cross_size - 1, x - cross_size - 1, y + cross_size + 1);
		g.drawLine(x + cross_size + 1, y + cross_size + 1, x - cross_size - 1, y + cross_size + 1);
		g.drawLine(x + cross_size + 1, y + cross_size + 1, x + cross_size + 1, y - cross_size - 1);
		g.setColor(markFg[pairIndex % markBg.length]);
		g.drawLine(x + cross_size, y, x - cross_size, y);
		g.drawLine(x, y + cross_size, x, y - cross_size);
		char[] number = ((Integer) pairIndex).toString().toCharArray();
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 3 * cross_size));
		int width = g.getFontMetrics().charsWidth(number, 0, number.length);
		g.fillRect(x + cross_size, y + cross_size, cross_size + width, 3 * cross_size);
		if (g.getColor().equals(Color.black)) {
			g.setColor(Color.white);
		} else {
			g.setColor(Color.black);
		}
		g.drawRect(x + cross_size, y + cross_size, cross_size + width, 3 * cross_size);

		g.drawChars(number, 0, number.length, x + cross_size + cross_size / 2, y + 3 * cross_size + cross_size / 2);

		mark = new Point2D.Double(ptB.x + imageA.getWidth(), ptB.y);
		x = (int) Math.round(mark.x * CData.getFocus() / 100);
		y = (int) Math.round(mark.y * CData.getFocus() / 100);

		g.setColor(markBg[pairIndex % markBg.length]);
		g.drawLine(x - cross_size - 1, y - cross_size - 1, x + cross_size + 1, y - cross_size - 1);
		g.drawLine(x - cross_size - 1, y - cross_size - 1, x - cross_size - 1, y + cross_size + 1);
		g.drawLine(x + cross_size + 1, y + cross_size + 1, x - cross_size - 1, y + cross_size + 1);
		g.drawLine(x + cross_size + 1, y + cross_size + 1, x + cross_size + 1, y - cross_size - 1);
		g.setColor(markFg[pairIndex % markBg.length]);
		g.drawLine(x + cross_size, y, x - cross_size, y);
		g.drawLine(x, y + cross_size, x, y - cross_size);
		number = ((Integer) pairIndex).toString().toCharArray();
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 3 * cross_size));
		width = g.getFontMetrics().charsWidth(number, 0, number.length);
		g.fillRect(x + cross_size, y + cross_size, cross_size + width, 3 * cross_size);
		if (g.getColor().equals(Color.black)) {
			g.setColor(Color.white);
		} else {
			g.setColor(Color.black);
		}
		g.drawRect(x + cross_size, y + cross_size, cross_size + width, 3 * cross_size);

		g.drawChars(number, 0, number.length, x + cross_size + cross_size / 2, y + 3 * cross_size + cross_size / 2);

	}

	@Override
	protected BufferedImage doInBackground() {
		int outW = imageA.getWidth() + imageB.getWidth();
		int outH = imageA.getHeight() > imageB.getHeight() ? imageA.getHeight() : imageB.getHeight();
		int[] pixel = new int[3];
		BufferedImage output = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < imageA.getWidth(); i++) {
			for (int j = 0; j < imageA.getHeight(); j++) {
				pixel = imageA.getRaster().getPixel(i, j, pixel);
				output.getRaster().setPixel(i, j, pixel);
			}
		}
		for (int i = 0; i < imageB.getWidth(); i++) {
			for (int j = 0; j < imageB.getHeight(); j++) {
				pixel = imageB.getRaster().getPixel(i, j, pixel);
				output.getRaster().setPixel(i + imageA.getWidth(), j, pixel);
			}
		}
		// somewhere there should be a setProgress()
		for (int i = 0; i < pairs.size(); i++) {
			Point2D.Double ptA = pairs.getOrigin(i);
			Point2D.Double ptB = pairs.getProjected(i);
			this.drawLineAndBoxes(ptA, ptB, output, i);

		}

		return output;
	}
}
