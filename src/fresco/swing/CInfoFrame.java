/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CImageContainer;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JLabel;
import workers.segmentation.CSegmentMap;

/**
 * @author gimli
 * @version 21.6.2009
 */
public class CInfoFrame extends JFrame {

	public CInfoFrame(CImageContainer image) {
		int t = image.getImage().getType();
		String type, segmentInfo;

		switch (t) {
			case BufferedImage.TYPE_3BYTE_BGR:
				type = "3 Bytes for RGB";
				break;
			case BufferedImage.TYPE_4BYTE_ABGR:
				type = "4 Bytes for RGB and alpha chanel";
				break;
			case BufferedImage.TYPE_4BYTE_ABGR_PRE:
				type = "4 Bytes for RGB and alpha chanel";
				break;
			case BufferedImage.TYPE_BYTE_BINARY:
				type = "Black and white image, one bit for colour";
				break;
			case BufferedImage.TYPE_BYTE_GRAY:
				type = "Image in 1 Byte grayscale";
				break;
			case BufferedImage.TYPE_BYTE_INDEXED:
				type = "Indexed image";
				break;
			case BufferedImage.TYPE_INT_ARGB:
				type = "4 Integer for RGB and alpha chanel";
				break;
			case BufferedImage.TYPE_INT_ARGB_PRE:
				type = "4 Integer for RGB and alpha chanel";
				break;
			case BufferedImage.TYPE_INT_BGR:
				type = "3 Integers for RGB";
				break;
			case BufferedImage.TYPE_INT_RGB:
				type = "3 Integers for RGB";
				break;
			case BufferedImage.TYPE_USHORT_555_RGB:
				type = "5 bits for each from RGB";
				break;
			case BufferedImage.TYPE_USHORT_565_RGB:
				type = "5 bits for red and blue 6 bits for green";
				break;
			case BufferedImage.TYPE_USHORT_GRAY:
				type = "Grayscale in 1 short";
				break;
			default:
				type = "Unspecified type of image";
				break;
		}

		if (image.isSegmented()) {
			CSegmentMap map = image.getSegmentMap();
			segmentInfo = "<tr><th>Segmented:</th><td>YES</td></tr>"
					+ "<tr><th>Number of segments:</th><td>" + map.getNumSegments() + "</td></tr>";
		} else {
			segmentInfo = "<tr><th>Segmented:</th><td>NO</td>";
		}

		setIconImage(image.getImage());
		setLayout(new FlowLayout(10, 10, 1));
		add(new JLabel("<html><body><h3 align=\"center\">"
				+ image.getFilename() + "</h3>"
				+ "<small><table border=\"1\" cellpadding=\"3\">"
				+ "<tr><th>Image size:</th><td>" + image.getWidth() + " x " + image.getHeight() + "</td></tr>"
				+ "<tr><th>Image type:</th><td>" + type + "</td></tr>"
				+ segmentInfo
				+ "</table></small></body></html>"));
		//add(new Label();
		addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				((JFrame) e.getSource()).setVisible(false);
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
		pack();
	}
}
