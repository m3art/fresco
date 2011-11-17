/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */

package fresco.swing;

import fresco.action.IAction;
import javax.swing.JRadioButtonMenuItem;

/**
 *
 * @author gimli
 */
public class CRadioMenuItem extends JRadioButtonMenuItem implements IAction {
	private RegID id;

	public CRadioMenuItem(String label, boolean selected, RegID id) {
		super(label, selected);
		this.id = id;
	}

	public RegID getID() {
		return id;
	}
}
