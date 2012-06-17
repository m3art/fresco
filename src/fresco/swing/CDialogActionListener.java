package fresco.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JDialog;
import workers.CImageWorker;

/**
 * @author gimli
 * @version Nov 5, 2011
 */
public class CDialogActionListener implements ActionListener {

	private final CImageWorker worker;
	private final static Logger logger = Logger.getLogger(CDialogActionListener.class.getName());

	public CDialogActionListener(CImageWorker worker) {
		this.worker = worker;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Component component = (Component)e.getSource();
		while(!(component instanceof JDialog)) {
			component = component.getParent();
		}
		((JDialog)component).setVisible(false);
		((JDialog)component).dispose();

		if (e.getActionCommand().equals(CWorkerDialogFactory.OK_COMMAND)) {
			if (worker.confirmDialog()) {
				worker.execute();
			} else {
				logger.warning("Params are not valid");
			}
		} else if (e.getActionCommand().equals(CWorkerDialogFactory.CANCEL_COMMAND)) {
			logger.fine("Dialog canceled.");
		}
	}
}
