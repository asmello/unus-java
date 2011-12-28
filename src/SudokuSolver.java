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

import javax.swing.ImageIcon;
import com.apple.eawt.Application;

/**
 * This is the Main Class for the program. It just creates an instance of
 * GSudokuContainer, which provides the main window.
 */
public class SudokuSolver {
	public static void main (String[] args) {
		GSudokuContainer inst = new GSudokuContainer();
		ImageIcon icon = inst.createImageIcon("sudoku_icon.png");
		
		// Mac OS X needs specific aesthetic settings
		if (System.getProperty("os.name").equals("Mac OS X")) {
			Application app = Application.getApplication();
			app.setDockIconImage(icon.getImage());
		}
		
		inst.setIconImage(icon.getImage());
	}
}
