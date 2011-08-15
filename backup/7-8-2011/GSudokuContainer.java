import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

public class GSudokuContainer implements ActionListener, FocusListener, ItemListener
{
	private GSudokuBoard sudoku;
	
	private JFrame frame;
	
	private JMenu fileMenu, optionsMenu;
	private JMenuBar menuBar;
	private JMenuItem openFile, saveFile;
	private JCheckBoxMenuItem slowMotion;
	private JLabel minLabel, sizeLabel, statusLabel;
	private JButton solve, clean, reset;
	private JTextField minField, sizeField;
	private JFileChooser fileChooser;
	
	GSudokuContainer() {
		// MAIN FRAME
		frame = new JFrame();
		frame.setTitle("Sudoku Solver");
		frame.setSize(new Dimension(550, 620));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		// END MAIN FRAME
		
		// FILE CHOOSER
		fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String ext;
				int dotIndex = f.getName().lastIndexOf('.');
				if (dotIndex > 0 && dotIndex < f.getName().length() - 1) {
					ext = f.getName().substring(dotIndex, f.getName().length());
					if (ext.toLowerCase().equals(".txt")) return true;
				}
				
				return false;
			}
			
			public String getDescription() {
				return "Simple .txt files";
			}
		});
		// END FILE CHOOSER
		
		// MENU
		menuBar = new JMenuBar();
		
		// File
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription(
				"Access file options.");
		menuBar.add(fileMenu);
		
		openFile = new JMenuItem("Open Board");
		openFile.setMnemonic(KeyEvent.VK_O);
		openFile.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_O, ActionEvent.ALT_MASK));
		openFile.getAccessibleContext().setAccessibleDescription(
				"Opens the current board configuration in a file.");
		openFile.addActionListener(this);
		fileMenu.add(openFile);
		
		saveFile = new JMenuItem("Save Board");
		saveFile.setMnemonic(KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription(
				"Saves the current board configuration in a file.");
		saveFile.addActionListener(this);
		fileMenu.add(saveFile);
		
		fileMenu.addSeparator();
		
		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic(KeyEvent.VK_E);
		exit.getAccessibleContext().setAccessibleDescription(
				"Exits the program.");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{ System.exit(0); }
		});
		fileMenu.add(exit);
		
		// End file
		menuBar.add(fileMenu);
		
		// Options
		optionsMenu = new JMenu("Options");
		optionsMenu.setMnemonic(KeyEvent.VK_P);
		optionsMenu.getAccessibleContext().setAccessibleDescription(
				"Access program controls.");
		
		slowMotion = new JCheckBoxMenuItem("Slow Motion");
		slowMotion.setMnemonic(KeyEvent.VK_S);
		slowMotion.addItemListener(this);
		optionsMenu.add(slowMotion);
		
		// End options
		menuBar.add(optionsMenu);
		
		frame.setJMenuBar(menuBar);
		// END MENU
		
		// NORTH
		JPanel status = new JPanel(new FlowLayout());
		
		statusLabel = new JLabel("Ready.");
		statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
		status.add(statusLabel);
		
		frame.add(status, BorderLayout.NORTH);
		// END NORTH
		
		// CENTER
		sudoku = new GSudokuBoard(this);
		frame.add(sudoku, BorderLayout.CENTER);
		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				sudoku.updateSize(frame.getSize());
			}
		});
		// END CENTER
		
		// SOUTH
		JPanel buttons = new JPanel(new FlowLayout());
		
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
		
		minField = new JLimitedTextField(1,
				JLimitedTextField.ALL).createField();
		minField.setText("1");
		minField.addActionListener(this);
		minField.addFocusListener(this);
		buttons.add(minField);
		
		sizeLabel = new JLabel("Board size:");
		buttons.add(sizeLabel);
		
		sizeField = new JLimitedTextField(2,
				JLimitedTextField.NUM).createField();
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
	
	public void setStatus(String statusString) {
		statusLabel.setText(statusString);
	}
	
	public void setClean() {
		solve.setEnabled(true);
		openFile.setEnabled(true);
		saveFile.setEnabled(true);
		slowMotion.setEnabled(true);
	}
	
	public boolean isClean() {
		return (solve.isEnabled() && solve.getText() == "Solve it!");
	}
	
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
					openFile.setEnabled(false);
					saveFile.setEnabled(false);
					slowMotion.setEnabled(false);
				}
			}
			else if (e.getActionCommand() == "STOP") sudoku.cancel();
		}
		else if (e.getSource() == clean) { sudoku.clean(); }
		else if (e.getSource() == reset) { sudoku.reset(); }
		else if (e.getSource() == openFile) {
			int option = fileChooser.showOpenDialog(frame);
			
			if (option == JFileChooser.APPROVE_OPTION) {
				int val = sudoku.readFile(fileChooser.getSelectedFile());
				if (val == -1)
					JOptionPane.showMessageDialog(frame,
							"Could not read \""
							+ fileChooser.getSelectedFile().getName() + "\".",
							"Error!",
							JOptionPane.ERROR_MESSAGE);
				else if (val > 0)
					JOptionPane.showMessageDialog(frame,
							"Invalid line length: " + val + '.',
							"Error!",
							JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (e.getSource() == saveFile) {
			int val = fileChooser.showSaveDialog(frame);
			
			if (val == JFileChooser.APPROVE_OPTION) {
				File f = fileChooser.getSelectedFile();
				
				String ext, newName = null;
				int dotIndex = f.getName().lastIndexOf('.');
				if (dotIndex > 0) {
					ext = f.getName().substring(dotIndex, f.getName().length());
					if (ext.toLowerCase().equals(".txt"))
						newName = f.getAbsolutePath();
					else newName = f.getAbsolutePath().substring(0,
							dotIndex) + ".txt";
				}
				else newName = f.getAbsolutePath() + ".txt";
				
				f = new File(newName);
				
				if (sudoku.writeFile(new File(newName)))
					 JOptionPane.showMessageDialog(frame,
						"Saved board as \"" + f.getName() + "\".",
						"Success!",
						JOptionPane.INFORMATION_MESSAGE);
				else JOptionPane.showMessageDialog(frame,
						"Could not write \"" + f.getName() + "\".",
						"Error!",
						JOptionPane.ERROR_MESSAGE);
			}
		}
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
		
		Codes code = sudoku.setBoardSize(size);
		
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
		
		Codes code = sudoku.setMinValue(min);
		
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