/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.action;

import file.CImageFile;
import fresco.CData;
import fresco.swing.CButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 *
 * @author gimli
 */
public class CStatusManager extends JPanel implements PropertyChangeListener {

	public static int messageTimeout = 7000;
	public static String MESSAGE_INFO = "MESSAGE_INFO", MESSAGE_ERROR = "MESSAGE_ERROR", MESSAGE_LOG = "Message log";
	public static SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
	private StringBuilder userMessages = new StringBuilder("<b>Messages:</b><br />");
	JPopupMenu messageExpand;
	JPanel westComponent;
	JProgressBar progressBar;
	JLabel messageLabel;
	JLabel userLog;
	Timer messageTimer;
	JButton killer;
	CActionManager manager;
	private static Logger logger = Logger.getLogger(CStatusManager.class.getName());

	public CStatusManager() {
		super(new BorderLayout());

		messageLabel = new JLabel();
		add(messageLabel, BorderLayout.LINE_START);

		userLog = new JLabel(userMessages.toString());
		userLog.setBorder(new EmptyBorder(3, 3, 3, 3));

		messageExpand = new JPopupMenu(MESSAGE_LOG);
		messageExpand.setBorder(new LineBorder(Color.BLACK));
		messageExpand.add(userLog);

		messageLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				checkForTriggerEvent(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				checkForTriggerEvent(e);
			}

			private void checkForTriggerEvent(MouseEvent e) {
				if (e.isPopupTrigger()) {
					messageExpand.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		messageTimer = new Timer(messageTimeout, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				messageLabel.setText(" Ready");
			}
		});

		westComponent = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		westComponent.setBorder(null);
		killer = new CButton(CImageFile.createImageIcon("/icons/close.png", "close.png"), IAction.RegID.kill);
		killer.setPreferredSize(new Dimension(18, 18));
		killer.setBorder(null);
		killer.addActionListener(CData.userActionListener);
		westComponent.add(killer);

		progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
		progressBar.setPreferredSize(new Dimension(200, 15));
		westComponent.add(progressBar);
		add(westComponent, BorderLayout.LINE_END);
		setMessage("Message bar loaded");
	}

	public void setManager(CActionManager manager) {
		this.manager = manager;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (manager == null) {
			logger.warning("Action manager is not set - unable to set output image");
		}
		if ("state".equals(evt.getPropertyName())) {
			progressBar.setVisible(true);
			if ("STARTED".equals(evt.getNewValue().toString())) {
				killer.setVisible(true);
				progressBar.setIndeterminate(true);
			} else if ("DONE".equals(evt.getNewValue().toString())) {
				progressBar.setIndeterminate(false);
				progressBar.setValue(0);
				if (evt.getSource() != null) {
					manager.cleanImageWorker();
				}
				killer.setVisible(false);
			}
		} else if ("progress".equals(evt.getPropertyName())) {
			int value = (Integer) (evt.getNewValue());
			progressBar.setVisible(true);
			progressBar.setIndeterminate(false);
			progressBar.setValue(value);
		} else if (MESSAGE_INFO.equals(evt.getPropertyName())) {
			setMessage((String) evt.getNewValue());
		} else if (MESSAGE_ERROR.equals(evt.getPropertyName())) {
			setMessage((String) evt.getNewValue());
		}
	}

	public void setMessage(String text) {
		messageLabel.setForeground(Color.black);
		messageLabel.setText(" " + text);
		updateLog(text);
		messageTimer.restart();
	}

	public void setErrorMessage(String text) {
		messageLabel.setForeground(Color.red);
		messageLabel.setText(" " + text);
		updateLog("<b>" + text + "</b>");
		messageTimer.restart();
	}

	private void updateLog(String message) {
		userMessages.append("[");
		userMessages.append(sdf.format(System.currentTimeMillis()));
		userMessages.append("]: ");
		userMessages.append(message);
		userMessages.append("<br />");
		userLog.setText("<html><body>" + userMessages.toString() + "</body></html>");
	}
}
