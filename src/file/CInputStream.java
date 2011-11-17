/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package file;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author gimli
 */
public class CInputStream {

	private final InputStream inputStream;
	private final boolean isBigEndian;

	public CInputStream(InputStream stream, boolean isBigEndian) {
		inputStream = stream;
		this.isBigEndian = isBigEndian;
	}

	public int read() throws IOException {
		return inputStream.read();
	}

	public int readInt() throws IOException {
		if (isBigEndian) {
			return (((((read() << 8) | read()) << 8) | read()) << 8) | read();
		} else {
			return read() | (read() << 8) | (read() << 16) | (read() << 24);
		}
	}

	public short readShort() throws IOException {
		if (isBigEndian) {
			return (short) ((read() << 8) | read());
		} else {
			return (short) (read() | (read() << 8));
		}
	}
}
