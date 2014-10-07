/*
 * Copyright 2014 Yaroslav Mytkalyk
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dnielfe.manager.preview;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public final class DrawableLruCache<T> extends LruCache<T, Drawable> {

	public DrawableLruCache() {
		super(512 * 1024);
	}

	@Override
	protected int sizeOf(T key, Drawable value) {
		if (value instanceof BitmapDrawable) {
			return ((BitmapDrawable) value).getBitmap().getByteCount() / 1024;
		} else {
			return super.sizeOf(key, value);
		}
	}
}
