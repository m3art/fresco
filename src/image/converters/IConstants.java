/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.converters;

public interface IConstants {

	int rgb_bands = 3,
			hsl_bands = 3,
			hsv_bands = 3,
			xyz_bands = 3,
			uvY_bands = 3;
	/*
	 * this are necessary conversions
	 *
	 *
	static int 	TYPE_3BYTE_BGR
	Represents an image with 8-bit RGB color components, corresponding to a Windows-style BGR color model) with the colors Blue, Green, and Red stored in 3 bytes.
	static int 	TYPE_4BYTE_ABGR
	Represents an image with 8-bit RGBA color components with the colors Blue, Green, and Red stored in 3 bytes and 1 byte of alpha.
	static int 	TYPE_4BYTE_ABGR_PRE
	Represents an image with 8-bit RGBA color components with the colors Blue, Green, and Red stored in 3 bytes and 1 byte of alpha.
	static int 	TYPE_BYTE_BINARY
	Represents an opaque byte-packed 1, 2, or 4 bit image.
	static int 	TYPE_BYTE_GRAY
	Represents a unsigned byte grayscale image, non-indexed.
	static int 	TYPE_BYTE_INDEXED
	Represents an indexed byte image.
	static int 	TYPE_CUSTOM
	Image type is not recognized so it must be a customized image.
	static int 	TYPE_INT_ARGB
	Represents an image with 8-bit RGBA color components packed into integer pixels.
	static int 	TYPE_INT_ARGB_PRE
	Represents an image with 8-bit RGBA color components packed into integer pixels.
	static int 	TYPE_INT_BGR
	Represents an image with 8-bit RGB color components, corresponding to a Windows- or Solaris- style BGR color model, with the colors Blue, Green, and Red packed into integer pixels.
	static int 	TYPE_INT_RGB
	Represents an image with 8-bit RGB color components packed into integer pixels.
	static int 	TYPE_USHORT_555_RGB
	Represents an image with 5-5-5 RGB color components (5-bits red, 5-bits green, 5-bits blue) with no alpha.
	static int 	TYPE_USHORT_565_RGB
	Represents an image with 5-6-5 RGB color components (5-bits red, 6-bits green, 5-bits blue) with no alpha.
	static int 	TYPE_USHORT_GRAY
	Represents an unsigned short grayscale image, non-indexed).
	 */
}
