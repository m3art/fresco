/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CData;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import workers.registration.CBlendWorker;

/**
 * @author gimli
 * @version Mar 25, 2011
 */
public class CBlendComponent extends CDrawPanel {

	int alpha = 0;
	CBlendWorker worker;
	CProgressDialog dialog;
	static final Logger logger = Logger.getLogger(CBlendComponent.class.getName());

	public CBlendComponent() {
		dialog = new CProgressDialog(this, worker);
	}

	public void setAlpha(int value) {
		alpha = value;
	}

	@Override
	public void setSizeByImage() {
		if (CData.showImage[0] == -1 || CData.showImage[2] == -1) {
			return;
		} else {
			setSize(0, 0);
		}

		BufferedImage inputA = CData.getImage(CData.showImage[0]).getTransformedImage(CData.view),
				inputB = CData.getImage(CData.showImage[2]).getTransformedImage(CData.view);

		Dimension realSize = new Dimension(Math.max(inputA.getWidth(), inputB.getWidth()),
				Math.max(inputA.getHeight(), inputB.getHeight()));
		setSize(realSize.width * CData.getFocus() / 100, realSize.height * CData.getFocus() / 100);
		logger.log(Level.FINE, "Panel resized by new image size: {0}x{1}", new Object[]{getWidth(), getHeight()});

		worker = new CBlendWorker(this, alpha);

		worker.addPropertyChangeListener(dialog);
		worker.execute();
		dialog.setVisible(true);
	}

	public void setToDraw(BufferedImage toDraw) {
		this.toDraw = toDraw;
		worker.removePropertyChangeListener(dialog);
		dialog.setVisible(false);
	}
}
