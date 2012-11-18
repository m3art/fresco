/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author gimli
 */
public class CLogParser {

	public static char Delimiter = '=';

	/**
	 * Stored setting of application is managed here
	 * @param filename specifies where are parameters stored
	 * @return table of parameters which are set
	 * @throws FileNotFoundException if no file is found
	 * @throws IOException problem with file reading
	 */
	public static Properties parse(String filename) throws FileNotFoundException, IOException {
		File file = new File(filename);
		Properties params = new Properties();
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;

		while ((line = reader.readLine()) != null) {

			int delPos = line.indexOf(Delimiter);
			String key = line.substring(0, delPos).toLowerCase();

			if (!key.contains("open")) {
				params.setProperty(line.substring(0, delPos).toLowerCase(), line.substring(delPos + 1));
			} else {
				params.setProperty(key, line.substring(delPos + 1));
			}
		}

		return params;
	}
}
