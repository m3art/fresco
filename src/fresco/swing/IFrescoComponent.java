/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

/**
 *
 * @author gimli
 */
public interface IFrescoComponent {

	/**
	 * Goes through all subcomponents and checks all menus, items etc
	 * if they are correctly enabled
	 */
	public void checkEnabled();
}
