import java.util.List;
import java.util.Queue;

import javax.swing.SwingWorker;

public class GSudokuWorker extends SwingWorker<Boolean, Long> {
	private long steps;
	private int boardSize;
	private int[][] board;
	private Queue<Integer>[][] posValues;
	private boolean[][] fixedPos;
	private GSudokuBoard sudoku;
	private GSudokuContainer container;
	
	GSudokuWorker(GSudokuBoard su, int[][] board,
			Queue<Integer>[][] posValues, boolean[][] fixedPos)
	{
		sudoku = su;
		container = su.getBoardContainer();
		boardSize = board.length;
		this.board = board;
		this.fixedPos = fixedPos;
		this.posValues = posValues;
	}
	
	public Boolean doInBackground() { return solve(0, 0); }
	
	public void done() { container.setDone(); }
	
	public void process (List<Long> chunks) {
		for (long number : chunks)
			container.setStepCount(number);
	}
	
	private boolean solve (int row, int column) {
		if (Thread.currentThread().isInterrupted())
			return true;
		
		if (column >= boardSize) {
			if (solve(row + 1, 0)) return true;
			return false;
		}
		
		if (row >= boardSize) {
			return true;
		}
		
		if (fixedPos[row][column]) {
			if (solve(row, column + 1)) return true;
			return false;
		}
		
		for (int number : posValues[row][column]) {
			publish(++steps);
			if (sudoku.isValid(number, row, column)) {
				board[row][column] = number;
				if (solve(row, column + 1)) return true;
				board[row][column] = GSudokuBoard.NULL;
			}
		}
		
		return false;
	}
}
