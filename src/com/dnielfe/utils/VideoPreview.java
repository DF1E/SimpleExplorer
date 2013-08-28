package com.dnielfe.utils;

import java.io.File;
import java.util.Collections;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import android.util.Log;
import android.widget.ImageView;

public enum VideoPreview {
	INSTANCE;

	private final ConcurrentMap<String, Bitmap> cache;
	private final ExecutorService pool;
	private Map<ImageView, String> imageViews = Collections
			.synchronizedMap(new ConcurrentHashMap<ImageView, String>());
	private Bitmap placeholder;

	VideoPreview() {
		cache = new ConcurrentHashMap<String, Bitmap>();
		pool = Executors.newFixedThreadPool(5);
	}

	public void setPlaceholder(Bitmap bmp) {
		placeholder = bmp;
	}

	public Bitmap getBitmapFromCache(String url) {
		if (cache.containsKey(url)) {
			return cache.get(url);
		}

		return null;
	}

	public void queueJob(final String url, final ImageView imageView) {
		/* Create handler in UI thread. */
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String tag = imageViews.get(imageView);
				if (tag != null && tag.equals(url)) {
					if (msg.obj != null) {
						imageView.setImageBitmap((Bitmap) msg.obj);
					} else {
						imageView.setImageBitmap(placeholder);
						Log.d(null, "fail " + url);
					}
				}
			}
		};

		pool.submit(new Runnable() {
			public void run() {
				final Bitmap bmp = getPreview(url);
				Message message = Message.obtain();
				message.obj = bmp;
				Log.d(null, "Item downloaded: " + url);

				handler.sendMessage(message);
			}
		});
	}

	public void loadBitmap(final String url, final ImageView imageView) {
		imageViews.put(imageView, url);
		Bitmap bitmap = getBitmapFromCache(url);

		// check in UI thread, so no concurrency issues
		if (bitmap != null) {
			Log.d(null, "Item loaded from cache: " + url);
			imageView.setImageBitmap(bitmap);
		} else {
			imageView.setImageBitmap(placeholder);
			queueJob(url, imageView);
		}
	}

	public void clearCache() {
		cache.clear();
	}

	private Bitmap getPreview(String url) {
		File path = new File(url);
		Bitmap mBitmap = null;

		mBitmap = ThumbnailUtils.createVideoThumbnail(path.getAbsolutePath(),
				MediaStore.Images.Thumbnails.MINI_KIND);

		cache.put(url, mBitmap);

		return mBitmap;
	}
}