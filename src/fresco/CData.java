/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco;

import fresco.CImageContainer.Screen;
import fresco.action.CStatusManager;
import fresco.action.CUserActionListener;
import fresco.swing.CAppWindow;
import fresco.swing.CContentPane;
import fresco.swing.CToolBar;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import workers.registration.CPointPairs;

/**
 *
 * @author gimli
 */
public class CData {

  /**
   * `
   * Fresco offers 6 possible views: <ul> <li> one of RGB color channels (in
   * gray) - usefull in some kind of component analysis</li> <li>gray image -
   * good for multimodal data</li> <li>mean - for segmented images</li>
   * <li>original - elsewere</li> </ul> Current value is stored in static param
   * <b>view</b>
   */
  /**
   * User defined type of view
   */
  public static Screen view = CImageContainer.Screen.MEANED;
  /*
   * Name of file which holds app config values
   */
  public static final String workspace = "workspace.fresco";
  /**
   * all final (except loading images) user actions are managed here
   */
  public static final CUserActionListener userActionListener = new CUserActionListener();
  /**
   * Handler for all loaded images
   */
  private static ArrayList<CImageContainer> images = new ArrayList<CImageContainer>();
  /**
   * Showed images - only index
   */
  public static int[] showImage = {-1, -1, -1};
  /**
   * If the panel for 2nd input is showed
   */
  public static boolean input2Showed;
  /**
   * User defined image zoom
   */
  private static int focus = 100;
  /**
   * handler for JFrame
   */
  public static CAppWindow mainFrame;
  /**
   * current folder from which we pull the images
   */
  private static File workingPath;
  /**
   * container for worker output
   */
  public static CImageContainer output;
  /**
   * storage for point pairs extracted during registration
   */
  public static CPointPairs pairs;
  /**
   * Toolbar defines set of tools.
   *
   * @param tool holds id of this tool
   */
  public static CToolBar.Tool tool;
  /**
   * Logger
   */
  public static final Logger logger = Logger.getLogger(CData.class.getName());

  public static synchronized File getWorkingPath() {
    return workingPath;
  }

  public static synchronized void setWorkingPath(File newValue) {
    workingPath = newValue;
  }

  public static synchronized void addImages(ArrayList<CImageContainer> newImages) {
    if (newImages != null) {
      images.addAll(newImages);
      userActionListener.getManager().fireImageListChanged();
    }
  }

  public static void addImage(CImageContainer container) {
    if (container != null) {
      images.add(container);
      userActionListener.getManager().fireImageListChanged();
    }
  }

  public static CImageContainer getImage(int id) {
    if (id < images.size()) {
      return images.get(id);
    }
    return null;
  }

  public static CImageContainer removeImage(int id) {
    if (id < images.size() && id >= 0) {
      return images.remove(id);
    } else {
      return null;
    }
  }

  public static synchronized int imagesSize() {
    return images.size();
  }

  public static synchronized int getFocus() {
    return focus;
  }

  public static synchronized void setFocus(int value) {
    focus = value;
  }

  public static synchronized void setTool(CToolBar.Tool newTool) {
    tool = newTool;
  }

  public static CToolBar.Tool getTool() {
    return tool;
  }

  public static CStatusManager getProgressBar() {
    return ((CContentPane) mainFrame.getContentPane()).getProgressBar();
  }
}
