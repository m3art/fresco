/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/** Formatter used for logging in one line */
public class CShortFormatter extends Formatter {

	final String shortFormat = "HH:mm:ss";
	final String longFormat = "HH:mm:ss.SSS";
	DateFormat df = new SimpleDateFormat(longFormat);
	int CODEPOINT_LIMIT = 35;
	boolean SHOW_THREAD_NAME = false; // may be slow

	@Override
	public String format(LogRecord r) {
		StringBuilder sb = new StringBuilder();
		sb.append(df.format(new Date(r.getMillis()))).append("[").append(r.getThreadID()).append(getThreadName()).append("]").append(" ").append(shorten(r.getSourceClassName() + "#" + r.getSourceMethodName(), CODEPOINT_LIMIT)).append(" ").append(r.getLevel()).append(": ").append(super.formatMessage(r)).append("\n");

		return sb.toString();
	}

	private String getThreadName() {
		if (SHOW_THREAD_NAME) {
			return "-" + Thread.currentThread().getName();
		} else {
			return "";
		}
	}

	private String shorten(String name, int limit) {
		int len = name.length();
		if (len <= limit) {
			return name;
		} else {
			return "~" + name.substring(len - limit + 1, len);
		}
	}
}
