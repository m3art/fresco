/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package workers.registration;

import fresco.CData;
import fresco.swing.CBlendComponent;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gimli
 */
public class CBlendWorker extends CRegistrationWorker {

	BufferedImage inputA, inputB;
	int alpha;
	CBlendComponent blendComponent;
	static final Logger logger = Logger.getLogger(CBlendWorker.class.getName());

	public CBlendWorker(CBlendComponent cBlendPanel, int blendValue) {
		this.alpha = blendValue;
		blendComponent = cBlendPanel;
	}

	public void setAlpha(int value) {
		alpha = value;
	}

	@Override
	public String getWorkerName() {
		return "Alpha blending worker";
	}

	@Override
	protected BufferedImage doInBackground() {
		if (CData.showImage[0] == -1 || CData.showImage[2] == -1) {
			return null;
		} else {
			inputA = CData.getImage(CData.showImage[0]).getTransformedImage(CData.view);
			inputB = CData.getImage(CData.showImage[2]).getTransformedImage(CData.view);
		}

		BufferedImage output = new BufferedImage(Math.max(inputA.getWidth(), inputB.getWidth()),
				Math.max(inputA.getHeight(), inputB.getHeight()), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2 = (Graphics2D) output.getGraphics();
		g2.drawImage(inputA, 0, 0, inputA.getWidth(), inputA.getHeight(), blendComponent);
		Composite old = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 100f));
		g2.drawImage(inputB, 0, 0, inputB.getWidth(), inputB.getHeight(), blendComponent);
		g2.setComposite(old);

		logger.info("Image regenerated.");

		setProgress(100);

		return output;
	}

	@Override
	public void done() {
		try {
			firePropertyChange("state", null, getState());
			blendComponent.setToDraw(get());
			blendComponent.revalidate();
			blendComponent.repaint();
		} catch (InterruptedException ex) {
			logger.log(Level.FINE, "Blending interrupted: ", ex);
		} catch (ExecutionException ex) {
			logger.log(Level.FINE, "Blending execution problem: ", ex);
		}
	}
}