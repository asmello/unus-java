
public class RoundQueue {
	private IntNode first;
	private IntNode iterator;
	
	RoundQueue() {
		first = new IntNode(GSudokuBoard.NULL);
		iterator = first;
	}
	
	public void add(int value) {
		if (value < 0) throw new
				IllegalArgumentException("* Value cannot be negative.");
		IntNode lastNode = first;
		while (lastNode.next != null) lastNode = lastNode.next;
		lastNode.next = new IntNode(value);
	}
	
	public int get() {
		iterator = iterator.next;
		if (iterator == null) iterator = first;
		int value = iterator.value;
		return value;
	}
	
	public void reset() {
		iterator = first;
	}
}
