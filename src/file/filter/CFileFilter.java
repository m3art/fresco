/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package file.filter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * @author Honza Blažek
 */
public class CFileFilter extends FileFilter {

	public static enum ImageFormat {

		bmp("bmp", "Bitmap image (*.bmp)"),
		jpeg("jpeg", "JPEG image (*.jpeg, *.jpg, *.jpe)"),
		gif("gif", "GIF image (*.gif)"),
		pnm("pnm", "Portable Anymap (*.pnm)"),;
		String extension;
		String description;

		ImageFormat(String ext, String desc) {
			extension = ext;
			description = desc;
		}
	}
	private String supported;
	private String description;

	public CFileFilter(ImageFormat iform) {
		supported = iform.extension;
		description = iform.description;
	}

	public CFileFilter(String extension, String desc) {
		supported = extension;
		description = desc;
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
		boolean value = false;

		if (supported.compareToIgnoreCase(ext) == 0) {
			value = true;
		}

		return value;
	}

	public String getDescription() {
		return description;
	}

	public String getExtension() {
		return supported;
	}
}
