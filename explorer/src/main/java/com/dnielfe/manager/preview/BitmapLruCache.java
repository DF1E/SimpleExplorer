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

package com.dnielfe.manager.preview;

import android.graphics.Bitmap;
import android.util.LruCache;

public final class BitmapLruCache<T> extends LruCache<T, Bitmap> {

    public BitmapLruCache() {
        super(512 * 1024);
    }

    @Override
    protected int sizeOf(T key, Bitmap value) {
        return value.getByteCount() / 1024;
    }
}
