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

/**
 * A simple queue-like linked list for storing integers.
 */
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
