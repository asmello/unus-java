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
 * This is an enumeration of codes for internal use in the program. Can represent an
 * error, state or other needed response.
 */
public enum Codes {
	NULL,
	SUCCESS,
	NO_TASK,
	ERR_OUT_OF_RANGE,
	ERR_INVALID_SIZE,
	ERR_JAGGED_BOARD,
	ERR_NO_SYMBOLS,
	ERR_READ,
	ERR_PARSING,
	STATE_IDLE,
	STATE_CONFLICT,
	STATE_WORKING,
	STATE_SUCCESS,
	STATE_CANCELLED,
	STATE_FAILED,
	STAGE_LOGICAL_DEDUCTION,
	STAGE_TRIAL_AND_ERROR
}
