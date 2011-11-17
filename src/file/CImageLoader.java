/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package file;

import fresco.CData;
import fresco.CImageContainer;
import fresco.action.CStatusManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author gimli
 */
public class CImageLoader extends SwingWorker<List<CImageContainer>, CImageContainer> {

	final File[] pictures;
	int published = 0;
	private final static Logger logger = Logger.getLogger(CImageLoader.class.getName());

	public CImageLoader(File[] files) {
		pictures = files;
	}

	@Override
	protected void process(List<CImageContainer> images) {
		for (CImageContainer cont : images) {
			CData.addImage(cont);
			firePropertyChange(CStatusManager.MESSAGE_INFO, "", "File " + pictures[published].getName() + " loaded.");
			published++;
		}
	}

	@Override
	protected List<CImageContainer> doInBackground() {
		if (pictures != null && pictures.length > 0) {
			firePropertyChange(CStatusManager.MESSAGE_INFO, "", "Opening images ...");
		}

		ArrayList<CImageContainer> newImages = new ArrayList<CImageContainer>(pictures.length);

		for (int i = 0; i < pictures.length; i++) {
			try {
				CImageContainer newContainer = new CImageContainer(CImageFile.loadImage(pictures[i].getAbsolutePath()), pictures[i]);
				newImages.add(newContainer);
				publish(newContainer);

			} catch (IOException e) {
				logger.info("File data unrecognized: " + e.getMessage());
				firePropertyChange(CStatusManager.MESSAGE_ERROR, "", "Image data not recognized: " + pictures[i].getName());
				return null;
			} catch (Exception e) {
				logger.info("Unqualified error " + e.getMessage());
				firePropertyChange(CStatusManager.MESSAGE_ERROR, "", "Image data not recognized: " + pictures[i].getName());
			}
			setProgress(i * 100 / (pictures.length - 1));
		}
		return newImages;
	}
}
