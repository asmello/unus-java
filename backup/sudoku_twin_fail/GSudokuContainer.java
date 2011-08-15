import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

public class GSudokuContainer implements ActionListener, FocusListener, ItemListener
{
	private GSudokuBoard sudoku;
	private JFrame frame;
	private JPanel buttons, status;
	private JLabel minLabel, sizeLabel, statusLabel;
	private JButton solve, clean, reset;
	private JToggleButton slowMotion;
	private JTextField minField, sizeField;
	
	GSudokuContainer() {
		frame = new JFrame();
		frame.setTitle("Sudoku Solver");
		frame.setSize(new Dimension(550, 600));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
		
		// NORTH
		
		status = new JPanel(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridheight = 1;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(3, 425, 0, 0);
		
		slowMotion = new JToggleButton("Slow");
		slowMotion.addItemListener(this);
		status.add(slowMotion, c);
		
		c.insets = new Insets(0,0,0,0);
		c.gridx = 1;
		
		statusLabel = new JLabel("Ready.");
		statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
		status.add(statusLabel, c);
		
		frame.add(status, BorderLayout.NORTH);
		// END NORTH
		
		// CENTER
		sudoku = new GSudokuBoard(this);
		frame.add(sudoku, BorderLayout.CENTER);
		// END CENTER
		
		// SOUTH
		buttons = new JPanel(new FlowLayout());
		
		solve = new JButton("Solve it!");
		solve.addActionListener(this);
		buttons.add(solve);
		
		clean = new JButton("Clean Sol.");
		clean.addActionListener(this);
		buttons.add(clean);
		
		reset = new JButton("Reset");
		reset.addActionListener(this);
		buttons.add(reset);
		
		minLabel = new JLabel("Min. value:");
		buttons.add(minLabel);
		
		minField = new JLimitedTextField(1, JLimitedTextField.ALL).createField();
		minField.setText("1");
		minField.addActionListener(this);
		minField.addFocusListener(this);
		buttons.add(minField);
		
		sizeLabel = new JLabel("Board size:");
		buttons.add(sizeLabel);
		
		sizeField = new JLimitedTextField(2, JLimitedTextField.NUM).createField();
		sizeField.setText("9");
		sizeField.addActionListener(this);
		sizeField.addFocusListener(this);
		buttons.add(sizeField);
		
		frame.add(buttons, BorderLayout.SOUTH);
		// END SOUTH
		
		frame.setVisible(true);
	}
	
	public ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) return new ImageIcon(imgURL);
		else {
			System.err.println("Couldn't find file: " + path);
			System.exit(1);
		}
		return null;
	}
	
	public void setIconImage(Image img) { frame.setIconImage(img); }
	
	public void setDone() {
		solve.setEnabled(false);
		solve.setText("Solve it!");
	}
	
	public void setStatus(String statusString)
		{ statusLabel.setText(statusString); }
	
	public void setClean() {
		solve.setEnabled(true);
		slowMotion.setEnabled(true);
	}
	
	public boolean isClean()
		{ return (solve.isEnabled() && solve.getText() == "Solve it!"); }
	
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == slowMotion)
			sudoku.setSlowMotion(e.getStateChange() == ItemEvent.SELECTED);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == minField) changeMinValue();
		else if (e.getSource() == sizeField) changeBoardSize();
		else if (e.getSource() == solve) {
			if (e.getActionCommand() == "Solve it!") {
				if (!sudoku.solve())
					JOptionPane.showMessageDialog(frame,
							"Impossible board configuration!",
							"Oops!",
							JOptionPane.ERROR_MESSAGE);
				else {
					solve.setText("STOP");
					slowMotion.setEnabled(false);
				}
			}
			else if (e.getActionCommand() == "STOP") sudoku.cancel();
		}
		else if (e.getSource() == clean) { sudoku.clean(); }
		else if (e.getSource() == reset) { sudoku.reset(); }
	}
	
	public void focusGained(FocusEvent e) { }
	public void focusLost(FocusEvent e) {
		if (e.getSource() == minField) changeMinValue();
		else if (e.getSource() == sizeField) changeBoardSize();
	}
	
	private void changeBoardSize() {
		if (sizeField.getText().isEmpty()) {
			sizeField.setText(sudoku.getBoardSize() + "");
			return;
		}

		int size = Integer.parseInt(sizeField.getText());
		
		Codes code = sudoku.setSize(size);
		
		switch (code) {
		case ERR_INVALID_SIZE:
			sizeField.setText(sudoku.getBoardSize() + "");
			JOptionPane.showMessageDialog(frame,
					"Cannot create a Sudoku board of this size!\n"
				  + "Please use only perfect squares.",
					"Irregular Board",
					JOptionPane.WARNING_MESSAGE);
			break;
		case ERR_OUT_OF_RANGE:
			sizeField.setText(sudoku.getSize() + "");
			break;
		case ERR_NO_SYMBOLS:
			sizeField.setText(sudoku.getSize() + "");
			JOptionPane.showMessageDialog(frame,
				"There are only 36 symbols available combining all\n"
			  + "numerals and all English letters. I could allow you\n"
			  + "to also use greek letters, but I doubt your computer\n"
			  + "would stand it anyway.",
				"Maximum Symbols Reached",
				JOptionPane.ERROR_MESSAGE);
			break;
		}
	}
	
	private void changeMinValue() {
		if (minField.getText().isEmpty()) {
			minField.setText(sudoku.getMinValue() + "");
			return;
		}
		
		int min = JLimitedTextField.getValue(minField.getText().charAt(0),
											JLimitedTextField.MIN,
											JLimitedTextField.MAX);
		
		Codes code = sudoku.setMin(min);
		
		if (code == Codes.ERR_NO_SYMBOLS) {
			minField.setText(sudoku.getMinValue() + "");
			JOptionPane.showMessageDialog(frame,
				"There are only 36 symbols available combining all\n"
			  + "numerals and all English letters. I could allow you\n"
			  + "to also use greek letters, but I doubt your computer\n"
			  + "would stand it anyway.",
				"Maximum Symbols Reached",
				JOptionPane.ERROR_MESSAGE);
		}
	}
}