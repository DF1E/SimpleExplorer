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

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public final class FileObserverCache {

	private static FileObserverCache instance;

	@NotNull
	public static FileObserverCache getInstance() {
		if (instance == null) {
			instance = new FileObserverCache();
		}
		return instance;
	}

	private final Map<String, WeakReference<MultiFileObserver>> cache;

	private FileObserverCache() {
		this.cache = new HashMap<String, WeakReference<MultiFileObserver>>();
	}

	public void clear() {
		this.cache.clear();
	}

	@NotNull
	public MultiFileObserver getOrCreate(final String path) {
		final WeakReference<MultiFileObserver> reference = cache.get(path);
		MultiFileObserver observer;
		if (reference != null && (observer = reference.get()) != null) {
			return observer;
		} else {
			observer = new MultiFileObserver(path);
			this.cache
					.put(path, new WeakReference<MultiFileObserver>(observer));
		}
		return observer;
	}
}
