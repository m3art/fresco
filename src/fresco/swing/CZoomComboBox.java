/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction;
import java.awt.Dimension;
import java.util.logging.Logger;
import javax.swing.JComboBox;

/**
 *
 * @author gimli
 */
public class CZoomComboBox extends JComboBox implements IAction {

	String[] zoomValues = {"10%", "25%", "50%", "75%", "100%", "200%", "300%"};
	static final Logger logger = Logger.getLogger(CZoomComboBox.class.getName());
	int value;

	public CZoomComboBox() {
		setEditable(true);
		setModel(new javax.swing.DefaultComboBoxModel(zoomValues));
		setSelectedIndex(4);
		setToolTipText("Set scale of showed images");
		setMaximumSize(new Dimension(70, 20));

	}

	public RegID getID() {
		return RegID.zoom;
	}

	public int getParam() {
		switch (getSelectedIndex()) {
			case 0:
				return 10;
			case 1:
				return 25;
			case 2:
				return 50;
			case 3:
				return 75;
			case 4:
				return 100;
			case 5:
				return 200;
			case 6:
				return 300;
			default:
				return evaluate();
		}
	}

	private int evaluate() {
		try {
			String userValue = ((String) getSelectedItem()).trim();
			boolean formatted = false;

			if (userValue.endsWith("%")) {
				userValue = userValue.substring(0, userValue.length() - 1).trim();
				formatted = true;
			}

			int value = Integer.valueOf(userValue);

			if (value > 400) {
				value = 400;
			}
			if (value < 1) {
				value = 1;
			}

			if (!formatted) {
				setSelectedItem(value + "%");
			}
			return value;
		} catch (NumberFormatException nfe) {
			logger.warning("Value is not valid integer.");

			setSelectedIndex(4);
			return 100;
		}
	}
}
