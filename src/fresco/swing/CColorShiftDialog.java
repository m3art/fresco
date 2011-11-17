/*
 * Part of Fresco software under GPL licence
 * http://www.gnu.org/licenses/gpl-3.0.txt
 */
package fresco.swing;

import fresco.CData;
import info.clearthought.layout.TableLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import workers.correction.MeanAndVar;

/**
 *
 * @author gimli
 */
public class CColorShiftDialog extends JDialog {

	private MeanAndVar params;
	private final static Logger logger = Logger.getLogger(CColorShiftDialog.class.getName());

	public CColorShiftDialog() {
		super(CData.mainFrame, "Shifting colours", true);
		initComponents();
	}

	private void initComponents() {

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JCheckBox use2ndInput = new JCheckBox("Use second input as pattern");
		if (CData.showImage[2] == -1) {
			use2ndInput.setSelected(true);
		} else {
			use2ndInput.setEnabled(false);
		}



		JLabel targetLabel = new JLabel("Target mean value (range 0-255):");

		JLabel red = new JLabel("Red colour: ");
		final JTextField redValue = new JTextField("128.0", 5);
		redValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel redVar = new JLabel("Red colour variance: ");
		final JTextField redVarValue = new JTextField("2048.0", 8);
		redVarValue.setHorizontalAlignment(JTextField.RIGHT);

		JLabel green = new JLabel("Green colour: ");
		final JTextField greenValue = new JTextField("128.0", 5);
		greenValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel greenVar = new JLabel("Green colour: ");
		final JTextField greenVarValue = new JTextField("2048.0", 8);
		greenVarValue.setHorizontalAlignment(JTextField.RIGHT);

		JLabel blue = new JLabel("Green colour mean: ");
		final JTextField blueValue = new JTextField("128.0", 5);
		blueValue.setHorizontalAlignment(JTextField.RIGHT);
		JLabel blueVar = new JLabel("Green colour variance: ");
		final JTextField blueVarValue = new JTextField("2048.0", 8);
		blueVarValue.setHorizontalAlignment(JTextField.RIGHT);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					params = new MeanAndVar();

					params.mean[0] = Double.valueOf(redValue.getText());
					params.mean[1] = Double.valueOf(greenValue.getText());
					params.mean[2] = Double.valueOf(blueValue.getText());
					params.var[0] = Double.valueOf(redVarValue.getText());
					params.var[1] = Double.valueOf(greenVarValue.getText());
					params.var[2] = Double.valueOf(blueVarValue.getText());
				} catch (NumberFormatException nfe) {
					logger.warning("Format of numbers not supported");
				} finally {
					setVisible(false);
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		this.setLayout(new TableLayout(new double[]{0.6, 0.4}, new double[]{TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL, TableLayout.FILL}));

		add(use2ndInput, "0, 0, 1, 0");
		add(targetLabel, "0, 1, 1, 1");
		add(red, "0, 2");
		add(redValue, "1, 2");
		add(redVar, "0, 3");
		add(redVarValue, "1, 3");
		add(green, "0, 4");
		add(greenValue, "1, 4");
		add(greenVar, "0, 5");
		add(greenVarValue, "1, 5");
		add(blue, "0, 6");
		add(blueValue, "1, 6");
		add(blueVar, "0, 7");
		add(blueVarValue, "1, 7");
		add(okButton, "0, 8");
		add(cancelButton, "1, 8");

		pack();
	}

	public MeanAndVar get() {
		return params;
	}
}
