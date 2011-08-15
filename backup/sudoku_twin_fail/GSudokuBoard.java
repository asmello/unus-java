/*
 * TODO:
 * -- Improve algorithm
 *    2-3 trees to store row, column and subsquares values
 *    Add analytical phase, checking for definite solution cases until no logical
 *    deductions can be made.
 *    -- Sub-group exclusion
 *    -- Hidden twin
 *    
 *    Display possibilities queue as small numbers
 *    
 * -- Irregular boards
 * -- Lock feature
 *    > Allow use as normal sudoku board
 * -- Resize
 * -- File support
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

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

public class GSudokuBoard extends JPanel {
	public  static final int NULL = -1;
	private static final int boardWidth = 500;
	private static final int boardHeight = 500;
	
	private int min = 1;
	private int subSize = 3;
	private int boardSize = 9;
	private int selX = NULL;
	private int selY = NULL;
	private boolean slowMotion = false;
	
	private int[][] board;
	private boolean[][] fixPos;
	private boolean[][] defPos;
	private boolean[][] confPos;
	private CIntQueue[][] pValues;
	
	private Timer timer;
	private Font numberStyle;
	private Codes subStage = Codes.NULL;
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
				selX = (e.getX() - 25) * boardSize / boardWidth;
				selY = e.getY() * boardSize / boardHeight;
				requestFocus();
			}
		});
		
		setFocusable(true);
		setBoard();
	}
	
	public Codes setSize(int size) {
		if (boardSize == size) 						  return Codes.NO_TASK;
		if (min + size > JLimitedTextField.MAX) 	  return Codes.ERR_NO_SYMBOLS;
		if (size <= JLimitedTextField.MIN)     		  return Codes.ERR_OUT_OF_RANGE;
		if (!isSquare(size))						  return Codes.ERR_INVALID_SIZE;
		boardSize = size;
		subSize = (int) Math.sqrt(size);
		setBoard();
		container.setClean();
		return Codes.SUCCESS;
	}
	
	public Codes setMin(int min) {
		if (this.min == min) 						  return Codes.NO_TASK;
		if (min + boardSize > JLimitedTextField.MAX)  return Codes.ERR_NO_SYMBOLS;
		if (min < JLimitedTextField.MIN) 			  return Codes.ERR_OUT_OF_RANGE;
		this.min = min;
		setBoard();
		container.setClean();
		return Codes.SUCCESS;
	}
	
	public String getStatusString() {
		switch (state) {
		case STATE_IDLE:
			return "Ready.";
		case STATE_WORKING:
			switch (stage) {
			case STAGE_LOGICAL_DEDUCTION:
				String submsg;
				switch (subStage) {
				case SUBSTAGE_TWIN_ELIMINATION:
					submsg = ", Twin Elimination";
					break;
				case SUBSTAGE_ONE_POSSIBILITY:
					submsg = ", One Possibility Check";
					break;
				case SUBSTAGE_ONLY_POSSIBILITY:
					submsg = ", Only Possibility Check";
					break;
				default:
					submsg = "";
				}
				return "Working... Stage 1: Logical Deduction" + submsg;
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
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				int posX = x * boardWidth/boardSize + 25;
				int posY = y * boardHeight/boardSize + 2;
				
				if (x == selX && y == selY) g.setColor(Color.YELLOW);
				else g.setColor(Color.WHITE);
				g.fillRect(posX, posY, boardWidth/boardSize, boardHeight/boardSize);
				
				g.setFont(numberStyle);
				if (confPos[y][x]) g.setColor(Color.RED);
				else if (fixPos[y][x]) g.setColor(Color.BLACK);
				else g.setColor(new Color(140,140,140)); 
				g.drawString(JLimitedTextField.getChar(board[y][x]) + "",
							posX + (int)(boardWidth*0.23/boardSize),
							posY + (int)(boardHeight*0.78/boardSize));
			}
		
		g.setColor(Color.LIGHT_GRAY);
		for (int i = 1; i < boardSize; ++i) {
			g.fillRect(i * boardWidth / boardSize + 24, 2, 2, boardWidth);
			g.fillRect(24, i * boardHeight / boardSize, boardHeight, 2);
		}
		g.setColor(Color.BLACK);
		for (int i = 0; i <= boardSize; i += subSize) {
			g.fillRect(i * boardWidth / boardSize + 23, 0, 3, boardWidth+3);
			g.fillRect(23, i * boardHeight / boardSize, boardHeight+3, 3);
		}
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
			};
		
		for (int y = 0; y < boardSize; ++y) 
			for (int x = 0; x < boardSize; ++x)
				if (board[y][x] != NULL) setConflicts(x, y);
		
		setState(Codes.STATE_IDLE);
		container.setClean();
	}
	
	public boolean solve() {
		for (int i = 0; i < boardSize; ++i)
			for (int j = 0; j < boardSize; ++j)
				if (confPos[i][j]) return false;
		worker = new GSudokuWorker();
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
				{ container.setStatus(getStatusString()); }
		});
		worker.execute();
		return true;
	}
	
	private void insert(int x, int y, int value, boolean fixed) {
		board[y][x] = value;
		defPos[y][x] = fixed;
	}
	
	private void setState(Codes state) {
		this.state = state;
		container.setStatus(getStatusString());
	}
	
	private void setStage(Codes stage) {
		this.stage = stage;
		container.setStatus(getStatusString());
	}
	
	private void setSubStage(Codes subStage) {
		this.subStage = subStage;
		container.setStatus(getStatusString());
	}
	
	private void setBoard() {
		timer.stop();
		numberStyle = new Font(Font.MONOSPACED, Font.BOLD, 468/boardSize);
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
		
		int startRow = (y / subSize) * subSize;
		int startColumn = (x / subSize) * subSize;
		
		outer:
		for (int i = startRow; i < startRow + subSize; ++i) {
			if (i == y) continue;
			for (int j = startColumn; j < startColumn + subSize; ++j) {
				if (defPos[i][j] || j == x) continue;
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
		}
		return flag;
	}
	
	private void proccessTwins(int x, int y) {
		setSubStage(Codes.SUBSTAGE_TWIN_ELIMINATION);
		
		for (int i = 0; i < boardSize; ++i) {
			if (defPos[i][x] || i == y) continue;
			CIntQueue intersect = intersection(x, y, x, i);
			intersect.getNext();
			if (intersect.getNext() == NULL) continue;
			else {
				intersect.resetIterator();
				pValues[y][x] = intersection(x, y, x, i);
				pValues[i][x] = intersection(x, y, x, i);
				int next = intersect.getNext();
				while (next != NULL) {
					for (int j = 0; j < boardSize; ++j) {
						if (defPos[j][x] || j == y) continue;
						CIntQueue newQueue = new CIntQueue();
						int next2 = pValues[j][x].getNext();
						while (next2 != NULL) {
							if (next2 != next) newQueue.add(next2);
							next2 = pValues[j][x].getNext();
						}
						pValues[j][x] = newQueue;
					}
					next = intersect.getNext();
				}
			}
		}
		
		for (int i = 0; i < boardSize; ++i) {
			if (defPos[y][i] || i == x) continue;
			CIntQueue intersect = intersection(x, y, i, y);
			intersect.getNext();
			if (intersect.getNext() == NULL) continue;
			else {
				intersect.resetIterator();
				pValues[y][x] = intersection(x, y, i, y);
				pValues[y][i] = intersection(x, y, i, y);
				int next = intersect.getNext();
				while (next != NULL) {
					for (int j = 0; j < boardSize; ++j) {
						if (defPos[y][j] || j == x) continue;
						CIntQueue newQueue = new CIntQueue();
						int next2 = pValues[y][j].getNext();
						while (next2 != NULL) {
							if (next2 != next) newQueue.add(next2);
							next2 = pValues[y][j].getNext();
						}
						pValues[y][j] = newQueue;
					}
					next = intersect.getNext();
				}
			}
		}
		
		int startRow = (y / subSize) * subSize;
		int startColumn = (x / subSize) * subSize;
		
		for (int i = startRow; i < startRow + subSize; ++i) {
			if (i == y) continue;
			for (int j = startColumn; j < startColumn + subSize; ++j) {
				if (defPos[i][j] || j == x) continue;
				CIntQueue intersect = intersection(x, y, j, i);
				intersect.getNext();
				if (intersect.getNext() == NULL) continue;
				else {
					intersect.resetIterator();
					pValues[y][x] = intersection(x, y, j, i);
					pValues[i][j] = intersection(x, y, j, i);
					int next = intersect.getNext();
					while (next != NULL) {
						for (int k = startRow; k < startRow + subSize; ++k) {
							if (k == y) continue;
							for (int l = startColumn; l < startColumn+subSize; ++l)
							{
								if (defPos[k][l] || l == x) continue;
								CIntQueue newQueue = new CIntQueue();
								int next2 = pValues[k][l].getNext();
								while (next2 != NULL) {
									if (next2 != next) newQueue.add(next2);
									next2 = pValues[k][l].getNext();
								}
								pValues[k][l] = newQueue;
							}
						}
						next = intersect.getNext();
					}
				}
			}
		}
		
	}
	
	private CIntQueue intersection (int x1, int y1, int x2, int y2) {
		CIntQueue intersect = new CIntQueue();
		int next1 = pValues[y1][x1].getNext();
		while (next1 != NULL) {
			int next2 = pValues[y2][x2].getNext();
			while (next2 != NULL)
				if (next1 == next2) intersect.add(next1);
			next1 = pValues[y1][x1].getNext();
		}
		return intersect;
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
			if (slowMotion) return doLogicSlowMotion();
			return doLogicNormal();
		}
		
		public void done() {
			if (isCancelled()) setState(Codes.STATE_CANCELLED);
			else {
				try {
					if (get()) setState(Codes.STATE_SUCCESS);
					else setState(Codes.STATE_FAILED);
				} catch (Exception e) { }
			}
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
		
		private boolean doLogicNormal() {
			boolean flag;
			
			setStage(Codes.STAGE_LOGICAL_DEDUCTION);
			
			while (true) {
				flag = true;
				for (int y = 0; y < boardSize; ++y) {
					for (int x = 0; x < boardSize; ++x) {
						if (defPos[y][x]) continue;
						
						//proccessTwins(x, y);
						
						int first = pValues[y][x].getNext();
						if (pValues[y][x].getNext() == NULL) {
							insert(x, y, first, true);
							setConflicts(x, y);
							flag = false;
						}
						else resetIterator(x, y);
						
						if (flag) {
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
					return btSolveSlowMotion(0, 0);
				}
		return true;
	}
	
	private boolean doLogicSlowMotion() {
		boolean flag;
		
		setStage(Codes.STAGE_LOGICAL_DEDUCTION);
		
		while (true) {
			flag = true;
			for (int y = 0; y < boardSize; ++y) {
				for (int x = 0; x < boardSize; ++x) {
					if (defPos[y][x]) continue;
					
					//proccessTwins(x, y);
					
					setSubStage(Codes.SUBSTAGE_ONE_POSSIBILITY);
					int first = pValues[y][x].getNext();
					if (pValues[y][x].getNext() == NULL) {
						insert(x, y, first, true);
						setConflicts(x, y);
						flag = false;
					}
					else resetIterator(x, y);
					
					try { Thread.sleep(200); }
					catch (Exception e) { return false; }
					
					if (flag) {
						setSubStage(Codes.SUBSTAGE_ONLY_POSSIBILITY);
						
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
