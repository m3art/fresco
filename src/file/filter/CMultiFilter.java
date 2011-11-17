/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package file.filter;

import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author gimli
 */
public class CMultiFilter extends FileFilter {

	private String[] supported = ImageIO.getReaderFileSuffixes();

	public CMultiFilter(String[] extension) {
		supported = extension;
	}

	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		int extensionPosition = f.getName().lastIndexOf('.') + 1;

		if (extensionPosition < 0) {
			return false;
		}
		String ext = f.getName().substring(extensionPosition);
		int i;
		boolean value = false;

		for (i = 0; i < supported.length; i++) {
			if (supported[i].compareToIgnoreCase(ext) == 0) {
				value = true;
			}
		}

		return value;
	}

	public String getDescription() {
		String out = "";
		for (int i = 0; i < supported.length; i++) {
			out += "*." + supported[i].toLowerCase() + ", ";
		}
		return out;
	}
}
