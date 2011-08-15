/*
 * TODO:
 * -- Improve algorithm
 *    Optimize: Define own class for holding these values QUEUE
 *    Use array of NODES
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.util.Queue;
import java.util.LinkedList;

import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class GSudokuBoard extends JPanel
	implements KeyListener, MouseListener, ActionListener
{
	public static final int ERR_OUT_OF_RANGE = -1;
	public static final int ERR_INVALID_SIZE = -2;
	public static final int ERR_NO_SYMBOLS = -3;
	public static final int SUCCESS = 0;
	
	public static final int NULL = -1;
	
	private int min = 1;
	private int subSize = 3;
	private int boardSize = 9;
	private int selX = NULL;
	private int selY = NULL;
	private boolean isClean;
	
	private int[][] board;
	private int[][] backupBoard;
	private boolean[][] fixedPos;
	private boolean[][] conflPos;
	private Queue<Integer>[][] posValues;
	
	private Timer timer;
	private Font numberStyle;
	private GSudokuWorker worker;
	private GSudokuContainer container;
	
	GSudokuBoard(GSudokuContainer con) {
		timer = new Timer(20, this);
		timer.start();
		setFocusable(true);
		addKeyListener(this);
		container = con;
		setBoard();
	}
	
	public int setSize(int size) {
		if (min + size > JLimitedTextField.MAX) return ERR_NO_SYMBOLS;
		if (size <= JLimitedTextField.MIN)      return ERR_OUT_OF_RANGE;
		if (!isSquare(size))					return ERR_INVALID_SIZE;
		boardSize = size;
		subSize = (int) Math.sqrt(size);
		setBoard();
		return SUCCESS;
	}
	
	public int setMin(int min) {
		if (min + boardSize > JLimitedTextField.MAX) return ERR_NO_SYMBOLS;
		if (min < JLimitedTextField.MIN) 			 return ERR_OUT_OF_RANGE;
		this.min = min;
		setBoard();
		return SUCCESS;
	}
	
	public int getBoardSize() { return boardSize; }
	public int getSubSize() { return subSize; }
	public int getMinValue() { return min; }
	public boolean isClean() { return isClean; }
	public boolean isWorking() {
		if (worker != null) return !worker.isDone();
		else return false;
	}
	public GSudokuContainer getBoardContainer() {
		return container;
	}
	
	public boolean isValid (int number, int row, int column) {
		for (int i = 0; i < boardSize; ++i)
			if (i != column && board[row][i] == number) return false;
		for (int i = 0; i < boardSize; ++i)
			if (i != row && board[i][column] == number) return false;
		
		int startRow = (row / subSize) * subSize;
		int startColumn = (column / subSize) * subSize;
		
		for (int i = startRow; i < startRow + subSize; ++i) {
			if (i == row) continue;
			for (int j = startColumn; j < startColumn + subSize; ++j) {
				if (j == column) continue;
				if (board[i][j] == number) return false;
			}
		}
		
		return true;
	}
	
	public void mouseClicked(MouseEvent e) {
		selX = (e.getX() - 25) * boardSize / 500;
		selY = (e.getY() - 56) * boardSize / 500;
		requestFocus();
	}
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	
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
		}
		if (isClean) {
			int value =  JLimitedTextField.getValue
						(e.getKeyChar(), min, boardSize+min);
			if (value != NULL) {
				board[selY][selX] = value;
				backupBoard[selY][selX] = value;
				fixedPos[selY][selX] = true;
				setConflicts(selY, selX);
			}
			else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
					|| e.getKeyCode() == KeyEvent.VK_DELETE) {
				board[selY][selX] = value;
				backupBoard[selY][selX] = value;
				fixedPos[selY][selX] = false;
				setConflicts(selY, selX);
			}
			
		}
	}
	public void keyReleased(KeyEvent e) { }
	public void keyTyped(KeyEvent e) { }
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		for (int i = 0; i < boardSize; ++i) {
			for (int j = 0; j < boardSize; ++j) {
				int posX = i*500/boardSize + 25;
				int posY = j*500/boardSize + 2;
				
				if (i == selX && j == selY) g.setColor(Color.YELLOW);
				else g.setColor(Color.WHITE);
				g.fillRect(posX, posY, 500/boardSize, 500/boardSize);
				
				g.setFont(numberStyle);
				if (conflPos[j][i]) g.setColor(Color.RED);
				else if (fixedPos[j][i]) g.setColor(Color.BLACK);
				else g.setColor(new Color(140,140,140)); 
				g.drawString(JLimitedTextField.getChar(board[j][i]) + "",
							posX + 115/boardSize,
							posY + 390/boardSize);
			}
		}
		g.setColor(Color.LIGHT_GRAY);
		for (int i = 1; i < boardSize; ++i) {
			g.fillRect(i * 500/boardSize + 24, 2, 2, 500);
			g.fillRect(24, i * 500/boardSize, 500, 2);
		}
		g.setColor(Color.BLACK);
		for (int i = 0; i <= boardSize; i += subSize) {
			g.fillRect(i * 500/boardSize + 23, 0, 3, 503);
			g.fillRect(23, i * 500/boardSize, 503, 3);
		}
	}
	
	public void cancel() { worker.cancel(true); }
	public void reset() {
		if (worker != null) worker.cancel(true);
		setBoard();
	}
	
	public void clean() {
		if (worker != null) worker.cancel(true);
		
		for (int i = 0; i < boardSize; ++i)
			for (int j = 0; j < boardSize; ++j)
				board[i][j] = backupBoard[i][j];
		
		isClean = true;
	}
	
	public void actionPerformed(ActionEvent e) {
		repaint();
	}
	
	public boolean solve() {
		if (isClean) {
			for (int i = 0; i < boardSize; ++i)
				for (int j = 0; j < boardSize; ++j)
					if (conflPos[i][j]) return false;
			isClean = false;
			worker = new GSudokuWorker(this, board, posValues, fixedPos);
			worker.execute();
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private void setBoard() {
		timer.stop();
		numberStyle = new Font(Font.MONOSPACED, Font.BOLD, 468/boardSize);
		board = new int[boardSize][boardSize];
		backupBoard = new int[boardSize][boardSize];
		fixedPos = new boolean[boardSize][boardSize];
		conflPos = new boolean[boardSize][boardSize];
		posValues = (LinkedList<Integer>[][])
					new LinkedList[boardSize][boardSize];
		
		for (int i = 0; i < boardSize; ++i)
			for (int j = 0; j < boardSize; ++j) {
				LinkedList<Integer> allValues = new LinkedList<Integer>();
				for (int k = min; k < boardSize + min; ++k)
					allValues.add(k);
				posValues[i][j] = allValues;
				board[i][j] = NULL;
				backupBoard[i][j] = NULL;
			}
				
		isClean = true;
		timer.start();
	}
	
	private void setConflicts(int row, int column) {
		conflPos[row][column] = (board[row][column] != NULL &&
								!isValid(board[row][column], row, column));
		
		for (int i = 0; i < boardSize; ++i) {
			if (i == column) continue;
			if (board[row][i] == NULL) {
				posValues[row][i] = new LinkedList<Integer>();
				for (int k = min; k < boardSize + min; ++k)
					if (isValid(k, row, i)) posValues[row][i].add(k);
			}
			else conflPos[row][i] = !isValid(board[row][i], row, i);
		}
		
		for (int i = 0; i < boardSize; ++i) {
			if (i == row) continue;
			if (board[i][column] == NULL) {
				posValues[i][column] = new LinkedList<Integer>();
				for (int k = min; k < boardSize + min; ++k)
					if (isValid(k, i, column)) posValues[i][column].add(k);
			}
			else conflPos[i][column] = !isValid(board[i][column], i, column);
		}
		
		int startRow = (row / subSize) * subSize;
		int startColumn = (column / subSize) * subSize;
		
		for (int i = startRow; i < startRow + subSize; ++i) {
			if (i == row) continue;
			for (int j = startColumn; j < startColumn + subSize; ++j) {
				if (j == column) continue;
				if (board[i][j] == NULL) {
					posValues[i][j] = new LinkedList<Integer>();
					for (int k = min; k < boardSize + min; ++k)
						if (isValid(k, i, j)) posValues[i][j].add(k);
				}
				else conflPos[i][j] = !isValid(board[i][j], i, j);
			}
		}
	}
	
	private boolean isSquare(int num) {
		int root = (int) Math.sqrt(num);
		return (root*root == num);
	}
}
