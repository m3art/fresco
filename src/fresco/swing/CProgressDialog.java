/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 *
 * @author gimli
 */
public class CProgressDialog extends JDialog implements PropertyChangeListener, ActionListener {

	JProgressBar progressBar = new JProgressBar();
	JButton killer = new JButton("Cancel");
	SwingWorker worker;

	public CProgressDialog(JComponent centerComponent, SwingWorker worker) {
		super(new JFrame(), "blending ...", ModalityType.APPLICATION_MODAL);
		this.worker = worker;
		this.setLayout(new BorderLayout());
		this.setUndecorated(true);
		this.add(new Label("blending ..."), BorderLayout.NORTH);
		this.add(progressBar);
		killer.addActionListener(this);
		this.add(killer, BorderLayout.SOUTH);
		this.pack();
		this.setLocationRelativeTo(centerComponent);
	}

	public void setWorker(SwingWorker worker) {
		this.worker = worker;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("state".equals(evt.getPropertyName())) {
			if ("STARTED".equals(evt.getNewValue().toString())) {
				progressBar.setIndeterminate(true);
			} else if ("DONE".equals(evt.getNewValue().toString())) {
				progressBar.setIndeterminate(false);
				progressBar.setValue(0);
			}
		} else if ("progress".equals(evt.getPropertyName())) {
			int value = (Integer) (evt.getNewValue());
			progressBar.setIndeterminate(false);
			progressBar.setValue(value);
		}
	}

	public void actionPerformed(ActionEvent e) {
		worker.cancel(false);
		progressBar.setIndeterminate(false);
		progressBar.setValue(0);
		setVisible(false);
	}
}
