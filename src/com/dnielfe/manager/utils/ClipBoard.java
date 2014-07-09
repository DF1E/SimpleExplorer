/*
 * Copyright (C) 2014 Simple Explorer
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.dnielfe.manager.utils;

/**
 * Holds references to copied or moved to clipboard files
 */
public final class ClipBoard {

	private static final Object LOCK = new Object();

	/**
	 * Files in clip board
	 */
	private static String[] clipBoard;

	/**
	 * True if move, false if cutCopy
	 */
	private static boolean isMove;

	/**
	 * If true, clipBoard can't be modified
	 */
	private static volatile boolean isLocked;

	private ClipBoard() {
	}

	public static synchronized boolean cutCopy(String[] files) {
		synchronized (LOCK) {
			if (!isLocked) {
				isMove = false;
				clipBoard = files;
				return true;
			} else {
				return false;
			}
		}
	}

	public static synchronized boolean cutMove(String[] files) {
		synchronized (LOCK) {
			if (!isLocked) {
				isMove = true;
				clipBoard = files;
				return true;
			} else {
				return false;
			}
		}
	}

	public static void lock() {
		synchronized (LOCK) {
			isLocked = true;
		}
	}

	public static void unlock() {
		synchronized (LOCK) {
			isLocked = false;
		}
	}

	public static String[] getClipBoardContents() {
		synchronized (LOCK) {
			return clipBoard;
		}
	}

	public static boolean isEmpty() {
		synchronized (LOCK) {
			return clipBoard == null;
		}
	}

	public static boolean isMove() {
		synchronized (LOCK) {
			return isMove;
		}
	}

	public static boolean clear() {
		synchronized (LOCK) {
			if (!isLocked) {
				clipBoard = null;
				isMove = false;
				return true;
			} else {
				return false;
			}
		}
	}
}
