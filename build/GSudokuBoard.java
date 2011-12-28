/** Copyright 2011 André Sá de Mello
 *  This file is part of Unus.
 *  
 *  Unus is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Unus is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Unus.  If not, see <http://www.gnu.org/licenses/>.
 */

/* TODO:
 * -- Improve algorithm
 *    * 2-3 trees to store row, column and subsquares values
 *    * Add other logical deduction solving algorithms
 *    
 * -- Irregular boards
 * -- Lock feature
 *    > Allow use as normal sudoku board
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.PrintWriter;

import java.util.Scanner;

import javax.swing.Timer;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import javax.swing.event.MouseInputAdapter;

/**
 * This class deals with the entire computation of the actual virtual Sudoku board.
 */
public class GSudokuBoard extends JPanel {
	// CLASS CONSTANTS
	private static final long serialVersionUID = 1760927559618887130L;
	public static final int NULL = -1;
	
	// INSTANCE VARIABLES
	private int min = 1;
	private int subSize = 3;
	private int boardSize = 9;
	private int boardWidth;
	private int xMargin;
	private int boardHeight;
	private int selX = NULL;
	private int selY = NULL;
	private int wPosX = NULL;
	private int wPosY = NULL;
	private boolean slowMotion;
	private boolean showPValues;
	
	// SUPPORT CHILD OBJECTS
	private int[][] board;
	private boolean[][] fixPos;
	private boolean[][] defPos;
	private boolean[][] confPos;
	private CIntQueue[][] pValues;
	
	// UTILITY REFERENCES
	private Timer timer;
	private Font bigNumStyle;
	private Font smallNumStyle;
	private Codes stage = Codes.NULL;
	private Codes state = Codes.STATE_IDLE;
	private GSudokuWorker worker;
	private GSudokuContainer container;
	
	// Getters and Setters
	public int getBoardSize() { return boardSize; }
	public int getSubSize() { return subSize; }
	public int getMinValue() { return min; }
	public void setSlowMotion (boolean state) { slowMotion = state; }
	
	GSudokuBoard(GSudokuContainer con) {
		container = con; // Store the Main Window reference
		
		// Control repaint rate
		timer = new Timer(20, new ActionListener()
			{ public void actionPerformed(ActionEvent e) { repaint(); } });
		timer.start();
		
		// Handle keyboard events
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					if (selX > 0) selX--;
					break;
				case KeyEvent.VK_RIGHT:
					if (selX < boardSize - 1) selX++;
					break;
				case KeyEvent.VK_UP:
					if (selY > 0) selY--;
					break;
				case KeyEvent.VK_DOWN:
					if (selY < boardSize - 1) selY++;
					break;
				case KeyEvent.VK_BACK_SPACE:
				case KeyEvent.VK_DELETE:
					if (container.isClean()) {
						insert(selX, selY, NULL, false);
						fixPos[selY][selX] = false;
						setConflicts(selX, selY);
					} break;
				default:
					if (container.isClean()) {
						int value =  JLimitedTextField.getValue
								(e.getKeyChar(), min, boardSize + min);
						if (value != NULL) {
							insert(selX, selY, value, true);
							fixPos[selY][selX] = true;
							setConflicts(selX, selY);
						}
					}
				}
			}
		});
		
		// Handle mouse events
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				selX = (e.getX() - xMargin) * boardSize / boardWidth;
				selY = e.getY() * boardSize / (boardHeight - 3);
				requestFocus();
			}
		});
		
		// Set data and draw
		setFocusable(true);
		setBoard();
	}
	
	/**
	 * Calculates available board size for drawing, based on parent window info.
	 * Updates internal data for immediate use upon repaint.
	 */
	public void updateSize() {
		boardWidth = (int) (container.getAvailableWidth() * 0.9);
		xMargin = (container.getAvailableWidth() - boardWidth)/2;
		boardHeight = container.getAvailableHeight();
		bigNumStyle = new Font(Font.MONOSPACED, Font.BOLD,
				(int) (boardHeight * 0.936 / boardSize));
		smallNumStyle = new Font(Font.MONOSPACED, Font.PLAIN,
				(int) (boardHeight * 0.288 / boardSize));
	}
	
	/**
	 * Defines the total number of spaces in the board. Should be a square number.
	 * @param size the number of spaces
	 * @return a code response for success or failure
	 */
	public Codes setBoardSize(int size) {
		if (boardSize == size) 
			return Codes.NO_TASK;
		if (min + size > JLimitedTextField.MAX)
			return Codes.ERR_NO_SYMBOLS;
		if (size <= JLimitedTextField.MIN || !isSquare(size))
			return Codes.ERR_INVALID_SIZE;
		boardSize = size;
		subSize = (int) Math.sqrt(size);
		setBoard();
		container.setClean();
		return Codes.SUCCESS;
	}
	
	/**
	 * Defines the minimum value valid for placement on the board.
	 * @param min the minimum value allowable
	 * @return a code response for success or failure
	 */
	public Codes setMinValue(int min) {
		if (this.min == min)
			return Codes.NO_TASK;
		if (min + boardSize > JLimitedTextField.MAX)
			return Codes.ERR_NO_SYMBOLS;
		if (min < JLimitedTextField.MIN)
			return Codes.ERR_OUT_OF_RANGE;
		
		// Handles conversion of existing placements on the board
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				if (board[y][x] != NULL)
					board[y][x] += min - this.min;
				CIntQueue newQueue = new CIntQueue();
				int next = pValues[y][x].getNext();
				while (next != NULL) {
					newQueue.add(next + min - this.min);
					next = pValues[y][x].getNext();
				}
				pValues[y][x] = newQueue;
			}
		
		this.min = min;
		return Codes.SUCCESS;
	}
	
	/**
	 * Returns a string representing current board processing status.
	 * @return a string representing the current board status
	 */
	public String getStatusString() {
		switch (state) {
		case STATE_IDLE:
			return "Ready.";
		case STATE_CONFLICT:
			return "Conflict detected!";
		case STATE_WORKING:
			switch (stage) {
			case STAGE_LOGICAL_DEDUCTION:
				return "Working... Stage 1: Logical Deduction";
			case STAGE_TRIAL_AND_ERROR:
				return "Working... Stage 2: Trial and Error";
			default:
				return "UNKNOWN STAGE: " + stage.name();
			}
		case STATE_SUCCESS:
			return "Found a solution!";
		case STATE_CANCELLED:
			return "Stopped.";
		case STATE_FAILED:
			return "No solution found.";
		default:
			return "UNKNOWN STATE: " + state.name();
		}
	}
	
	// Override for paintComponent(). Handles the drawing of the board.
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		boolean conflictsFound = false;
		
		// Iterate over every space in the board
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				// Calculate current space position
				int posX = x * boardWidth/boardSize + xMargin;
				int posY = y * (boardHeight-3)/boardSize + 2;
				
				// Color-code the current space background
				if (x == selX && y == selY)
					g.setColor(Color.YELLOW); // Space currently selected
				else if (x == wPosX && y == wPosY)
					g.setColor(new Color(210, 210, 210)); // Working on space
				else g.setColor(Color.WHITE); // Default background
				
				g.fillRect(posX, posY, boardWidth/boardSize, boardHeight/boardSize);
				
				// Set the font for drawing current space's value
				g.setFont(bigNumStyle);
				// Check for conflicting value
				if (confPos[y][x]) {
					g.setColor(Color.RED); // Highlight conflicting value
					conflictsFound = true;
				}
				// Check if value was input by user (fixed value)
				else if (fixPos[y][x]) g.setColor(Color.BLACK);
				// Default (value assigned by the computer)
				else g.setColor(new Color(140,140,140)); 
				
				// Draw current space value
				g.drawString(JLimitedTextField.getChar(board[y][x]) + "",
							posX + (int)(boardWidth * 0.5 / boardSize
									- smallNumStyle.getSize()),
							posY + (int)(boardHeight*0.78/boardSize));
				
				// Handle solving-time drawing aspects (small numbers/possible values)
				if (board[y][x] == NULL && showPValues) {
					g.setFont(smallNumStyle);
					// Draw every possible value for current space
					for (int i = min; i < min + boardSize; ++i) {
						int next = pValues[y][x].getNext();
						while (next != NULL) {
							// All values in the space list are possible values
							if (i == next)
								// Draw value on appropriate location
								g.drawString(JLimitedTextField.getChar(i) + "",
									posX + ((i-min)%subSize) * boardWidth /
									(boardSize * subSize)
									+ (int)(boardWidth*0.5) / (boardSize*subSize)
									- smallNumStyle.getSize()/subSize,
									posY + ((i-min)/subSize) * boardHeight / 
									(boardSize * subSize)
									+ (int)(boardHeight*0.78)/(boardSize*subSize));
							next = pValues[y][x].getNext();
						}
					}
					pValues[y][x].resetIterator();
				}
			}
		
		// Draw board division lines
		g.setColor(Color.LIGHT_GRAY);
		for (int i = 1; i < boardSize; ++i) {
			g.fillRect(i*boardWidth/boardSize + xMargin - 1, 2, 2, boardHeight);
			g.fillRect(xMargin - 1, i * (boardHeight-3)/boardSize, boardWidth-1, 2);
		}
		g.setColor(Color.BLACK);
		for (int i = 0; i <= boardSize; i += subSize) {
			g.fillRect(xMargin - 2, i * (boardHeight-3) / boardSize, boardWidth, 3);
			g.fillRect(i*boardWidth/boardSize + xMargin - 2, 0, 3, boardHeight);
		}
		
		// Set state to conflicting (prevent solving from starting)
		if (conflictsFound) setState(Codes.STATE_CONFLICT);
		else if (state == Codes.STATE_CONFLICT) setState(Codes.STATE_IDLE);
	}
	
	/**
	 * Cancel solving thread.
	 */
	public void cancel() { worker.cancel(true); }
	/**
	 * Reset board (clears all values).
	 */
	public void reset() {
		if (worker != null) worker.cancel(true);
		setBoard();
		setState(Codes.STATE_IDLE);
		container.setClean();
	}
	/**
	 * Clean computer generated values.
	 */
	public void clean() {
		if (worker != null) worker.cancel(true);
		
		defPos = new boolean[boardSize][boardSize];
		confPos = new boolean[boardSize][boardSize];
		
		// Iterate over board spaces
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				defPos[y][x] = fixPos[y][x]; // Only user input values are definitive.
				if (!defPos[y][x]) board[y][x] = NULL; // Value is computer-generated.
				resetQueue(x, y); // Make sure list is ready for reading/writing
			}
		
		// Iterate over board spaces (to update conflict information)
		for (int y = 0; y < boardSize; ++y) 
			for (int x = 0; x < boardSize; ++x)
				if (board[y][x] != NULL) setConflicts(x, y);
		
		setState(Codes.STATE_IDLE);
		container.setClean();
	}
	
	/**
	 * Start solving thread.
	 * @return success (true) / failure (false)
	 */
	public boolean solve() {
		// Make sure there are no conflicts (board is solvable)
		if (state == Codes.STATE_CONFLICT) return false;
		worker = new GSudokuWorker();
		// Allow thread to update board status (solving stage)
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				container.setStatus(getStatusString());
			}
		});
		worker.execute();
		return true;
	}
	
	/**
	 * Load board information from file.
	 * @param f file to read from
	 * @return a code response for success or failure
	 */
	public Codes readFile (File f) {
		// Create scanner and check if file is readable
		Scanner input;
		try { input = new Scanner(f); }
		catch (Exception e) { return Codes.ERR_READ; }
		
		// Temporary placeholders for read data
		int newMin, newSize;
		int[][] newValues;
		
		// Parse metadata
		if (input.hasNextLine()) {
			String firstLine = input.nextLine();
			// Detect malformed header
			if (firstLine.length() != 13
				|| firstLine.charAt(5) != ','
				|| !firstLine.substring(0, 4).toUpperCase().equals("MIN:")
				|| !firstLine.substring(6, 11).toUpperCase().equals("SIZE:"))
				return Codes.ERR_PARSING;
			// Parse minimum valid value
			newMin = JLimitedTextField.getValue(firstLine.charAt(4),
												JLimitedTextField.MIN,
												JLimitedTextField.MAX);
			if (newMin == NULL) return Codes.ERR_PARSING;
			// Parse board size
			try { newSize = Integer.parseInt(firstLine.substring(11, 13)); }
			catch (Exception e) { return Codes.ERR_PARSING; }
			// Check if symbol range is enough for all possible values
			if (newMin + newSize > JLimitedTextField.MAX)
				return Codes.ERR_NO_SYMBOLS;
			// Check for invalid board size
			if (newSize <= JLimitedTextField.MIN || !isSquare(newSize))
				return Codes.ERR_INVALID_SIZE;
			newValues = new int[newSize][newSize];
		} else return Codes.ERR_PARSING;
		
		// Parse actual board data
		String line = null;
		boolean nowPadding = false;
		for (int y = 0; y < newSize; ++y) {
			if (!nowPadding && input.hasNextLine()) {
				line = input.nextLine();
				// Handle non-square boards
				if (line.length() > newSize) return Codes.ERR_JAGGED_BOARD;
			}
			else nowPadding = true; // Missing rows
			for (int x = 0; x < newSize; ++x) {
				// If missing rows, missing columns or current char is placeholder
				if (nowPadding || x >= line.length() || line.charAt(x) == '*')
					newValues[y][x] = NULL; // No value.
				else {
					// Assign value (as user input)
					newValues[y][x] = JLimitedTextField.getValue(
							line.charAt(x), newMin, newSize + newMin);
					if (newValues[y][x] == NULL) return Codes.ERR_OUT_OF_RANGE;
				}
			}
		}
		// If after all expected rows have been parsed there is still more...
		if (input.hasNextLine() && !input.nextLine().isEmpty())
			return Codes.ERR_INVALID_SIZE;
		
		// Actual board data update
		setMinValue(newMin);
		setBoardSize(newSize);
		container.updateMinField();
		container.updateSizeField();
		reset();
		
		// Actual board values update
		for (int y = 0; y < boardSize; ++y) {
			for (int x = 0; x < boardSize; ++x) {
				if (newValues[y][x] != NULL) {
					insert(x, y, newValues[y][x], true);
					fixPos[y][x] = true;
					setConflicts(x, y);
				}
			}
		}
		
		return Codes.SUCCESS;
	}
	
	/**
	 * Save board data to file.
	 * @param f file to save in
	 * @return success (true) / failure (false)
	 */
	public boolean writeFile (File f) {
		// Check if file is writable
		PrintWriter out;
		try { out = new PrintWriter(f); }
		catch (Exception e) { return false; }
		
		// Save metadata
		out.print("MIN:" + JLimitedTextField.getChar(min));
		out.println(",SIZE:" + boardSize);
		
		// Save board values
		for (int y = 0; y < boardSize; ++y) {
			for (int x = 0; x < boardSize; ++x) {
				if (board[y][x] == NULL) out.print('*');
				else out.print(JLimitedTextField.getChar(board[y][x]));
			}
			out.println();
		}
		out.close();
		
		return true;
	}
	
	/**
	 * Assigns a value to the given space.
	 * @param x x position
	 * @param y y position
	 * @param value value to assign
	 * @param fixed should value be immutable by the computer?
	 */
	private void insert(int x, int y, int value, boolean fixed) {
		board[y][x] = value;
		defPos[y][x] = fixed;
	}
	
	/**
	 * Update current working space position.
	 * @param x x position
	 * @param y y position
	 */
	private void setWorkPos(int x, int y) { wPosX = x; wPosY = y; }
	
	/**
	 * Update board state.
	 * @param state state to set
	 */
	private void setState(Codes state) {
		this.state = state;
		container.setStatus(getStatusString());
	}
	
	/**
	 * Update solving stage.
	 * @param stage stage to set
	 */
	private void setStage(Codes stage) {
		this.stage = stage;
		container.setStatus(getStatusString());
	}
	
	/**
	 * Reset all board data (keeps size information).
	 */
	private void setBoard() {
		timer.stop();
		bigNumStyle = new Font(Font.MONOSPACED, Font.BOLD,
				(int) (boardHeight * 0.936 / boardSize));
		smallNumStyle = new Font(Font.MONOSPACED, Font.PLAIN,
				(int) (boardHeight * 0.288 / boardSize));
		board = new int[boardSize][boardSize];
		fixPos = new boolean[boardSize][boardSize];
		defPos = new boolean[boardSize][boardSize];
		confPos = new boolean[boardSize][boardSize];
		pValues = new CIntQueue[boardSize][boardSize];
		
		// Iterate over board spaces
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				resetQueue(x, y);
				board[y][x] = NULL;
			}
		
		timer.start();
	}
	
	/**
	 * Reset the list at the given space position.
	 * @param x x position
	 * @param y y position
	 */
	private void resetQueue(int x, int y) {
		CIntQueue newQueue = new CIntQueue();
		for (int k = min; k < boardSize + min; ++k) newQueue.add(k);
		pValues[y][x] = newQueue;
	}
	
	/**
	 * Reset the iterator of the list at the given space position.
	 * @param x x position
	 * @param y y position
	 */
	private void resetIterator(int x, int y) { pValues[y][x].resetIterator(); }
	
	/**
	 * Check if a number is valid at the given space position.
	 * @param x x position
	 * @param y y position
	 * @param number number to check
	 * @return valid (true) / invalid (false)
	 */
	private boolean isValid (int x, int y, int number) {
		// Check board sub-square for matches
		for (int i = 0; i < boardSize; ++i)
			if (i != x && board[y][i] == number
			 || i != y && board[i][x] == number)
				return false;
		
		int startRow = (y / subSize) * subSize;
		int startColumn = (x / subSize) * subSize;

		// Check board column and row for matches
		for (int i = startRow; i < startRow + subSize; ++i) {
			if (i == y) continue;
			for (int j = startColumn; j < startColumn + subSize; ++j) {
				if (j == x) continue;
				if (board[i][j] == number)
					return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if a number is the only possibility at the given space position.
	 * @param x x position
	 * @param y y position
	 * @param number number to check
	 * @return true / false
	 */
	private boolean isOnlyPossibility(int x, int y, int number) {
		boolean flag = true;
		
		// Check if value is possible in another space in the same column
		outer:
			for (int i = 0; i < boardSize; ++i) {
				if (defPos[i][x] || i == y) continue; // Ignore definitive values
				int next = pValues[i][x].getNext();
				while (next != NULL) {
					if (next == number) {
						resetIterator(x, i);
						flag = false;
						break outer;
					}
					next = pValues[i][x].getNext();
				}
			}
		
		if (flag) return flag;
		flag = true;
		
		// Check if value is possible in another space in the same row
		outer:
			for (int i = 0; i < boardSize; ++i) {
				if (defPos[y][i] || i == x) continue; // Ignore definitive values
				int next = pValues[y][i].getNext();
				while (next != NULL) {
					if (next == number) {
						resetIterator(i, y);
						flag = false;
						break outer;
					}
				next = pValues[y][i].getNext();
				}
			}
		
		if (flag) return flag;
		flag = true;
		
		int startRow = (y / subSize) * subSize;
		int startColumn = (x / subSize) * subSize;
		
		// Check if value is possible in another space in the same board sub-square
		outer:
			for (int i = startRow; i < startRow + subSize; ++i)
				for (int j = startColumn; j < startColumn + subSize; ++j) {
					// Ignore definitive values
					if (defPos[i][j] || j == x && i == y) continue; 
					int next = pValues[i][j].getNext();
					while (next != NULL) {
						if (next == number) {
							resetIterator(j, i);
							flag = false;
							break outer;
						}
					next = pValues[i][j].getNext();
					}
				}
		
		return flag;
	}
	
	/**
	 * Set all conflicts related to given space position and update possible values.
	 * @param x x position
	 * @param y y position
	 */
	private void setConflicts(int x, int y) {
		confPos[y][x] = (board[y][x] != NULL && !isValid(x, y, board[y][x]));
		
		// Iterate over row spaces
		for (int row = 0; row < boardSize; ++row) {
			if (row == y) continue;
			if (board[row][x] == NULL) {
				// Empty space, regenerate possible values
				pValues[row][x] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(x, row, i)) pValues[row][x].add(i);
			}
			// Update conflicting
			else confPos[row][x] = !isValid(x, row, board[row][x]);
		}
		
		// Iterate over column spaces
		for (int column = 0; column < boardSize; ++column) {
			if (column == x) continue;
			if (board[y][column] == NULL) {
				// Empty space, regenerate possible values
				pValues[y][column] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(column, y, i)) pValues[y][column].add(i);
			}
			// Update conflicting
			else confPos[y][column] = !isValid(column, y, board[y][column]);
		}
		
		int startColumn = (x / subSize) * subSize;
		int startRow = (y / subSize) * subSize;
		
		// Iterate over board sub-square
		for (int row = startRow; row < startRow + subSize; ++row) {
			if (row == y) continue;
			for (int column = startColumn; column < startColumn + subSize; ++column)
			{
				if (column == x) continue;
				if (board[row][column] == NULL) {
					// Empty space, regenerate possible values
					pValues[row][column] = new CIntQueue();
					for (int i = min; i < boardSize + min; ++i)
						if (isValid(column, row, i))
							pValues[row][column].add(i);
				}
				// Update conflicting
				else confPos[row][column] =
						!isValid(column, row, board[row][column]);
			}
		}
	}
	
	/**
	 * Checks if a given number is a square number.
	 * @param num number to check
	 * @return true / false
	 */
	private boolean isSquare(int num) {
		int root = (int) Math.sqrt(num);
		return (root*root == num);
	}
	
	/**
	 * Private class to handle solving work for the board.
	 */
	private class GSudokuWorker extends SwingWorker<Boolean, Object> {
		/**
		 * Start solving process
		 */
		public Boolean doInBackground() {
			setState(Codes.STATE_WORKING);
			if (slowMotion) return doSlowMotion();
			return doNormal();
		}
		
		/** 
		 * Called when work is done
		 */
		public void done() {
			// Record finishing state
			if (isCancelled()) setState(Codes.STATE_CANCELLED);
			else {
				try {
					if (get()) setState(Codes.STATE_SUCCESS);
					else setState(Codes.STATE_FAILED);
				} catch (Exception e) { }
			}
			
			// Finish up
			showPValues = false;
			setWorkPos(NULL, NULL);
			container.setDone();
		}
		
		// NORMAL algorithm - as fast as possible
		
		/**
		 * Set-up trial-and-error backtracking algorithm.
		 * @return success (true) / failure (false)
		 */
		private boolean doTrial() {
			for (int y = 0; y < boardSize; ++y)
				for (int x = 0; x < boardSize; ++x)
					if (!defPos[y][x]) {
						setStage(Codes.STAGE_TRIAL_AND_ERROR);
						return btSolve(0, 0);
					}
			
			return true;
		}
		
		/**
		 * Try logical deduction solving, then fall into trial and error.
		 * @return success (true) / failure (false)
		 */
		private boolean doNormal() {
			boolean flag;
			
			setStage(Codes.STAGE_LOGICAL_DEDUCTION);
			
			/* Iterate over every space in the board until no changes are made
			 * by the logical-solving methods.
			 */
			while (true) {
				flag = true;
				for (int y = 0; y < boardSize; ++y) {
					for (int x = 0; x < boardSize; ++x) {
						if (defPos[y][x]) continue; // Skip definitive values
						
						// Only one possibility
						int first = pValues[y][x].getNext();
						if (pValues[y][x].getNext() == NULL) {
							insert(x, y, first, true);
							setConflicts(x, y);
							flag = false;
						} else resetIterator(x, y);
						
						// Only possibility
						int next = pValues[y][x].getNext();
						while (next != NULL) {
							if (isOnlyPossibility(x, y, next)) {
								insert(x, y, next, true);
								setConflicts(x, y);
								flag = false;
							}
							next = pValues[y][x].getNext();
						}
					}
				}
				if (flag) break;
			}
			
			return doTrial();
		}
		
		/**
		 * Backtracking (exhaustive) algorithm for trial-and-error solving.
		 * @param x current x position
		 * @param y current y position
		 * @return success (true) / failure (false)
		 */
		private boolean btSolve (int x, int y) {
			// Check if thread has been killed
			if (Thread.currentThread().isInterrupted()) 
				return true;
			
			// End of columns, go to next row
			if (x >= boardSize) {
				if (btSolve(0, y + 1)) return true;
				return false;
			}
			
			// End of rows, success
			if (y >= boardSize) 
				return true;
			
			// Skip definitive values
			if (defPos[y][x]) {
				if (btSolve(x + 1, y)) return true;
				return false;
			}
			
			// Try possible values
			int next = pValues[y][x].getNext();
			while (next != NULL) {
				if (isValid(x, y, next)) {
					board[y][x] = next;
					if (btSolve(x + 1, y)) return true;
					board[y][x] = NULL;
				}
				next = pValues[y][x].getNext();
			}
			
			// No values work, fall back
			return false;
		}
	}
	
	/* SLOW MOTION algorithm - delay computation to slow down solving process
	 * Great to keep track of the entire solving process.
	 */
	
	/**
	 * Set-up slow-motion trial-and-error backtracking algorithm.
	 * @return success (true) / failure (false)
	 */
	private boolean doTrialSlowMotion() {
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x)
				if (!defPos[y][x]) {
					setStage(Codes.STAGE_TRIAL_AND_ERROR);
					showPValues = false;
					return btSolveSlowMotion(0, 0);
				}
		return true;
	}
	
	/**
	 * Try slow-motion logical deduction solving, then fall into trial and error.
	 * @return success (true) / failure (false)
	 */
	private boolean doSlowMotion() {
		boolean flag;
		
		setStage(Codes.STAGE_LOGICAL_DEDUCTION);
		showPValues = true;
		
		/* Iterate over every space in the board until no changes are made
		 * by the logical-solving methods.
		 */
		while (true) {
			flag = true;
			for (int y = 0; y < boardSize; ++y) {
				for (int x = 0; x < boardSize; ++x) {
					if (defPos[y][x]) continue; // Skip definitive values
					setWorkPos(x, y); // Update working position
					
					// ZzZzZz... Delay.
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
					
					// Only one possibility
					int first = pValues[y][x].getNext();
					if (pValues[y][x].getNext() == NULL) {
						insert(x, y, first, true);
						setConflicts(x, y);
						flag = false;
					}
					else resetIterator(x, y);
					
					// ZzZzZz... Delay.
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
					
					// Only possibility
					int next = pValues[y][x].getNext();
					while (next != NULL) {
						if (isOnlyPossibility(x, y, next)) {
							insert(x, y, next, true);
							setConflicts(x, y);
							flag = false;
						}
						next = pValues[y][x].getNext();
					}
					
					// ZzZzZz... Delay.
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
				}
			}
			if (flag) break;
		}
		
		return doTrialSlowMotion();
	}
	
	/**
	 * Slow-motion backtracking (exhaustive) algorithm for trial-and-error solving.
	 * @param x current x position
	 * @param y current y position
	 * @return success (true) / failure (false)
	 */
	private boolean btSolveSlowMotion (int x, int y) {
		// ZzZzZz... Delay.
		try { Thread.sleep(100); }
		catch (Exception e) { return true; }
		
		// Check if thread has been killed
		if (Thread.currentThread().isInterrupted()) 
			return true;
		
		// End of columns, go to next row
		if (x >= boardSize) {
			if (btSolveSlowMotion(0, y + 1)) return true;
			return false;
		}
		
		// End of rows, success
		if (y >= boardSize) 
			return true;
		
		// Skip definitive values
		if (defPos[y][x]) {
			if (btSolveSlowMotion(x + 1, y)) return true;
			return false;
		}
		
		setWorkPos(x, y); // Update working position
		
		// Try possible values
		int next = pValues[y][x].getNext();
		while (next != NULL) {
			if (isValid(x, y, next)) {
				board[y][x] = next;
				if (btSolveSlowMotion(x + 1, y)) return true;
				board[y][x] = NULL;
			}
			next = pValues[y][x].getNext();
		}
		
		// No values work, fall back
		return false;
	}
}
