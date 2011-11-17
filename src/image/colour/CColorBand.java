/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package image.colour;

import javax.swing.JComboBox;

/**
 *
 * @author gimli
 */
public class CColorBand {

	public static enum Band {

		hue("hue"),
		saturation("saturation"),
		value("value"),
		lightness("lightness"),
		red("red"),
		green("green"),
		blue("blue"),
		gray("gray");
		String name;

		Band(String name) {
			this.name = name;
		}

		String getName() {
			return name;
		}
	};

	public static Band getBand(String name) {
		for (Band band : Band.values()) {
			if (name.equalsIgnoreCase(band.getName())) {
				return band;
			}
		}

		return Band.gray;
	}

	public static JComboBox getCombo() {
		JComboBox out = new JComboBox(Band.values());

		return out;
	}
}
