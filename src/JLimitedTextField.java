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

import javax.swing.*;
import javax.swing.text.*;

public class JLimitedTextField extends PlainDocument {
	private static final long serialVersionUID = -3846912517390711715L;
	public static final String charTable =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final int MIN = 0;
	public static final int MAX = 36;
	public static final int ALL = 0;
	public static final int NUM = 1;
	public static final int CHAR = 2;
	public static final int POS = 3;
	private int limit;
	private int max, min;
    
    JLimitedTextField(int limit, int type) {
    	super();
    	this.limit = limit;
    	switch (type) {
    	case 0:
    		min = 0;
    		max = 36;
    		break;
    	case 1:
    		min = 0;
    		max = 10;
    		break;
    	case 2:
    		min = 10;
    		max = 36;
    		break;
    	case 3:
    		min = 1;
    		max = 10;
    		break;
    	default:
    		min = 0;
    		max = 36;
    		break;
    	}
    }
    
    public JTextField createField () {
    	JTextField field = new JTextField(limit);
    	field.setDocument(this);
    	return field;
    }
    
    public void insertString (int offset, String  str, AttributeSet attr) 
    		throws BadLocationException 
    {
    	if (str == null) return;
        
        if ((getLength() + str.length()) <= limit) {
        	str = str.toUpperCase();
        	for (int i = 0; i < str.length(); ++i)
        		if (!validate(str.charAt(i))) return;
            super.insertString(offset, str, attr);
        }
    }
    
    public static int getValue (char ch, int min, int max) {
    	if (min < MIN || max > MAX)
    		throw new IllegalArgumentException("* Out of range!");
    	ch = Character.toUpperCase(ch);
    	for (int i = min; i < max; ++i)
			if (charTable.charAt(i) == ch) return i;
		return -1;
    }
    
    public static char getChar (int num) {
    	if (num > JLimitedTextField.MAX || num < 0)
    		return ' ';
    	return charTable.charAt(num);
    }
    
    private boolean validate (char ch) {
    	for (int i = min; i < max; ++i)
    		if (ch == charTable.charAt(i)) return true;
    	return false;
    }
}

