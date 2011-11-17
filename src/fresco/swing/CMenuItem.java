/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.action.IAction;
import javax.swing.JMenuItem;

/**
 * FIXME: translate comments
 * Slouzi k rozpoznani typu JMenuItem, kazda uzivatelska akce ma svuj
 * identifikator, podle ktereho se pozna. Tato trida k JMenuItem pridava
 * asociovanou akci
 * @author gimli
 */
public class CMenuItem extends JMenuItem implements IAction {

	/** specifikace uzivatelske akce */
	RegID id;

	public CMenuItem(String label, RegID id) {
		super(label);
		this.id = id;
	}

	/**
	 * vraci identifikator uzivatelske akce registrovany v CUserAction
	 */
	public RegID getID() {
		return id;
	}
}
