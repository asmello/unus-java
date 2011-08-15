public class SudokuNode {
	private int value;
	private int subGroupX;
	private int subGroupY;
	private int posX;
	private int posY;
	private boolean isFixed;
	private boolean isConflicting;
	private RoundQueue possibleValues;

	SudokuNode(int posX, int posY, GSudokuBoard board) {
		value = GSudokuBoard.NULL;
		this.posX = posX;
		this.posY = posY;
		subGroupX = (posX / board.getSubSize()) * board.getSubSize();
		subGroupY = (posY / board.getSubSize()) * board.getSubSize();
		isFixed = false;
		isConflicting = false;

		possibleValues = new RoundQueue();
		for (int i = board.getMinValue(); i < board.getMinValue()
				+ board.getBoardSize(); ++i)
			possibleValues.add(i);
	}

	public void setValue(int value) {
		this.value = value;
	}

	public void setNull() {
		value = GSudokuBoard.NULL;
	}

	public void cleanPossibleValues() {
		possibleValues = new RoundQueue();
	}
	
	public void resetPossibleValuesIterator() {
		possibleValues.reset();
	}

	public void addPossibleValue(int value) {
		possibleValues.add(value);
	}

	public int getNextPossibleValue() {
		return possibleValues.get();
	}

	public void setFixed(boolean state) {
		isFixed = state;
	}

	public void setConflict(boolean state) {
		isConflicting = state;
	}

	public int getValue() {
		return value;
	}
	
	public int getPosX() {
		return posX;
	}

	public int getPosY() {
		return posY;
	}

	public int getSubX() {
		return subGroupX;
	}

	public int getSubY() {
		return subGroupY;
	}

	public boolean isConflicting() {
		return isConflicting;
	}

	public boolean isFixed() {
		return isFixed;
	}

	public boolean isNull() {
		return (value == GSudokuBoard.NULL);
	}

	public boolean sameValue(SudokuNode node) {
		if (node != this && !node.isNull() && node.getValue() == value)
			return true;
		return false;
	}
}
