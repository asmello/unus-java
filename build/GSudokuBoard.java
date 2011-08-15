/*
 * TODO:
 * -- Improve algorithm
 *    2-3 trees to store row, column and subsquares values
 *    Add analytical phase, checking for definite solution cases until no logical
 *    deductions can be made.
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

public class GSudokuBoard extends JPanel {
	private static final long serialVersionUID = 1760927559618887130L;
	public static final int NULL = -1;
	
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
	
	private int[][] board;
	private boolean[][] fixPos;
	private boolean[][] defPos;
	private boolean[][] confPos;
	private CIntQueue[][] pValues;
	
	private Timer timer;
	private Font bigNumStyle;
	private Font smallNumStyle;
	private Codes stage = Codes.NULL;
	private Codes state = Codes.STATE_IDLE;
	private GSudokuWorker worker;
	private GSudokuContainer container;
	
	GSudokuBoard(GSudokuContainer con) {
		container = con;
		timer = new Timer(20, new ActionListener()
			{ public void actionPerformed(ActionEvent e) { repaint(); } });
		timer.start();
		
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
		
		addMouseListener(new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				selX = (e.getX() - xMargin) * boardSize / boardWidth;
				selY = e.getY() * boardSize / (boardHeight - 3);
				requestFocus();
			}
		});
		
		setFocusable(true);
		setBoard();
	}
	
	public void updateSize() {
		boardWidth = (int) (container.getAvailableWidth() * 0.9);
		xMargin = (container.getAvailableWidth() - boardWidth)/2;
		boardHeight = container.getAvailableHeight();
		bigNumStyle = new Font(Font.MONOSPACED, Font.BOLD,
				(int) (boardHeight * 0.936 / boardSize));
		smallNumStyle = new Font(Font.MONOSPACED, Font.PLAIN,
				(int) (boardHeight * 0.288 / boardSize));
	}
	
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
	
	public Codes setMinValue(int min) {
		if (this.min == min)
			return Codes.NO_TASK;
		if (min + boardSize > JLimitedTextField.MAX)
			return Codes.ERR_NO_SYMBOLS;
		if (min < JLimitedTextField.MIN)
			return Codes.ERR_OUT_OF_RANGE;
		
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
	
	public int getBoardSize() { return boardSize; }
	public int getSubSize() { return subSize; }
	public int getMinValue() { return min; }
	public void setSlowMotion (boolean state) { slowMotion = state; }
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		boolean conflictsFound = false;
		
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				int posX = x * boardWidth/boardSize + xMargin;
				int posY = y * (boardHeight-3)/boardSize + 2;
				
				if (x == selX && y == selY)
					g.setColor(Color.YELLOW);
				else if (x == wPosX && y == wPosY)
					g.setColor(new Color(210, 210, 210));
				else g.setColor(Color.WHITE);
				g.fillRect(posX, posY, boardWidth/boardSize, boardHeight/boardSize);
				
				g.setFont(bigNumStyle);
				if (confPos[y][x]) {
					g.setColor(Color.RED);
					conflictsFound = true;
				}
				else if (fixPos[y][x]) g.setColor(Color.BLACK);
				else g.setColor(new Color(140,140,140)); 
				g.drawString(JLimitedTextField.getChar(board[y][x]) + "",
							posX + (int)(boardWidth * 0.5 / boardSize
									- smallNumStyle.getSize()),
							posY + (int)(boardHeight*0.78/boardSize));
				
				if (board[y][x] == NULL && showPValues) {
					g.setFont(smallNumStyle);
					for (int i = min; i < min + boardSize; ++i) {
						int next = pValues[y][x].getNext();
						while (next != NULL) {
							if (i == next)
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
		
		if (conflictsFound) setState(Codes.STATE_CONFLICT);
		else if (state == Codes.STATE_CONFLICT) setState(Codes.STATE_IDLE);
	}
	
	public void cancel() { worker.cancel(true); }
	public void reset() {
		if (worker != null) worker.cancel(true);
		setBoard();
		setState(Codes.STATE_IDLE);
		container.setClean();
	}
	public void clean() {
		if (worker != null) worker.cancel(true);
		
		defPos = new boolean[boardSize][boardSize];
		confPos = new boolean[boardSize][boardSize];
		
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				defPos[y][x] = fixPos[y][x];
				if (!defPos[y][x]) board[y][x] = NULL;
				resetQueue(x, y);
			}
		
		for (int y = 0; y < boardSize; ++y) 
			for (int x = 0; x < boardSize; ++x)
				if (board[y][x] != NULL) setConflicts(x, y);
		
		setState(Codes.STATE_IDLE);
		container.setClean();
	}
	
	public boolean solve() {
		if (state == Codes.STATE_CONFLICT) return false;
		worker = new GSudokuWorker();
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				container.setStatus(getStatusString());
			}
		});
		worker.execute();
		return true;
	}
	
	public Codes readFile (File f) {
		Scanner input;
		try { input = new Scanner(f); }
		catch (Exception e) { return Codes.ERR_READ; }
		
		int newMin, newSize;
		int[][] newValues;
		
		if (input.hasNextLine()) {
			String firstLine = input.nextLine();
			if (firstLine.length() != 13
				|| firstLine.charAt(5) != ','
				|| !firstLine.substring(0, 4).toUpperCase().equals("MIN:")
				|| !firstLine.substring(6, 11).toUpperCase().equals("SIZE:"))
				return Codes.ERR_PARSING;
			newMin = JLimitedTextField.getValue(firstLine.charAt(4),
												JLimitedTextField.MIN,
												JLimitedTextField.MAX);
			if (newMin == -1) return Codes.ERR_PARSING;
			try { newSize = Integer.parseInt(firstLine.substring(11, 13)); }
			catch (Exception e) { return Codes.ERR_PARSING; }
			if (newMin + newSize > JLimitedTextField.MAX)
				return Codes.ERR_NO_SYMBOLS;
			if (newSize <= JLimitedTextField.MIN || !isSquare(newSize))
				return Codes.ERR_INVALID_SIZE;
			newValues = new int[newSize][newSize];
		} else return Codes.ERR_PARSING;
		
		String line = null;
		boolean nowPadding = false;
		for (int y = 0; y < newSize; ++y) {
			if (!nowPadding && input.hasNextLine()) {
				line = input.nextLine();
				if (line.length() > newSize) return Codes.ERR_JAGGED_BOARD;
			}
			else nowPadding = true;
			for (int x = 0; x < newSize; ++x) {
				if (nowPadding || x >= line.length() || line.charAt(x) == '*')
					newValues[y][x] = NULL;
				else {
					newValues[y][x] = JLimitedTextField.getValue(
							line.charAt(x), newMin, newSize + newMin);
					if (newValues[y][x] == -1) return Codes.ERR_OUT_OF_RANGE;
				}
			}
		}
		if (input.hasNextLine() && !input.nextLine().isEmpty())
			return Codes.ERR_INVALID_SIZE;
		
		setMinValue(newMin);
		setBoardSize(newSize);
		container.updateMinField();
		container.updateSizeField();
		reset();
		
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
	
	public boolean writeFile (File f) {
		PrintWriter out;
		try { out = new PrintWriter(f); }
		catch (Exception e) { return false; }
		
		out.print("MIN:" + JLimitedTextField.getChar(min));
		out.println(",SIZE:" + boardSize);
		
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
	
	private void insert(int x, int y, int value, boolean fixed) {
		board[y][x] = value;
		defPos[y][x] = fixed;
	}
	
	private void setWorkPos(int x, int y) { wPosX = x; wPosY = y; }
	
	private void setState(Codes state) {
		this.state = state;
		container.setStatus(getStatusString());
	}
	
	private void setStage(Codes stage) {
		this.stage = stage;
		container.setStatus(getStatusString());
	}
	
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
		
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				resetQueue(x, y);
				board[y][x] = NULL;
			}
		
		timer.start();
	}
	
	private void resetQueue(int x, int y) {
		CIntQueue newQueue = new CIntQueue();
		for (int k = min; k < boardSize + min; ++k) newQueue.add(k);
		pValues[y][x] = newQueue;
	}
	
	private void resetIterator(int x, int y) { pValues[y][x].resetIterator(); }
	
	private boolean isValid (int x, int y, int number) {
		for (int i = 0; i < boardSize; ++i)
			if (i != x && board[y][i] == number
			 || i != y && board[i][x] == number)
				return false;
		
		int startRow = (y / subSize) * subSize;
		int startColumn = (x / subSize) * subSize;
		
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
	
	private boolean isOnlyPossibility(int x, int y, int number) {
		boolean flag = true;
		
		outer:
			for (int i = 0; i < boardSize; ++i) {
				if (defPos[i][x] || i == y) continue;
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
		
		outer:
			for (int i = 0; i < boardSize; ++i) {
				if (defPos[y][i] || i == x) continue;
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
		
		outer:
			for (int i = startRow; i < startRow + subSize; ++i)
				for (int j = startColumn; j < startColumn + subSize; ++j) {
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
	
	private void setConflicts(int x, int y) {
		confPos[y][x] = (board[y][x] != NULL && !isValid(x, y, board[y][x]));
		
		for (int row = 0; row < boardSize; ++row) {
			if (row == y) continue;
			if (board[row][x] == NULL) {
				pValues[row][x] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(x, row, i)) pValues[row][x].add(i);
			}
			else confPos[row][x] = !isValid(x, row, board[row][x]);
		}
		
		for (int column = 0; column < boardSize; ++column) {
			if (column == x) continue;
			if (board[y][column] == NULL) {
				pValues[y][column] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(column, y, i)) pValues[y][column].add(i);
			}
			else confPos[y][column] = !isValid(column, y, board[y][column]);
		}
		
		int startColumn = (x / subSize) * subSize;
		int startRow = (y / subSize) * subSize;
		
		for (int row = startRow; row < startRow + subSize; ++row) {
			if (row == y) continue;
			for (int column = startColumn; column < startColumn + subSize; ++column)
			{
				if (column == x) continue;
				if (board[row][column] == NULL) {
					pValues[row][column] = new CIntQueue();
					for (int i = min; i < boardSize + min; ++i)
						if (isValid(column, row, i))
							pValues[row][column].add(i);
				}
				else confPos[row][column] =
						!isValid(column, row, board[row][column]);
			}
		}
	}
	
	private boolean isSquare(int num) {
		int root = (int) Math.sqrt(num);
		return (root*root == num);
	}
	
	private class GSudokuWorker extends SwingWorker<Boolean, Object> {
		public Boolean doInBackground() {
			setState(Codes.STATE_WORKING);
			if (slowMotion) return doSlowMotion();
			return doNormal();
		}
		
		public void done() {
			if (isCancelled()) setState(Codes.STATE_CANCELLED);
			else {
				try {
					if (get()) setState(Codes.STATE_SUCCESS);
					else setState(Codes.STATE_FAILED);
				} catch (Exception e) { }
			}
			
			showPValues = false;
			setWorkPos(NULL, NULL);
			container.setDone();
		}
		
		private boolean doTrial() {
			for (int y = 0; y < boardSize; ++y)
				for (int x = 0; x < boardSize; ++x)
					if (!defPos[y][x]) {
						setStage(Codes.STAGE_TRIAL_AND_ERROR);
						return btSolve(0, 0);
					}
			
			return true;
		}
		
		private boolean doNormal() {
			boolean flag;
			
			setStage(Codes.STAGE_LOGICAL_DEDUCTION);
			
			while (true) {
				flag = true;
				for (int y = 0; y < boardSize; ++y) {
					for (int x = 0; x < boardSize; ++x) {
						if (defPos[y][x]) continue;
						
						int first = pValues[y][x].getNext();
						if (pValues[y][x].getNext() == NULL) {
							insert(x, y, first, true);
							setConflicts(x, y);
							flag = false;
						} else resetIterator(x, y);
						
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
		
		private boolean btSolve (int x, int y) {
			if (Thread.currentThread().isInterrupted()) 
				return true;
			
			if (x >= boardSize) {
				if (btSolve(0, y + 1)) return true;
				return false;
			}
			
			if (y >= boardSize) 
				return true;
			
			if (defPos[y][x]) {
				if (btSolve(x + 1, y)) return true;
				return false;
			}
			
			int next = pValues[y][x].getNext();
			while (next != NULL) {
				if (isValid(x, y, next)) {
					board[y][x] = next;
					if (btSolve(x + 1, y)) return true;
					board[y][x] = NULL;
				}
				next = pValues[y][x].getNext();
			}
			
			return false;
		}
	}
	
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
	
	private boolean doSlowMotion() {
		boolean flag;
		
		setStage(Codes.STAGE_LOGICAL_DEDUCTION);
		showPValues = true;
		
		while (true) {
			flag = true;
			for (int y = 0; y < boardSize; ++y) {
				for (int x = 0; x < boardSize; ++x) {
					if (defPos[y][x]) continue;
					setWorkPos(x, y);
					
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
					
					int first = pValues[y][x].getNext();
					if (pValues[y][x].getNext() == NULL) {
						insert(x, y, first, true);
						setConflicts(x, y);
						flag = false;
					}
					else resetIterator(x, y);
					
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
					
					int next = pValues[y][x].getNext();
					while (next != NULL) {
						if (isOnlyPossibility(x, y, next)) {
							insert(x, y, next, true);
							setConflicts(x, y);
							flag = false;
						}
						next = pValues[y][x].getNext();
					}
					
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
				}
			}
			if (flag) break;
		}
		
		return doTrialSlowMotion();
	}
	
	private boolean btSolveSlowMotion (int x, int y) {
		try { Thread.sleep(100); }
		catch (Exception e) { return true; }
		
		if (Thread.currentThread().isInterrupted()) 
			return true;
		
		if (x >= boardSize) {
			if (btSolveSlowMotion(0, y + 1)) return true;
			return false;
		}
		
		if (y >= boardSize) 
			return true;
		
		if (defPos[y][x]) {
			if (btSolveSlowMotion(x + 1, y)) return true;
			return false;
		}
		
		setWorkPos(x, y);
		
		int next = pValues[y][x].getNext();
		while (next != NULL) {
			if (isValid(x, y, next)) {
				board[y][x] = next;
				if (btSolveSlowMotion(x + 1, y)) return true;
				board[y][x] = NULL;
			}
			next = pValues[y][x].getNext();
		}
		
		return false;
	}
}
