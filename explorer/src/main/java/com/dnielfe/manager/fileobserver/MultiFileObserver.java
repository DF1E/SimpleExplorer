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

package com.dnielfe.manager.fileobserver;

import android.os.FileObserver;

import java.util.HashSet;
import java.util.Set;

public final class MultiFileObserver extends FileObserver {

	public interface OnEventListener {
		void onEvent(int event, String path);
	}

	public final Set<OnEventListener> listeners;
	private final String path;

	private int watchCount;

	public MultiFileObserver(String path) {
		this(path, ALL_EVENTS);
	}

	public MultiFileObserver(String path, int mask) {
		super(path, mask);
		this.path = path;
		this.listeners = new HashSet<OnEventListener>();
	}

	public void addOnEventListener(final OnEventListener listener) {
		this.listeners.add(listener);
	}

	public void removeOnEventListener(final OnEventListener listener) {
		this.listeners.remove(listener);
	}

	public String getPath() {
		return this.path;
	}

	@Override
	public void onEvent(final int event, final String pathStub) {
		for (final OnEventListener listener : this.listeners) {
			listener.onEvent(event, this.path);
		}
	}

	@Override
	public synchronized void startWatching() {
		super.startWatching();
		this.watchCount++;
	}

	@Override
	public synchronized void stopWatching() {
		if (--this.watchCount <= 0) {
			if (this.watchCount < 0) {
				this.watchCount = 0;
			}
			super.stopWatching();
		}
	}
}
