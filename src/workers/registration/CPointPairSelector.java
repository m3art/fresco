/*
 * 
 *  Part of Fresco software under GPL licence
 *  http://www.gnu.org/licenses/gpl-3.0.txt
 * 
 */
package workers.registration;

import fresco.swing.CWorkerDialogFactory;
import info.clearthought.layout.TableLayout;
import java.awt.image.BufferedImage;
import javax.swing.*;
import workers.CImageWorker;

/**
 *
 * @author Jakub
 */
public class CPointPairSelector extends CImageWorker<CPointPairs, Void> {

	protected CDerotationCorrelator correlator;
	protected int requestedPairs;
	protected BufferedImage imageA, imageB;

	@Override
	public String getTypeName() {
		return "REGISTRATION";
	}

	@Override
	public String getWorkerName() {
		return "Point pair selector";
	}

	public CPointPairSelector(BufferedImage imageA, BufferedImage imageB, int requestedPairs) {
		this.imageA = imageA;
		this.imageB = imageB;
		this.correlator = new CDerotationCorrelator(imageA, imageB);
		this.requestedPairs = requestedPairs;
	}

	/**
	 * given a correlator(something that takes two images and returns points and
	 * their correlations), selects the best correlated pairs using an eager
	 * algorithm
	 *
	 */
	private CPointPairs getPairs(CDerotationCorrelator correlator, int pairCount) {
		CPointPairs pairs = new CPointPairs();
		double[][] correlations = correlator.doInBackground();
		CPointsAndQualities ptsA = correlator.ptsA;
		CPointsAndQualities ptsB = correlator.ptsB;
    //eager algorithm
		// take best remaining correlated pair, remove both points from selection, repeat
		double maxCor;
		do {
			maxCor = 0.0;
			int maxIdxA = -1;
			int maxIdxB = -1;
			for (int i = 0; i < ptsA.size(); i++) {
				for (int j = 0; j < ptsB.size(); j++) {
					if (correlations[i][j] > maxCor) {
						maxCor = correlations[i][j];
						maxIdxA = i;
						maxIdxB = j;
					}
				}
			}
			// no "good enough" remaining pair found 
			if (maxCor == 0.0) {
				break;
			}
			pairs.addPointPair(ptsA.getPoint(maxIdxA), ptsB.getPoint(maxIdxB));
			//too many pairs
			if (pairs.size() >= pairCount) {
				break;
			}
			//remove usd points from selection
			for (int i = 0; i < ptsA.size(); i++) {
				correlations[i][maxIdxB] = 0.0;
			}
			for (int j = 0; j < ptsB.size(); j++) {
				correlations[maxIdxA][j] = 0.0;
			}
		} while (maxCor > 0.0);
		return pairs;

	}

	@Override
	protected CPointPairs doInBackground() {
		CPointPairs pairs = getPairs(correlator, requestedPairs);

		return pairs;
	}

	@Override
	public Type getType() {
		return Type.REGISTRATION;
	}

	private JTextField reqPairsInput = new JTextField();

	@Override
	public boolean confirmDialog() {
		this.requestedPairs = Integer.parseInt(reqPairsInput.getText());
		return true;
	}

	@Override
	public JDialog getParamSettingDialog() {
		JPanel content = new JPanel();
		TableLayout layout = new TableLayout(new double[]{200, 100}, new double[]{20});
		content.setLayout(layout);

		content.add(new JLabel("Set number of pairs: "), "0,0");
		content.add(reqPairsInput, "1,0");

		return CWorkerDialogFactory.createOkCancelDialog(this, content);
	}
}
