/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 *
 * http://lclevy.free.fr/cr2/
 */
package file;

import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author gimli
 */
public class CTiffImage {

	boolean bigEndian;
	int tiffOffset;
	int versionMajor;
	int versionMinor;
	int rawIfdOffset;
	LinkedList<IDFEntry> idfEntries = new LinkedList<IDFEntry>();

	public static IDFEntry parseIDFEntry(CInputStream cis) throws IOException {
		return new IDFEntry(cis.readShort(), IDFEntry.getType(cis.readShort()), cis.readInt(), cis.readInt());
	}

	public void addIDFEntry(CInputStream cis) throws IOException {
		idfEntries.add(parseIDFEntry(cis));
	}

	/*
	 *  From IFD#0:
	Camera make is taken from tag #271 (0x10f)
	Camera model is from tag #272 (0x110)
	model ID from Makernotes, Tag #0x10
	white balance information is taken from tag #0x4001
	From IFD#3:
	StripOffset, offset to RAW data : tag #0x111
	StripByteCount, length of RAW data : tag #0x117
	image slice layout (cr2_slice[]) : tag #0xc640
	the RAW image dimensions is taken from lossless jpeg (0xffc3 section)
	 */
	public static class IDFEntry {

		public static enum IDFType {

			unsignedChar, String, unsignedShort, unsignedLong,
			unsignedRational, signedChar, byteSequence, sighnedShort, signedLong,
			singedRational, float4bytes, float8bytes
		};

		public static IDFType getType(int value) throws IOException {
			switch (value) {
				case 1:
					return IDFType.unsignedChar;
				case 2:
					return IDFType.String;
				case 3:
					return IDFType.unsignedShort;
				case 4:
					return IDFType.unsignedLong;
				case 5:
					return IDFType.unsignedRational;
				case 6:
					return IDFType.signedChar;
				case 7:
					return IDFType.byteSequence;
				case 8:
					return IDFType.sighnedShort;
				case 9:
					return IDFType.signedLong;
				case 10:
					return IDFType.singedRational;
				case 11:
					return IDFType.float4bytes;
				case 12:
					return IDFType.float8bytes;
				default:
					throw new IOException("Unknown IFD type");
			}
		}
		final int id;
		final IDFType type;
		final int numOfValues;
		final int ptrOrValue;

		private IDFEntry(int id, IDFType type, int valCount, int ptr) {
			this.id = id;
			this.type = type;
			this.numOfValues = valCount;
			this.ptrOrValue = ptr;
		}
	}
}
