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

package com.dnielfe.manager.adapters;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import com.dnielfe.manager.Browser;
import com.dnielfe.manager.R;
import com.dnielfe.manager.preview.DrawableLruCache;
import com.dnielfe.manager.preview.IconPreview;
import com.dnielfe.manager.preview.MimeTypes;
import com.dnielfe.manager.settings.Settings;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BrowserListAdapter extends ArrayAdapter<String> {
	private Context mContext;
	private Resources mResources;
	private ArrayList<String> mDataSource;

	private DrawableLruCache<Integer> mDrawableLruCache;
	private DrawableLruCache<String> mMimeTypeIconCache;

	public BrowserListAdapter(@NotNull final Context context,
			ArrayList<String> data) {
		super(context, R.layout.item_browserlist, data);

		this.mContext = context;
		this.mDataSource = data;
		this.mResources = context.getResources();

		if (mDrawableLruCache == null) {
			mDrawableLruCache = new DrawableLruCache<Integer>();
		}
		if (mMimeTypeIconCache == null) {
			mMimeTypeIconCache = new DrawableLruCache<String>();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder mViewHolder;
		int num_items = 0;
		final File file = new File(Browser.mCurrentPath + "/"
				+ this.getItem(position));
		String[] list = file.list();

		if (list != null)
			// get number of files in directory
			num_items = list.length;

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.item_browserlist, parent,
					false);
			mViewHolder = new ViewHolder(convertView);
			convertView.setTag(mViewHolder);
		} else {
			mViewHolder = (ViewHolder) convertView.getTag();
		}

		if (Settings.viewmode > 0) {
			mViewHolder.dateview.setVisibility(TextView.VISIBLE);
		} else {
			mViewHolder.dateview.setVisibility(TextView.GONE);
		}

		setIcon(file, mViewHolder.icon);

		// Shows the size of File
		if (file.isFile()) {
			long size = file.length();

			mViewHolder.bottomView.setText(FileUtils
					.byteCountToDisplaySize(size));
		} else {
			// Shows the number of Files in Folder
			String s = mContext.getString(R.string.files);
			mViewHolder.bottomView.setText(num_items + s);
		}

		DateFormat dateFormat = android.text.format.DateFormat
				.getDateFormat(mContext);
		DateFormat timeFormat = android.text.format.DateFormat
				.getTimeFormat(mContext);

		mViewHolder.topView.setText(file.getName());

		mViewHolder.dateview.setText(dateFormat.format(file.lastModified())
				+ " " + timeFormat.format(file.lastModified()));

		return convertView;
	}

	@Override
	public String getItem(int pos) {
		return mDataSource.get(pos);
	}

	private final void setIcon(final File file, final ImageView icon) {
		final boolean isImage = MimeTypes.isPicture(file);
		final boolean isVideo = MimeTypes.isVideo(file);
		final boolean isApk = file.getName().endsWith(".apk");

		if (file != null && file.isDirectory()) {
			if (file.canRead() && file.list().length > 0)
				icon.setImageResource(R.drawable.type_folder);
			else
				icon.setImageResource(R.drawable.type_folder_empty);
		} else {
			if (Settings.showthumbnail) {
				if (isImage) {
					// IconPreview.INSTANCE.setPlaceholder(bitmap);
					icon.setTag(file.getAbsolutePath());
					IconPreview.INSTANCE.loadBitmap(file, icon);
				} else if (isVideo) {
					// IconPreview.INSTANCE.setPlaceholder(bitmap);
					icon.setTag(file.getAbsolutePath());
					IconPreview.INSTANCE.loadBitmap(file, icon);
				} else if (isApk) {
					// IconPreview.INSTANCE.setPlaceholder(bitmap);
					icon.setTag(file.getAbsolutePath());
					IconPreview.INSTANCE.loadApk(file, icon, mContext);
				} else {
					loadFromRes(file, icon);
				}
			} else {
				loadFromRes(file, icon);
			}
		}
	}

	private void loadFromRes(final File file, final ImageView icon) {
		final String fileExt = FilenameUtils.getExtension(file.getName());
		Drawable mimeIcon = mMimeTypeIconCache.get(fileExt);

		if (mimeIcon == null) {
			final int mimeIconId = MimeTypes.getIconForExt(fileExt);
			if (mimeIconId != 0) {
				mimeIcon = mResources.getDrawable(mimeIconId);
				mMimeTypeIconCache.put(fileExt, mimeIcon);
			}
		}

		if (mimeIcon != null) {
			icon.setImageDrawable(mimeIcon);
		} else {
			// default icon
			icon.setImageResource(R.drawable.type_unknown);
		}
	}

	private static class ViewHolder {
		TextView topView;
		TextView bottomView;
		TextView dateview;
		ImageView icon;

		ViewHolder(View view) {
			topView = (TextView) view.findViewById(R.id.top_view);
			bottomView = (TextView) view.findViewById(R.id.bottom_view);
			dateview = (TextView) view.findViewById(R.id.dateview);
			icon = (ImageView) view.findViewById(R.id.row_image);
		}
	}
}