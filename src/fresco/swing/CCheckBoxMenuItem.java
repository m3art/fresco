/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction.RegID;
import javax.swing.JCheckBoxMenuItem;

/**
 *
 * @author gimli
 */
public class CCheckBoxMenuItem extends JCheckBoxMenuItem {

	RegID id;

	public CCheckBoxMenuItem(String label, boolean checked, RegID id) {
		super(label, checked);
		this.id = id;
	}

	public RegID getID() {
		return id;
	}
}
