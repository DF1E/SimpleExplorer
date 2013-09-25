package com.dnielfe.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Collections;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import android.widget.ImageView;

public enum IconPreview {
	INSTANCE;
	private String type = "image";

	private final ConcurrentMap<String, Bitmap> cache;
	private final ExecutorService pool;
	private Map<ImageView, String> imageViews = Collections
			.synchronizedMap(new ConcurrentHashMap<ImageView, String>());
	private Bitmap placeholder;

	IconPreview() {
		cache = new ConcurrentHashMap<String, Bitmap>();
		pool = Executors.newFixedThreadPool(5);
	}

	public void setPlaceholder(Bitmap bmp) {
		placeholder = bmp;
	}

	public void setType(String type1) {
		type = type1;
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
						// Fail
					}
				}
			}
		};

		pool.submit(new Runnable() {
			public void run() {
				final Bitmap bmp = getPreview(url);
				Message message = Message.obtain();
				message.obj = bmp;

				handler.sendMessage(message);
			}
		});
	}

	public void loadBitmap(final String url, final ImageView imageView) {
		imageViews.put(imageView, url);
		Bitmap bitmap = getBitmapFromCache(url);

		// check in UI thread, so no concurrency issues
		if (bitmap != null) {
			// Item loaded from cache
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
		Bitmap mBitmap = null;
		InputStream photoStream = null;
		File path = new File(url);
		int size = 72;

		if (type.contentEquals("image")) {

			try {
				photoStream = new FileInputStream(path);
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				opts.inSampleSize = 1;

				mBitmap = BitmapFactory.decodeStream(photoStream, null, opts);
				if (opts.outWidth > opts.outHeight && opts.outWidth > size) {
					opts.inSampleSize = opts.outWidth / size;
				} else if (opts.outWidth < opts.outHeight
						&& opts.outHeight > size) {
					opts.inSampleSize = opts.outHeight / size;
				}
				if (opts.inSampleSize < 1) {
					opts.inSampleSize = 1;
				}
				opts.inJustDecodeBounds = false;
				photoStream.close();
				photoStream = new FileInputStream(path);
				mBitmap = BitmapFactory.decodeStream(photoStream, null, opts);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (photoStream != null) {
					try {
						photoStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			cache.put(url, mBitmap);

			return mBitmap;

		} else if (type.contentEquals("video")) {

			mBitmap = ThumbnailUtils.createVideoThumbnail(
					path.getAbsolutePath(),
					MediaStore.Images.Thumbnails.MICRO_KIND);

			cache.put(url, mBitmap);

			return mBitmap;

		} else {
			return null;
		}
	}
}