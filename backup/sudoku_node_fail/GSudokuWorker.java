import javax.swing.SwingWorker;

public class GSudokuWorker extends SwingWorker<Boolean, Long> {
	private SudokuNode[][] board;
	private GSudokuBoard sudoku;
	private GSudokuContainer container;
	
	GSudokuWorker(GSudokuBoard su, SudokuNode[][] board)
	{
		sudoku = su;
		container = su.getBoardContainer();
		this.board = board;
	}
	
	public Boolean doInBackground() { return solve(0, 0); }
	
	public void done() { container.setDone(); }
	
	private boolean solve (int x, int y) {
		if (Thread.currentThread().isInterrupted())
			return true;
		
		if (x >= sudoku.getBoardSize()) {
			if (solve(0, y + 1)) return true;
			return false;
		}
		
		if (y >= sudoku.getBoardSize()) {
			return true;
		}
		
		if (board[y][x].isFixed()) {
			if (solve(x + 1, y)) return true;
			return false;
		}
		
		int number = board[y][x].getNextPossibleValue();
		while (number != GSudokuBoard.NULL) {
			if (sudoku.isValid(number, board[y][x])) {
				board[y][x].setValue(number);
				if (solve(x + 1, y)) return true;
				board[y][x].setNull();
			}
			number = board[y][x].getNextPossibleValue();
		}
		
		return false;
	}
}
