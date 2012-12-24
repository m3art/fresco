/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.action;

import file.CImageFile;
import fresco.CData;
import fresco.CImageContainer;
import fresco.CImageContainer.Screen;
import fresco.action.IAction.RegID;
import fresco.swing.CContentPane;
import fresco.swing.CContentPane.Structure;
import fresco.swing.CDrawPanel;
import fresco.swing.CInfoFrame;
import fresco.swing.CPreviewBar;
import image.converters.Crgb2grey;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import support.regmarks.CPointPairsOverview;
import workers.CImageWorker;
import workers.analyse.CBestWindowSize;
import workers.analyse.CInterestingPointsThread;
import workers.analyse.paramObjects.CCOGParams;
import workers.registration.CPointPairs;
import workers.registration.CPointsAndQualities;
import workers.registration.refpointga.CRefPointMarker;
import workers.registration.CInterestingPoints;
import workers.registration.CInterestingPoints.Cornerer;
import workers.registration.CInterestingPoints.Edger;
import workers.segmentation.CSegmentMap;

/**
 * Holds all user actions in Fresco application
 * @author gimli
 */
public class CActionManager {

	CImageWorker imageWorker;
	CStatusManager progressBar;
	CDrawPanel[] imagePanel;
	private CContentPane content;
	private CPreviewBar previewBar;
	private static final Logger logger = Logger.getLogger(CActionManager.class.getName());

	/**
	 * Constructor have to handle a lot of gui panels
	 * @param imagePanel three panels for image painting
	 * @param content main window with images
	 * @param previewBar shortcut bar for image processing
	 * @param progressBar handler of progress bar for showing image worker progress
	 */
	public CActionManager(CDrawPanel[] imagePanel, CContentPane content, CPreviewBar previewBar, CStatusManager progressBar) {
		this.imagePanel = imagePanel;
		this.previewBar = previewBar;
		this.progressBar = progressBar;
		this.content = content;
	}

	/**
	 * Tool for image loading from files
	 */
	public void loadImage() {
		CImageFile.openPicture();
	}

	public void fireImageListChanged() {
		logger.info("Set of pictures changed ... update");

		if (CData.showImage[0] == -1 && CData.imagesSize() != 0) {
			CData.showImage[0] = 0;
			loadContainer(0, 0);
		}
		previewBar.refreshBar();
		refreshGUI();
	}

	/**
	 * Every user action calls refreshGUI, images are repainted, sizes are
	 * set once more etc.
	 */
	private static void refreshGUI() {
		CData.mainFrame.checkEnabled();
		CData.mainFrame.repaint();
		((CContentPane) CData.mainFrame.getContentPane()).getInputs().repaint();
	}

	/**
	 * Warning before image closing is generated there
	 * @param id image specification
	 */
	public void closePicture(int id) {
		if (CData.getImage(id).isChanged()) { // image is not saved
			int userChoose = JOptionPane.showOptionDialog(new JFrame("Image closing"),
					"Warning: Image is not saved. Do you want to save it before closing it?",
					"Image closing", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, null, null);

			if (userChoose == JOptionPane.CANCEL_OPTION || userChoose == JOptionPane.CLOSED_OPTION) {
				return;
			} else if (userChoose == JOptionPane.OK_OPTION) {
				savePicture(id);
			}
		}

		CData.removeImage(id);
		for (int i = 0; i < CData.showImage.length; i++) {
			if (CData.showImage[i] > id) {
				CData.showImage[i]--;
			} else if (CData.showImage[i] == id) {
				CData.showImage[i] = -1;
			}
		}
		previewBar.refreshBar();
		refreshGUI();
	}

	public void renamePicture(int id) {
		if (id < 0 || id > CData.imagesSize()) {
			return;
		}

		String filename = (String) JOptionPane.showInputDialog(
				new JFrame("Rename ..."),
				"Enter new name: ", "Rename ...",
				JOptionPane.PLAIN_MESSAGE, null, null,
				CData.getImage(id).getFilename());

		if (filename != null && filename.length() > 0) {
			CData.getImage(id).setFilename(filename);
			previewBar.refreshBar();
		}
	}

	public void saveAsPicture(int id) {
		CImageFile.saveAsPicture(CData.getImage(id));
	}

	public void savePicture(int id) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public void killProcess() {
		imageWorker.cancel(true);
		imageWorker.removePropertyChangeListener(progressBar);
		imageWorker = null;
	}

	/**
	 * User can choose from different types of view
	 * @param id defines type of view (CData.View)
	 */
	public void switchView(Screen id) {
		CData.view = id;
		refreshGUI();
	}

	/**
	 * This action is manager of CData.input2showed field
	 */
	public void show2ndInput() {
		CData.input2Showed = !CData.input2Showed;

		if (CData.input2Showed) {
			content.setStructure(content.getTwoImageStructure());
		} else {
			content.setStructure(Structure.oneInput);
		}

		CData.mainFrame.getJMenuBar().getMenu(2).getItem(0).setSelected(CData.input2Showed);
	}

	/**
	 * Action manages user defined zoom value
	 * @param percentage specify size of showed image in percents of real size
	 */
	public void setZoom(int percentage) {
		CData.setFocus(percentage);
		for (int i = 0; i < imagePanel.length; i++) {
			if (i < 3 && CData.showImage[i] != -1) {
				imagePanel[i].setSizeByImage();
				logger.log(Level.FINE, "Image size values: {0}, {1}", new Object[]{imagePanel[i].getWidth(), imagePanel[i].getHeight()});
			} else if (i > 2 && ((CContentPane) CData.mainFrame.getContentPane()).getStructure() == CContentPane.Structure.blend) {
				imagePanel[i].setSizeByImage();
			}
		}
	}

	/**
	 * Put specified container into specified image panel
	 * @param imagePanelID definition of image panel [0,1,2]
	 * @param containerID definition of container
	 */
	public void loadContainer(int imagePanelID, int containerID) {
		if (imagePanelID == 2 && !CData.input2Showed) {
			show2ndInput();
		}

		if (CData.imagesSize() > containerID) {
			CData.showImage[imagePanelID] = containerID;
		}

		imagePanel[imagePanelID].setSizeByImage();
		refreshGUI();
	}

	private void loadOutput() {
		if (CData.output == null) {
			return;
		}
		if (CData.showImage[1] != -2) {
			CData.showImage[1] = -2;
		}
		imagePanel[1].setSizeByImage();
	}

	/**
	 * Simplified multi-action which manage image container
	 * @param action defines type of user action
	 * @param containerID defines which container will be managed
	 */
	public void containerDo(RegID action, int containerID) {
		switch (action) {
			case close:
				closePicture(containerID);
				break;
			case rename:
				renamePicture(containerID);
				break;
			case open:
				loadContainer(0, containerID);
				break;
			case saveAs:
				saveAsPicture(containerID);
				break;
			case load2ndInput:
				loadContainer(2, containerID);
				break;
		}
	}

	public void showInfo() {
		JFrame infoFrame = new CInfoFrame(CData.getImage(CData.showImage[0]));
		infoFrame.setVisible(true);
	}

	public void runImageWorker(RegID id, Object[] params) {
		imageWorker = CImageWorker.createWorker(id, params);

		JDialog dialog = imageWorker.getParamSettingDialog();
		if (dialog != null) {
			dialog.setVisible(true);
		}
    imageWorker.addPropertyChangeListener(progressBar);
		imageWorker.execute();
	}

	public void cleanImageWorker() {
		if (imageWorker == null) {
			return;
		}
		imageWorker.removePropertyChangeListener(progressBar);
		try {
			logger.log(Level.INFO, "{0}, {1}", new Object[]{imageWorker.getTypeName(), imageWorker.getWorkerName()});
			
      switch (imageWorker.getType()) {
        
				case ANALYSIS:
         	CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
					break;
				case SEGMENTATION:
					CData.output = new CImageContainer(CData.getImage(CData.showImage[0]).getImage(), false);
					CData.output.setSegmentMap((CSegmentMap) imageWorker.get());
					break;
				case REGISTRATION:
          CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
       
      
          
					break;
				case CORRECTOR:
					CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
					break;
				case MORPH:
					CData.output = new CImageContainer(CData.getImage(CData.showImage[0]).getImage(), false);
					CData.output.setSegmentMap((CSegmentMap) imageWorker.get());
					break;
				case SUPPORT:
					
          if (imageWorker instanceof CRefPointMarker) {
						logger.info("Marks distribution here");
						CPointPairs pairs = (CPointPairs) imageWorker.get();

						CData.getImage(CData.showImage[0]).setMarks(pairs.getOrigins());
						CData.getImage(CData.showImage[2]).setMarks(pairs.getProjected());

						return;
					} else if (imageWorker instanceof CPointPairsOverview) {
						((CPointPairsOverview)imageWorker).get().setVisible(true);
						return;
					} else if (imageWorker instanceof CInterestingPoints) {
            
            
           //CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
            /*CPointPairs pt = (CPointPairs) imageWorker.get();
            logger.info("got pts");
            CData.getImage(CData.showImage[0]).setMarks(pt.getOrigins());
            logger.info("pts in A");
            CData.getImage(CData.showImage[2]).setMarks(pt.getProjected());
            logger.info("pts in B");
            return;
            * 
            */
            return;
          }
            else
          {
						CData.output = new CImageContainer((BufferedImage) imageWorker.get(), true);
					}
					break;
			}
			CData.output.setFilename(CData.getImage(CData.showImage[0]).getFilename());
			CData.showImage[1] = -2;
			loadOutput();
			refreshGUI();
		} catch (NullPointerException npe) {
			logger.log(Level.INFO, "NullPointerException: Bad input params for worker or worker bug.{0}", npe.getMessage());
		} catch (CancellationException ce) {
			logger.info("Operation cancelled");
		} catch (InterruptedException ie) {
			logger.info("Operation interrupted");
		} catch (ExecutionException ee) {
			logger.log(Level.INFO, "Other exception: {0}", ee.getMessage());
		}
		imageWorker = null;
	}

	public void fixImage() {
		if (CData.showImage[0] != -1 && CData.output != null) {
			CData.getImage(CData.showImage[0]).setImage(CData.output.getTransformedImage(CData.view));
		} else if (CData.output != null) {
			captureImage();
		} else {
			JOptionPane.showMessageDialog(new JFrame(), "There is no output image to fix.", "Warning", JOptionPane.WARNING_MESSAGE);
		}

		previewBar.refreshBar();
		refreshGUI();
	}

	public void captureImage() {
		if (CData.output != null) {
			CData.addImage(CData.output);
		}
		previewBar.refreshBar();
		refreshGUI();
	}

	public void setContentStructure(RegID regID) {
		content.setStructure(regID);
		refreshGUI();
	}
  
  public void runMultipleIntPoints() throws InterruptedException {
    logger.info("MIP started");
    BufferedImage imageA = null;
    BufferedImage imageAGreyscale = null;
    double harrisScore = 0.0;
    double COGScore = 0.0;
    //double prevCOGScore = 0.0;
    //double [] derCOGScore = new double[4];
    double randomScore = 0.0;
    //double distL = 0.0, perpCL = 0.0, whiteDiffL = 0.0, centerWhitenessL = 0.0;
    
    double learnrate = 0.3;
    int iters = 30;
    double derStep = 1.03;
    int setSize = CData.imagesSize()/2;
    int paramCount = 5;
    int [] windowSizes = new int[setSize];
    double lambda = 0.01;
    logger.info("easy allocation finished");
    
    double [] growth = new double[paramCount];
    CCOGParams [] p = new CCOGParams[paramCount+1];
    logger.info("params allocated");
    CInterestingPointsThread [] threadField = new CInterestingPointsThread[paramCount+1];
    logger.info("threads allocated");
    CInterestingPoints [] workerField = new CInterestingPoints[paramCount+1];
    logger.info("workers allocated");
    double [] scoreField = new double[paramCount+1];
    logger.info("scores allocated");
    //CCOGParams prevP = new CCOGParams();
    
    logger.info("calculating windowSizes of " + CData.imagesSize() + " images");
    for (int i = 0; i < CData.imagesSize(); i+=2) {
      if (CData.imagesSize() == i+1) break;
      if(!CData.input2Showed) show2ndInput();    
      CData.showImage[0] = i;
      imagePanel[0].setSizeByImage();
      CData.showImage[2] = i+1;
      imagePanel[2].setSizeByImage();
      
      imageA = CData.getImage(CData.showImage[0]).getImage();
      
      imageAGreyscale = (new Crgb2grey()).convert(imageA);
      
      windowSizes[i/2] = CBestWindowSize.getBestWindowSize(imageAGreyscale.getData(), 10000, 3, 25);
      logger.info("windowsize: " + windowSizes[i/2]);
    }
    
    for (int j = 0; j < paramCount+1; j++) {
      p[j] = new CCOGParams();
    }
    
    logger.info("finished calculating windowsizes");
    logger.info("starting workers");
    
    
    for (int iter = 0; iter < iters; iter++) {
      logger.info("started iter " + iter);
    
      p[1].centerWhitenessW *= derStep;
      p[1].normalizeWeights();
      p[2].distW *= derStep;
      p[2].normalizeWeights();
      p[3].perpCW *= derStep;
      p[3].normalizeWeights();
      p[4].whiteDiffW *= derStep;
      p[4].normalizeWeights();
      p[5].thresholdq *= derStep;
      p[5].normalizeWeights();
      
      
      
      for (int i = 0; i < CData.imagesSize(); i+=2) {
      
        if (CData.imagesSize() == i+1) break;
        if(!CData.input2Showed) show2ndInput();    
        CData.showImage[0] = i;
        imagePanel[0].setSizeByImage();
        CData.showImage[2] = i+1;
        imagePanel[2].setSizeByImage();
        for (int j = 0; j < paramCount+1; j++) {
          p[j].windowSize = windowSizes[i/2];          
        }
        
        for (int j = 0; j < paramCount+1; j++) {
          
          //logger.info("i. e. CW: " + p[j].centerWhitenessW + " dist: " + p[j].distW + " perpC: " + p[j].perpCW + "whiteDiff: " + p[j].whiteDiffW);
          workerField[j] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[j]);
          threadField[j] = new CInterestingPointsThread(workerField[j]);
          threadField[j].start();
          //logger.info("started thread " + j + " in iter " + iter + " doing image " + i/2);
        }
       /* 
        workerField[1] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[1]);
        threadField[1] = new CInterestingPointsThread(workerField[0]);
        threadField[1].start();
        
        workerField[2] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[2]);
        threadField[2] = new CInterestingPointsThread(workerField[0]);
        threadField[2].start();
        
        workerField[3] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[3]);
        threadField[3] = new CInterestingPointsThread(workerField[0]);
        threadField[3].start();

        workerField[4] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[4]);
        threadField[4] = new CInterestingPointsThread(workerField[0]);
        threadField[4].start();
        */
        
        //CW[i/2] = new CInterestingPoints(CData.getImage(CData.showImage[0]).getImage(), CData.getImage(CData.showImage[2]).getImage(), Cornerer.COG, Edger.sobel, p[i/2]);
        //CW[i/2].addPropertyChangeListener(progressBar);
        //CW[i/2].execute();
        //refreshGUI();
        
        for (int j = 0; j < paramCount+1; j++) {
          try {
            logger.info("waiting for thread " + j + " in iter " + iter + " on image " + i/2 );
            threadField[j].join();
          } catch(InterruptedException e) {
            logger.log(Level.SEVERE, "Thread " + j + " interrupted" );
          }
          scoreField[j] += (threadField[j].result)/(double)setSize;
            
        }
      }
        logger.info("base score: " + scoreField[0]);
        logger.info("base params: CW: " + p[0].centerWhitenessW + " dist: " + p[0].distW + " perpC: " + p[0].perpCW + " whiteDiff: " + p[0].whiteDiffW + " threshold: " + p[0].threshold);
        
        for (int j = 0; j < paramCount; j++) {
          growth[j] = (scoreField[j+1] - scoreField[0]) - lambda*p[j].getRegularization();
        }
        //growth[1] = scoreField[2] - scoreField[0];
        //growth[2] = scoreField[3] - scoreField[0];
        //growth[3] = scoreField[4] - scoreField[0];
        //growth[4] = scoreField[5] - scoreField[0];
        for (int j = 0; j < paramCount; j++) {
          p[j].centerWhitenessW += growth[0]*learnrate;
          p[j].distW += growth[1]*learnrate;
          p[j].perpCW += growth[2]*learnrate;
          p[j].whiteDiffW += growth[3]*learnrate;
          p[j].thresholdq += growth[4]*learnrate;
          p[j].normalizeWeights();
        }
      
        for (int j = 0; j < paramCount+1; j++) {
          workerField[j] = null;
          threadField[j] = null;
          scoreField[j] = 0.0;
        }
        logger.info("finished iter " + iter);
    }
    
    
      
      
     
      
    
      
      logger.info("Harris: " + harrisScore + " COG: " + COGScore + " random: " + randomScore);
    
    
  
  }
  
}
