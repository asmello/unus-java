import javax.swing.*;
import javax.swing.text.*;

@SuppressWarnings("serial")
public class JLimitedTextField extends PlainDocument {
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
    		return '\0';
    	return charTable.charAt(num);
    }
    
    private boolean validate (char ch) {
    	for (int i = min; i < max; ++i)
    		if (ch == charTable.charAt(i)) return true;
    	return false;
    }
}

