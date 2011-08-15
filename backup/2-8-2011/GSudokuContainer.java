import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GSudokuContainer implements ActionListener, FocusListener
{
	private GSudokuBoard sudoku;
	private JFrame frame;
	private JPanel buttons, status;
	private JLabel minLabel, sizeLabel, statusLabel;
	private JButton solve, clean, reset;
	private JTextField minField, sizeField;
	private Timer timer;
	
	private long steps;
	
	GSudokuContainer() {
		timer = new Timer(20, this);
		frame = new JFrame();
		frame.setTitle("Sudoku Solver");
		frame.setSize(new Dimension(550, 600));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
		
		// NORTH
		status = new JPanel(new FlowLayout());
		
		statusLabel = new JLabel(" ");
		statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
		status.add(statusLabel);
		
		frame.add(status, BorderLayout.NORTH);
		// END NORTH
		
		// CENTER
		sudoku = new GSudokuBoard(this);
		frame.add(sudoku, BorderLayout.CENTER);
		frame.addMouseListener(sudoku);
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
		
		timer.start();
		
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
	
	public void setIconImage(Image img) {
		frame.setIconImage(img);
	}
	
	public void setStepCount(long num) {
		steps = num;
	}
	
	public void setDone() {
		statusLabel.setText(steps + " steps taken in total.");
		solve.setEnabled(false);
		solve.setText("Solve it!");
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			if (sudoku.isWorking())
				statusLabel.setText(String.format
									("WORKING: %,d steps taken so far.", steps));
		}
		else if (e.getSource() == minField) changeMinValue();
		else if (e.getSource() == sizeField) changeBoardSize();
		else if (e.getSource() == solve) {
			if (e.getActionCommand() == "Solve it!") {
				if (!sudoku.solve())
					JOptionPane.showMessageDialog(frame,
							"Impossible board configuration!",
							"Oops!",
							JOptionPane.ERROR_MESSAGE);
				else solve.setText("STOP");
			}
			else if (e.getActionCommand() == "STOP") {
				sudoku.cancel();
			}
		}
		else if (e.getSource() == clean) {
			sudoku.clean();
			setClean();
		}
		else if (e.getSource() == reset) {
			sudoku.reset();
			setClean();
		}
	}
	public void focusGained(FocusEvent e) { }
	public void focusLost(FocusEvent e) {
		if (e.getSource() == minField) changeMinValue();
		else if (e.getSource() == sizeField) changeBoardSize();
	}
	
	private void setClean() {
		statusLabel.setText(" ");
		solve.setEnabled(true);
	}
	
	private void changeBoardSize() {
		if (sizeField.getText().isEmpty()) {
			sizeField.setText(sudoku.getBoardSize() + "");
			return;
		}

		int size = Integer.parseInt(sizeField.getText());
		
		int code = sudoku.setSize(size);
		
		if (code == GSudokuBoard.ERR_INVALID_SIZE) {
			sizeField.setText(sudoku.getBoardSize() + "");
			JOptionPane.showMessageDialog(frame,
					"Cannot create a Sudoku board of this size!\n"
				  + "Please use only perfect squares.",
					"Irregular Board",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		else if (code == GSudokuBoard.ERR_OUT_OF_RANGE) {
			sizeField.setText(sudoku.getSize() + "");
			return;
		}
		else if (code == GSudokuBoard.ERR_NO_SYMBOLS) {
			sizeField.setText(sudoku.getSize() + "");
			JOptionPane.showMessageDialog(frame,
				"There are only 36 symbols available combining all\n"
			  + "numerals and all English letters. I could allow you\n"
			  + "to also use greek letters, but I doubt your computer\n"
			  + "would stand it anyway.",
				"Maximum Symbols Reached",
				JOptionPane.ERROR_MESSAGE);
			return;
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
		
		int code = sudoku.setMin(min);
		
		if (code == GSudokuBoard.ERR_NO_SYMBOLS) {
			minField.setText(sudoku.getMinValue() + "");
			JOptionPane.showMessageDialog(frame,
				"There are only 36 symbols available combining all\n"
			  + "numerals and all English letters. I could allow you\n"
			  + "to also use greek letters, but I doubt your computer\n"
			  + "would stand it anyway.",
				"Maximum Symbols Reached",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
}