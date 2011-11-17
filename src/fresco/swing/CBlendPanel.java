/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import java.awt.BorderLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author gimli
 */
public class CBlendPanel extends JPanel {

	JSlider blender = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	int value = 0;
	CBlendComponent blendComponent;
	static final Logger logger = Logger.getLogger(CBlendPanel.class.getName());
	ChangeListener blenderListener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (value != blender.getValue() && !((JSlider) e.getSource()).getValueIsAdjusting()) {
				logger.log(Level.INFO, "New blend value: {0}%", blender.getValue());
				value = blender.getValue();
				blendComponent.setAlpha(value);
				blendComponent.setSizeByImage();
			}
		}
	};

	public CBlendPanel(JScrollPane imageWindow, CBlendComponent blendComponent) {
		setLayout(new BorderLayout());

		this.blendComponent = blendComponent;

		if (imageWindow == null) {
			imageWindow = new JScrollPane(blendComponent);
		} else {
			imageWindow.setViewportView(blendComponent);
		}
		add(imageWindow, BorderLayout.CENTER);

		blender.addChangeListener(blenderListener);
		blender.setValue(50);
		add(blender, BorderLayout.SOUTH);
	}
}
