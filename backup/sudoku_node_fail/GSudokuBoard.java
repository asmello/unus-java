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

import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class GSudokuBoard extends JPanel
	implements KeyListener, MouseListener, ActionListener
{
	public static final int NULL = -1;
	public static final int ERR_OUT_OF_RANGE = -2;
	public static final int ERR_INVALID_SIZE = -3;
	public static final int ERR_NO_SYMBOLS = -4;
	public static final int SUCCESS = 0;
	
	private int min = 1;
	private int subSize = 3;
	private int boardSize = 9;
	private int selX = NULL;
	private int selY = NULL;
	private boolean isClean;
	
	private SudokuNode[][] board;
	
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
	public GSudokuContainer getBoardContainer() {
		return container;
	}
	
	public void setConflicts (SudokuNode node) {
		node.setConflict(isValid());
		
		for (int i = 0; i < boardSize; ++i) {
			if (board[node.getPosY()][i].isNull()) {
				board[node.getPosY()][i].cleanPossibleValues();
				for (int j = min; j < min + boardSize; ++j)
					if (isValid(j, board[node.getPosY()][i]))
						board[node.getPosY()][i].addPossibleValue(j);
			}
			else board[node.getPosY()][i].setConflict(!isValid(board[node.getPosY()][i]));
			if (board[i][node.getPosX()].isNull()) {
				board[i][node.getPosX()].cleanPossibleValues();
				for (int j = min; j < min + boardSize; ++j)
					if (isValid(j, board[i][node.getPosX()]))
						board[i][node.getPosX()].addPossibleValue(j);
			}
			else board[i][node.getPosX()].setConflict(!isValid(board[i][node.getPosX()]));
		}
		
		for (int y = node.getSubY(); y < node.getSubY() + subSize; ++y)
			for (int x = node.getSubX(); x < node.getSubX() + subSize; ++x) {
				if (board[y][x].isNull()) {
					board[y][x].cleanPossibleValues();
					for (int j = min; j < min + boardSize; ++j)
						if (isValid(j, board[y][x]))
							board[y][x].addPossibleValue(j);
				}
				else board[y][x].setConflict(!isValid(board[y][x]));
			}
	}
	
	public boolean isValid(int value, SudokuNode node) {
		for (int i = 0; i < boardSize; ++i)
			if (board[node.getPosY()][i].getValue() == value
				|| board[i][node.getPosX()].getValue() == value)
				return false;
		
		for (int y = node.getSubY(); y < node.getSubY() + subSize; ++y)
			for (int x = node.getSubX(); x < node.getSubX() + subSize; ++x)
				if (board[y][x].getValue() == value) return false;
		
		return true;
	}
	
	public boolean isValid(SudokuNode node) {
		for (int i = 0; i < boardSize; ++i)
			if (node.sameValue(board[node.getPosY()][i])
				|| node.sameValue(board[i][node.getPosX()]))
				return false;
		
		for (int y = node.getSubY(); y < node.getSubY() + subSize; ++y)
			for (int x = node.getSubX(); x < node.getSubX() + subSize; ++x)
				if (node.sameValue(board[y][x])) return false;
		
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
				board[selY][selX].setValue(value);
				board[selY][selX].setFixed(true);
				setConflicts(board[selY][selX]);
			}
			else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
					|| e.getKeyCode() == KeyEvent.VK_DELETE) {
				board[selY][selX].setValue(value);
				board[selY][selX].setFixed(false);
				setConflicts(board[selY][selX]);
			}
			
		}
	}
	public void keyReleased(KeyEvent e) { }
	public void keyTyped(KeyEvent e) { }
	
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		for (int y = 0; y < boardSize; ++y) {
			for (int x = 0; x < boardSize; ++x) {
				int posX = x*500/boardSize + 25;
				int posY = y*500/boardSize + 2;
				
				if (x == selX && y == selY) g.setColor(Color.YELLOW);
				else g.setColor(Color.WHITE);
				g.fillRect(posX, posY, 500/boardSize, 500/boardSize);
				
				g.setFont(numberStyle);
				if (board[y][x].isConflicting()) g.setColor(Color.RED);
				else if (board[y][x].isFixed()) g.setColor(Color.BLACK);
				else g.setColor(new Color(140,140,140)); 
				g.drawString(JLimitedTextField.getChar(board[y][x].getValue()) + "",
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
		
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x)
				if (!board[y][x].isFixed()) {
					board[y][x].setNull();
					board[y][x].resetPossibleValuesIterator();
				}
		
		isClean = true;
	}
	
	public void actionPerformed(ActionEvent e) { repaint(); }
	
	public boolean solve() {
		if (isClean) {
			for (int y = 0; y < boardSize; ++y)
				for (int x = 0; x < boardSize; ++x)
					if (board[y][x].isConflicting()) return false;
			isClean = false;
			worker = new GSudokuWorker(this, board);
			worker.execute();
		}
		return true;
	}
	
	private void setBoard() {
		timer.stop();
		numberStyle = new Font(Font.MONOSPACED, Font.BOLD, 468/boardSize);
		board = new SudokuNode[boardSize][boardSize];
		for (int y = 0; y < boardSize; ++y)
			for (int x = 0; x < boardSize; ++x)
				board[y][x] = new SudokuNode(x, y, this);
		isClean = true;
		timer.start();
	}
	
	private boolean isSquare(int num) {
		int root = (int) Math.sqrt(num);
		return (root*root == num);
	}
}
