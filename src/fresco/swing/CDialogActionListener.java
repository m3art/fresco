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

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CWorkerDialogFactory.OK_COMMAND)) {
			if (worker.confirmDialog()) {
				// parameters are valid and correctly parsed
				Component component = (Component)e.getSource();
				while(!(component instanceof JDialog)) {
					component = component.getParent();
				}
				((JDialog)component).setVisible(false);
				((JDialog)component).dispose();
			} else {
				logger.warning("Params are not valid");
			}
		}
	}
}
