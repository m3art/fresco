/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction;
import javax.swing.JMenuItem;

/**
 *
 * @author gimli
 */
public class CPopupMenuItem extends JMenuItem implements IAction {

	/** specifikace uzivatelske akce */
	RegID id;
	/** reference to loaded item */
	int ref;

	public CPopupMenuItem(String label, RegID id, int index) {
		super(label);
		this.id = id;
		ref = index;
	}

	/**
	 * vraci identifikator uzivatelske akce registrovany v CUserAction
	 */
	public RegID getID() {
		return id;
	}

	public int getRef() {
		return ref;
	}
}
