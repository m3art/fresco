/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package utils.geometry;

import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author gimli
 */
public class CMaximalRectangle {

	public static Rectangle getRect(Point center, int range, Rectangle boundary) {
		int x = Math.max(boundary.x, center.x - range), y = Math.max(boundary.y, center.y - range);
		int maxX = Math.min(center.x + range, boundary.width + boundary.x),
				maxY = Math.min(center.y + range, boundary.height + boundary.y);

		return new Rectangle(x, y, maxX - x + 1, maxY - y + 1);
	}
}
