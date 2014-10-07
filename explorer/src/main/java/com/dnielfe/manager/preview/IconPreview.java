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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.dnielfe.manager.R;
import com.dnielfe.manager.settings.Settings;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconPreview {

    private static ConcurrentMap<String, Bitmap> cache;
    private static DrawableLruCache<String> mMimeTypeIconCache;
    private static ExecutorService pool = null;
    private static Map<ImageView, String> imageViews = Collections
            .synchronizedMap(new ConcurrentHashMap<ImageView, String>());
    private static PackageManager pm;
    private static int mSize = 64;

    private static Context mContext;
    private static Resources mResources;

    public IconPreview(Activity activity) {
        mContext = activity;

        cache = new ConcurrentHashMap<String, Bitmap>();
        pool = Executors.newFixedThreadPool(5);
        mResources = activity.getResources();
        pm = mContext.getPackageManager();

        if (mMimeTypeIconCache == null) {
            mMimeTypeIconCache = new DrawableLruCache<String>();
        }
    }

    public static void getFileIcon(File file, final ImageView icon) {
        if (Settings.showThumbnail() & isvalidMimeType(file)) {
            icon.setTag(file.getAbsolutePath());
            loadBitmap(file, icon);
        } else {
            loadFromRes(file, icon);
        }
    }

    private static boolean isvalidMimeType(File file) {
        boolean isImage = MimeTypes.isPicture(file);
        boolean isVideo = MimeTypes.isVideo(file);
        boolean isApk = file.getName().endsWith(".apk");

        return isImage || isVideo || isApk;
    }

    private static void loadFromRes(final File file, final ImageView icon) {
        Drawable mimeIcon = null;

        if (file != null && file.isDirectory()) {
            String[] files = file.list();
            if (file.canRead() && files != null && files.length > 0)
                mimeIcon = mResources.getDrawable(R.drawable.type_folder);
            else
                mimeIcon = mResources.getDrawable(R.drawable.type_folder_empty);
        } else if (file != null && file.isFile()) {
            final String fileExt = FilenameUtils.getExtension(file.getName());
            mimeIcon = mMimeTypeIconCache.get(fileExt);

            if (mimeIcon == null) {
                final int mimeIconId = MimeTypes.getIconForExt(fileExt);
                if (mimeIconId != 0) {
                    mimeIcon = mResources.getDrawable(mimeIconId);
                    mMimeTypeIconCache.put(fileExt, mimeIcon);
                }
            }
        }

        if (mimeIcon != null) {
            icon.setImageDrawable(mimeIcon);
        } else {
            // default icon
            icon.setImageResource(R.drawable.type_unknown);
        }
    }

    private static Bitmap getBitmapFromCache(String url) {
        if (cache.containsKey(url)) {
            return cache.get(url);
        }

        return null;
    }

    private static void queueJob(final File uri, final ImageView imageView) {
        /* Create handler in UI thread. */
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tag = imageViews.get(imageView);
                if (tag != null && tag.equals(uri.getAbsolutePath())) {
                    if (msg.obj != null) {
                        imageView.setImageBitmap((Bitmap) msg.obj);
                    } else {
                        imageView.setImageBitmap(null);
                    }
                }
            }
        };

        pool.submit(new Runnable() {
            public void run() {
                final Bitmap bmp = getPreview(uri);
                Message message = Message.obtain();
                message.obj = bmp;

                handler.sendMessage(message);
            }
        });
    }

    private static void loadBitmap(final File file, final ImageView imageView) {
        imageViews.put(imageView, file.getAbsolutePath());
        Bitmap bitmap = getBitmapFromCache(file.getAbsolutePath());

        // check in UI thread, so no concurrency issues
        if (bitmap != null) {
            // Item loaded from cache
            imageView.setImageBitmap(bitmap);
        } else {
            // here you can set a placeholder
            imageView.setImageBitmap(null);
            queueJob(file, imageView);
        }
    }

    private static Bitmap getPreview(File file) {
        final boolean isImage = MimeTypes.isPicture(file);
        final boolean isVideo = MimeTypes.isVideo(file);
        final boolean isApk = file.getName().endsWith(".apk");
        Bitmap mBitmap = null;
        String path = file.getAbsolutePath();

        if (isImage) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(path, o);
            o.inJustDecodeBounds = false;

            if (o.outWidth != -1 && o.outHeight != -1) {
                final int originalSize = (o.outHeight > o.outWidth) ? o.outWidth
                        : o.outHeight;
                o.inSampleSize = originalSize / mSize;
            }

            mBitmap = BitmapFactory.decodeFile(path, o);

            cache.put(path, mBitmap);
            return mBitmap;
        } else if (isVideo) {
            mBitmap = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Video.Thumbnails.MICRO_KIND);

            cache.put(path, mBitmap);
            return mBitmap;
        } else if (isApk) {
            final PackageInfo packageInfo = pm.getPackageArchiveInfo(path,
                    PackageManager.GET_ACTIVITIES);

            if (packageInfo != null) {
                final ApplicationInfo appInfo = packageInfo.applicationInfo;

                if (appInfo != null) {
                    appInfo.sourceDir = path;
                    appInfo.publicSourceDir = path;
                    final Drawable icon = appInfo.loadIcon(pm);

                    if (icon != null) {
                        mBitmap = ((BitmapDrawable) icon).getBitmap();
                    }
                }
            } else {
                // load apk icon from /res/drawable/..
                mBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                        R.drawable.type_apk);
            }

            cache.put(path, mBitmap);
            return mBitmap;
        }
        return null;
    }
}