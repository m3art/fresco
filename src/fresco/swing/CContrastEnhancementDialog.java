/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import workers.correction.CAHEParams;

/**
 * @author gimli
 * @version Aug 19, 2011
 */
public class CContrastEnhancementDialog extends JDialog {

	JButton okButton, cancelButton;
	JLabel regSizeLabel, effectLabel;
	JTextField regSizeInput;
	JSlider effectSlider;
	// FIXME: add this selectors
	JCheckBox red, green, blue;
	CAHEParams params;
	private static final Logger logger = Logger.getLogger(CContrastEnhancementDialog.class.getName());

	/** Creates new form CContrastEnhancementDialog */
	public CContrastEnhancementDialog(java.awt.Frame parent) {
		super(parent, true);
		initComponents();
	}

	private void cancelButtonActionPerformed(ActionEvent evt) {
		setVisible(false);
	}

	private void okButtonActionPerformed(ActionEvent evt) {
		params = new CAHEParams();
		params.effect = effectSlider.getValue();
		params.region_size = Integer.valueOf(regSizeInput.getText());

		setVisible(false);
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {

		setLayout(new GridBagLayout());

		GridBagConstraints constr = new GridBagConstraints();

		setTitle("Contrast enhancement - params settings:");

		regSizeLabel = new JLabel("Region size (smaller higher effect):", SwingConstants.RIGHT);
		constr.insets = new Insets(0, 10, 0, 0);
		constr.gridx = 0;
		constr.gridy = 0;
		//constr.gridwidth = 3;
		constr.ipadx = 2;
		constr.ipady = 2;
		add(regSizeLabel, constr);

		regSizeInput = new JTextField("200");
		regSizeInput.setToolTipText("value is in pixels");
		constr.insets = new Insets(0, 0, 0, 10);
		constr.gridx = 1;
		constr.gridwidth = 2;
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.anchor = GridBagConstraints.LINE_START;
		add(regSizeInput, constr);


		effectLabel = new JLabel("Effect:", SwingConstants.RIGHT);
		constr.fill = GridBagConstraints.NONE;
		constr.gridy = 1;
		constr.insets = new Insets(0, 10, 0, 0);
		constr.gridx = 0;
		constr.gridwidth = 1;
		constr.anchor = GridBagConstraints.LINE_END;
		this.add(effectLabel, constr);

		effectSlider = new JSlider();
		constr.gridy = 1;
		constr.gridx = 1;
		constr.gridwidth = 2;
		constr.insets = new Insets(0, 10, 0, 10);
		constr.anchor = GridBagConstraints.LINE_START;
		this.add(effectSlider, constr);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		okButton = new JButton();
		okButton.setText("OK");
		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});

		cancelButton = new JButton();
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		constr.gridwidth = 1;
		constr.anchor = GridBagConstraints.CENTER;
		constr.gridy = 2;
		constr.gridx = 1;
		this.add(okButton, constr);
		constr.gridx = 2;
		this.add(cancelButton, constr);

		pack();
	}

	public CAHEParams get() {
		return params;
	}
}
