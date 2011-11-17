/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import file.CImageFile;
import fresco.CData;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import fresco.logging.CLogParser;

/**
 * Parameters last used Fresco
 * @author gimli
 */
public final class CAppWindow extends JFrame implements WindowListener, IFrescoComponent {

	/** Files which was opened but not closed */
	ArrayList<String> fileOpened;
	CMenuBar menuBar;
	CContentPane contentPane;
	/** logger */
	private static final Logger logger = Logger.getLogger(CAppWindow.class.getName());

	public CAppWindow(String label) {
		super(label);
		init();
		try {
			load(CLogParser.parse(CData.workspace));
		} catch (Exception e) {
			logger.warning("Broken workspace.fresco.");
		}
	}

	private void init() {
		fileOpened = new ArrayList();
		setIconImage(CImageFile.loadResource("/icons/Zebra-128x128.png"));
		setLocation(new Point(0, 0));
		setPreferredSize(new Dimension(800, 600));
		addWindowListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setJMenuBar(menuBar = new CMenuBar());
		setContentPane(contentPane = new CContentPane());


		pack();
		checkEnabled();
	}

	@Override
	public void checkEnabled() {
		contentPane.checkEnabled();
		menuBar.checkEnabled();
	}

	/**
	 * Loads application params from a file
	 * @param params preprocessed files
	 */
	public void load(Properties params) {
		CData.setWorkingPath(new File(params.getProperty("working path", ".")));

		try {
			String posValue = params.getProperty("position", "0x0");
			int xPos = posValue.indexOf('x');
			setLocation(new Point(Integer.valueOf(posValue.substring(0, xPos)),
					Integer.valueOf(posValue.substring(xPos + 1))));
		} catch (NumberFormatException nfe) {
			setLocation(new Point(0, 0));
		}

		try {
			setSize(new Dimension(Integer.valueOf(params.getProperty("width", "800")),
					Integer.valueOf(params.getProperty("height", "600"))));
		} catch (NumberFormatException nfe) {
			setPreferredSize(new Dimension(800, 600));
		}

		int filesOpened = 0;
		while (true) {
			if (params.getProperty("open" + filesOpened) == null) {
				break;
			}
			fileOpened.add(params.getProperty("open" + filesOpened));
			filesOpened++;
		}
	}

	/**
	 * Saves appParams to workspace file
	 */
	public void save() {
		Properties params = new Properties();
		logger.info("Saving settings");

		params.setProperty("position", getLocation().x + "x" + getLocation().y);
		params.setProperty("width", "" + getSize().width);
		params.setProperty("height", "" + getSize().height);
		params.setProperty("working path", "" + CData.getWorkingPath().getPath());

		for (int i = 0; i < CData.imagesSize(); i++) {
			params.setProperty("open" + i, CData.getImage(i).getFile().toString());
		}


		try {
			File file = new File(CData.workspace);
			FileOutputStream fos = new FileOutputStream(file);
			Pattern regexp = Pattern.compile("[\\{\\}]");
			String rawParams = params.toString().replace(", ", "\n").replaceAll(regexp.toString(), "");

			fos.write(rawParams.getBytes());
			logger.info(rawParams);
		} catch (FileNotFoundException fnfe) {
			logger.log(Level.INFO, "Unable to store properties: {0}", fnfe.getMessage());
		} catch (IOException ioe) {
			logger.log(Level.INFO, "Problem with output file: {0}", ioe.getMessage());
		}

	}

	public void addFile(String path) {
		fileOpened.add(path);
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		save();
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
