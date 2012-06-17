package fresco.swing;

import fresco.CData;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import workers.CImageWorker;

/**
 * @author gimli
 * @version Nov 5, 2011
 */
public class CWorkerDialogFactory {

	public static final String OK_COMMAND = "OK";
	public static final String CANCEL_COMMAND = "Cancel";

	public static JDialog createOkCancelDialog(CImageWorker worker, JComponent content) {

		JDialog dialog = new JDialog(CData.mainFrame, worker.getWorkerName(), ModalityType.APPLICATION_MODAL);
		dialog.setLocationRelativeTo(CData.mainFrame.contentPane);
		dialog.setFocusCycleRoot(true);

		JButton ok = new JButton(OK_COMMAND);
		JButton cancel = new JButton(CANCEL_COMMAND);
		final CDialogActionListener actionListener = new CDialogActionListener(worker);

		ok.setActionCommand(OK_COMMAND);
		ok.addActionListener(actionListener);
		cancel.setActionCommand(CANCEL_COMMAND);
		cancel.addActionListener(actionListener);

		dialog.setLayout(new BorderLayout());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(ok);
		buttons.add(cancel);
		dialog.add(content, BorderLayout.CENTER);
		dialog.add(buttons, BorderLayout.SOUTH);

		dialog.getRootPane().setDefaultButton(ok);
		dialog.validate();
		dialog.pack();

		return dialog;
	}
}
