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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.StatFs;

/**
 * Backward compatible version of {@link android.os.StatFs}
 */
public final class StatFsCompat {

    /**
     * StatFs instance
     */
    private final StatFs mStatFs;

    /**
     * Construct a new StatFs for looking at the stats of the filesystem at
     * {@code path}. Upon construction, the stat of the file system will be
     * performed, and the values retrieved available from the methods on this
     * class.
     *
     * @param path path in the desired file system to stat.
     */
    public StatFsCompat(final String path) {
        this.mStatFs = new StatFs(path);
    }

    /**
     * The number of blocks that are free on the file system and available to
     * applications. This corresponds to the Unix {@code statvfs.f_bavail}
     * field.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public long getAvailableBlocksLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getAvailableBlocksLong();
        } else {
            return mStatFs.getAvailableBlocks();
        }
    }

    /**
     * The total number of blocks on the file system. This corresponds to the
     * Unix {@code statvfs.f_blocks} field.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public long getBlockCountLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getBlockCountLong();
        } else {
            return mStatFs.getBlockCount();
        }
    }

    /**
     * The size, in bytes, of a block on the file system. This corresponds to
     * the Unix {@code statvfs.f_bsize} field.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public long getBlockSizeLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getBlockSizeLong();
        } else {
            return mStatFs.getBlockSize();
        }
    }

    /**
     * The total number of blocks that are free on the file system, including
     * reserved blocks (that are not available to normal applications). This
     * corresponds to the Unix {@code statvfs.f_bfree} field. Most applications
     * will want to use {@link #getAvailableBlocksLong()} instead.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public long getFreeBlocksLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getFreeBlocksLong();
        } else {
            return mStatFs.getFreeBlocks();
        }
    }

    /**
     * The number of bytes that are free on the file system and available to
     * applications.
     */
    @SuppressLint("NewApi")
    public long getAvailableBytes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getAvailableBytes();
        } else {
            return getAvailableBlocksLong() * getBlockSizeLong();
        }
    }

    /**
     * The number of bytes that are free on the file system, including reserved
     * blocks (that are not available to normal applications). Most applications
     * will want to use {@link #getAvailableBytes()} instead.
     */
    @SuppressLint("NewApi")
    public long getFreeBytes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getFreeBytes();
        } else {
            return getFreeBlocksLong() * getBlockSizeLong();
        }
    }

    /**
     * The total number of bytes supported by the file system.
     */
    public long getTotalBytes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return mStatFs.getTotalBytes();
        } else {
            return getBlockCountLong() * getBlockSizeLong();
        }
    }

    /**
     * Perform a restat of the file system referenced by this object. This is
     * the same as re-constructing the object with the same file system path,
     * and the new stat values are available upon return.
     */
    public void restat(final String path) {
        mStatFs.restat(path);
    }
}