/*
 * TODO:
 * -- Improve algorithm
 *    2-3 trees to store row, column and subsquares values
 *    Add analytical phase, checking for definite solution cases until no logical
 *    deductions can be made. Reintroduce backupboard and take advantage of isfixed
 *    tag in backtracking stage.
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

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

public class GSudokuBoard extends JPanel {
	public static final int NULL = -1;
	private static final int boardWidth = 500;
	private static final int boardHeight = 500;
	
	private int min = 1;
	private int subSize = 3;
	private int boardSize = 9;
	private int selX = NULL;
	private int selY = NULL;
	
	private int[][] board;
	private boolean[][] fixedPos;
	private boolean[][] conflPos;
	private CIntQueue[][] possibleValues;
	
	private Timer timer;
	private Font numberStyle;
	private GSudokuWorker worker;
	private GSudokuContainer container;
	
	GSudokuBoard(GSudokuContainer con) {
		container = con;
		timer = new Timer(20, new ActionListener()
			{ public void actionPerformed(ActionEvent e) { repaint(); } });
		timer.start();
		setFocusable(true);
		
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
						setConflicts(selX, selY);
					} break;
				default:
					if (container.isClean()) {
						int value =  JLimitedTextField.getValue
								(e.getKeyChar(), min, boardSize + min);
						if (value != NULL) {
							insert(selX, selY, value, true);
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
		
		setBoard();
	}
	
	public Codes setSize(int size) {
		if (boardSize == size) 					return Codes.NO_TASK;
		if (min + size > JLimitedTextField.MAX) return Codes.ERR_NO_SYMBOLS;
		if (size <= JLimitedTextField.MIN)      return Codes.ERR_OUT_OF_RANGE;
		if (!isSquare(size))					return Codes.ERR_INVALID_SIZE;
		boardSize = size;
		subSize = (int) Math.sqrt(size);
		setBoard();
		container.setClean();
		return Codes.SUCCESS;
	}
	
	public Codes setMin(int min) {
		if (this.min == min) 						 return Codes.NO_TASK;
		if (min + boardSize > JLimitedTextField.MAX) return Codes.ERR_NO_SYMBOLS;
		if (min < JLimitedTextField.MIN) 			 return Codes.ERR_OUT_OF_RANGE;
		this.min = min;
		setBoard();
		container.setClean();
		return Codes.SUCCESS;
	}
	
	public int getBoardSize() { return boardSize; }
	public int getSubSize() { return subSize; }
	public int getMinValue() { return min; }
	public int getNextValue(int x, int y) { return possibleValues[y][x].getNext(); }
	public boolean isFixed(int x, int y) { return fixedPos[y][x]; }
	
	public void setDone(boolean success)
		{ container.setDone(worker.isCancelled() || success); }
	public void insert(int x, int y, int value) { board[y][x] = value; }
	
	
	public boolean isValid (int x, int y, int number) {
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
				if (conflPos[y][x]) g.setColor(Color.RED);
				else if (fixedPos[y][x]) g.setColor(Color.BLACK);
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
		container.setClean();
	}
	public void clean() {
		if (worker != null) worker.cancel(true);
		
		for (int i = 0; i < boardSize; ++i)
			for (int j = 0; j < boardSize; ++j) {
				if (!fixedPos[i][j]) {
					board[i][j] = -1;
					possibleValues[i][j].resetIterator();
				}
			}
		
		container.setClean();
	}
	
	public boolean solve() {
		for (int i = 0; i < boardSize; ++i)
			for (int j = 0; j < boardSize; ++j)
				if (conflPos[i][j]) return false;
		worker = new GSudokuWorker(this);
		worker.execute();
		return true;
	}
	
	private void insert(int x, int y, int value, boolean fixed) {
		board[y][x] = value;
		fixedPos[y][x] = fixed;
	}
	
	private void setBoard() {
		timer.stop();
		numberStyle = new Font(Font.MONOSPACED, Font.BOLD, 468/boardSize);
		board = new int[boardSize][boardSize];
		fixedPos = new boolean[boardSize][boardSize];
		conflPos = new boolean[boardSize][boardSize];
		possibleValues = new CIntQueue[boardSize][boardSize];
		
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x) {
				CIntQueue newQueue = new CIntQueue();
				for (int k = min; k < boardSize + min; ++k) newQueue.add(k);
				possibleValues[y][x] = newQueue;
				board[y][x] = NULL;
			}
		
		timer.start();
	}
	
	private void setConflicts(int x, int y) {
		conflPos[y][x] = (board[y][x] != NULL && !isValid(x, y, board[y][x]));
		
		for (int row = 0; row < boardSize; ++row) {
			if (row == y) continue;
			if (board[row][x] == NULL) {
				possibleValues[row][x] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(x, row, i)) possibleValues[row][x].add(i);
			}
			else conflPos[row][x] = !isValid(x, row, board[row][x]);
		}
		
		for (int column = 0; column < boardSize; ++column) {
			if (column == x) continue;
			if (board[y][column] == NULL) {
				possibleValues[y][column] = new CIntQueue();
				for (int i = min; i < boardSize + min; ++i)
					if (isValid(column, y, i)) possibleValues[y][column].add(i);
			}
			else conflPos[y][column] = !isValid(column, y, board[y][column]);
		}
		
		int startColumn = (x / subSize) * subSize;
		int startRow = (y / subSize) * subSize;
		
		for (int row = startRow; row < startRow + subSize; ++row) {
			if (row == y) continue;
			for (int column = startColumn; column < startColumn + subSize; ++column)
			{
				if (column == x) continue;
				if (board[row][column] == NULL) {
					possibleValues[row][column] = new CIntQueue();
					for (int i = min; i < boardSize + min; ++i)
						if (isValid(column, row, i))
							possibleValues[row][column].add(i);
				}
				else conflPos[row][column] =
										 !isValid(column, row, board[row][column]);
			}
		}
	}
	
	private boolean isSquare(int num) {
		int root = (int) Math.sqrt(num);
		return (root*root == num);
	}
}
