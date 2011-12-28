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

/** 
 * This class provides a custom JTextField which allows only alphanumerical input.
 * It also provides utility methods for conversion of strings into adequate values.
 */
public class JLimitedTextField extends PlainDocument {
	// CONSTANTS
	private static final long serialVersionUID = -3846912517390711715L;
	public static final String charTable =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final int MIN = 0;
	public static final int MAX = 36;
	// Acceptable types
	public static final int ALL = 0;
	public static final int NUM = 1;
	public static final int CHAR = 2;
	public static final int POS = 3;
	
	// VARIABLES
	private int limit;
	private int max, min;
    
	/**
	 * @param limit limit of characters
	 * @param type a type constant
	 */
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
    
    /**
     * Creates a configured JTextField and returns it.
     * @return a configured JTextField
     */
    public JTextField createField () {
    	JTextField field = new JTextField(limit);
    	field.setDocument(this);
    	return field;
    }
    
    // Extension for insertString (checks for valid input). Case insensitive.
    public void insertString (int offset, String  str, AttributeSet attr) 
    		throws BadLocationException 
    {
    	if (str == null) return;
        
        if ((getLength() + str.length()) <= limit) {
        	str = str.toUpperCase(); // Make case-insensitive
        	for (int i = 0; i < str.length(); ++i)
        		if (!validate(str.charAt(i))) return;
            super.insertString(offset, str, attr);
        }
    }
    
    /** 
     * Returns proper numerical value for a char, given needed range.
     * If no valid conversion can be made, returns GSudokuBoard.NULL.
     * @param ch character to convert
     * @param min minimum valid value
     * @param max maximum valid value
     * @return the correct numerical representation in the range
     */
    public static int getValue (char ch, int min, int max) {
    	if (min < MIN || max > MAX)
    		throw new IllegalArgumentException("* Out of range!");
    	ch = Character.toUpperCase(ch); // Make case-insensitive
    	for (int i = min; i < max; ++i)
			if (charTable.charAt(i) == ch) return i;
		return GSudokuBoard.NULL;
    }
    
    /**
     * Returns proper character representation for given numerical value.
     * If no valid conversion can be made, returns a space character.
     * @param num value to convert
     * @return the correct alphanumerical representation of the value
     */
    public static char getChar (int num) {
    	if (num > JLimitedTextField.MAX || num < 0)
    		return ' ';
    	return charTable.charAt(num);
    }
    
    /**
     * Checks if a character is an alphanumerical in range.
     * @param ch character to check
     * @return validation result
     */
    private boolean validate (char ch) {
    	for (int i = min; i < max; ++i)
    		if (ch == charTable.charAt(i)) return true;
    	return false;
    }
}

