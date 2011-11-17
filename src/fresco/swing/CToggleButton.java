/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 *
 * @author gimli
 */
public class CToggleButton extends JToggleButton implements IAction {

	RegID regID;

	public CToggleButton(Icon icon, RegID regID) {
		super(icon);
		this.regID = regID;
	}

	public RegID getID() {
		return regID;
	}
}
