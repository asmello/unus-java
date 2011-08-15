import javax.swing.SwingWorker;

public class GSudokuWorker extends SwingWorker<Boolean, Object> {
	private GSudokuBoard sudoku;
	
	GSudokuWorker(GSudokuBoard su) { sudoku = su; }
	
	public Boolean doInBackground() { return solve(0, 0); }
	
	public void done() {
		try { sudoku.setDone(get()); }
		catch (Exception e) { sudoku.setDone(false); }
	}
	
	private boolean solve (int x, int y) {
		if (Thread.currentThread().isInterrupted()) 
			return true;
		
		if (x >= sudoku.getBoardSize()) {
			if (solve(0, y + 1)) return true;
			return false;
		}
		
		if (y >= sudoku.getBoardSize()) 
			return true;
		
		if (sudoku.isFixed(x, y)) {
			if (solve(x + 1, y)) return true;
			return false;
		}
		
		int next = sudoku.getNextValue(x, y);
		while (next != GSudokuBoard.NULL) {
			if (sudoku.isValid(x, y, next)) {
				sudoku.insert(x, y, next);
				if (solve(x + 1, y)) return true;
				sudoku.insert(x, y, GSudokuBoard.NULL);
			}
			next = sudoku.getNextValue(x, y);
		}
		
		return false;
	}
}
