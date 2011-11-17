/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 *
 * @author gimli
 */
public class CButton extends JButton implements IAction {

	/** specifikace uzivatelske akce */
	RegID id;

	public CButton(String label, RegID id) {
		super(label);
		this.id = id;
	}

	public CButton(ImageIcon icon, RegID id) {
		super(icon);
		this.id = id;
	}

	public CButton(String label, ImageIcon icon, RegID id) {
		super(label, icon);
		this.id = id;
	}

	public CButton(RegID id) {
		super();
		this.id = id;
	}

	/**
	 * vraci identifikator uzivatelske akce registrovany v CUserAction
	 */
	public RegID getID() {
		return id;
	}
}
