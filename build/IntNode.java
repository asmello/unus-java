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
 * This class represents a simple linked integer node.
 */
public final class IntNode {
	public int value;
	public IntNode next;
	
	IntNode(int value) { this.value = value; }
}
