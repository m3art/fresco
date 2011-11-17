/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author gimli
 */
public class CImageReader {

	private static CTiffImage parseHeader(CInputStream cis, CTiffImage img) throws IOException {
		short tiffMagic = cis.readShort();
		if (tiffMagic != 0x2a) {
			throw new NoCR2Exception("No tiff magic - unsupported media.");
		}

		img.tiffOffset = cis.readInt();

		short cr2magic = cis.readShort();
		if (cr2magic != 4352) {
			throw new NoCR2Exception("CR2 magic missing, corrupted file.");
		}

		img.versionMajor = cis.read();
		img.versionMinor = cis.read();
		img.rawIfdOffset = cis.readInt();

		return img;
	}

	private static CTiffImage parseImageFileDirectory(CInputStream cis, CTiffImage img) throws IOException {

		int numberOfEntries = cis.readShort();

		// read all IDF
		for (int i = 0; i < numberOfEntries; i++) {
			img.addIDFEntry(cis);
		}

		// find next IDF info
		img.rawIfdOffset = cis.readInt();

		return img;
	}

	public static CTiffImage parseTiff(File file) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(file);
		CTiffImage img = new CTiffImage();

		int byteOrder = (fis.read() << 8) | fis.read();

		if (byteOrder == 0xdfdf) {
			img.bigEndian = false;
		} else if (byteOrder == 0x4d4d) {
			img.bigEndian = true;
		} else {
			throw new NoCR2Exception("Unknown endianity - probably wrong image parser");
		}

		CInputStream cis = new CInputStream(fis, img.bigEndian);

		parseHeader(cis, img);

		return img;
	}
}
