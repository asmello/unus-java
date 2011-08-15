public class CIntQueue {
	private IntNode head;
	private IntNode current;
	private IntNode last;
	
	CIntQueue() {
		head = new IntNode(GSudokuBoard.NULL);
		current = head;
		last = head;
	}
	
	public void add(int value) {
		IntNode newNode = new IntNode(value);
		last.next = newNode;
		last = last.next;
	}
	
	public int getNext() {
		current = current.next;
		if (current == null) current = head;
		return current.value;
	}
	
	public void resetIterator() { current = head; }
}
