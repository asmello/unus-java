import javax.swing.ImageIcon;
import com.apple.eawt.Application;

public class SudokuSolver {
	public static void main (String[] args) {
		GSudokuContainer inst = new GSudokuContainer();
		ImageIcon icon = inst.createImageIcon("sudoku_icon.png");
		
		if (System.getProperty("os.name").equals("Mac OS X")) {
			Application app = Application.getApplication();
			app.setDockIconImage(icon.getImage());
		}
		
		inst.setIconImage(icon.getImage());
	}
}
