/*
 * Copyright (C) 2013 Simple Explorer
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

import java.io.File;
import java.text.DateFormat;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import com.dnielfe.manager.FileUtils;
import com.dnielfe.manager.R;

public class FileProperties extends AsyncTask<String, Void, String> {

	final int KB = 1024;
	final int MG = KB * KB;
	final int GB = MG * KB;

	long size = 0;

	private String mDisplaySize;
	private File file3;
	private Context mContext;

	private TextView mNameLabel, mPathLabel, mTimeLabel, mSizeLabel,
			mPermissionLabel, mMD5Label;

	public FileProperties(Activity ac, View view, String path) {
		mNameLabel = (TextView) view.findViewById(R.id.name_label);
		mPathLabel = (TextView) view.findViewById(R.id.path_label);
		mTimeLabel = (TextView) view.findViewById(R.id.time_stamp);
		mSizeLabel = (TextView) view.findViewById(R.id.total_size);
		mPermissionLabel = (TextView) view.findViewById(R.id.permission1);
		mMD5Label = (TextView) view.findViewById(R.id.md5_summary);

		mContext = ac;

		file3 = new File(path);
	}

	protected void onPreExecute() {
		mNameLabel.setText(file3.getName());
		mPathLabel.setText(file3.getAbsolutePath());
		mPermissionLabel.setText(getFilePermissions(file3));
		mTimeLabel.setText("...");
		mSizeLabel.setText("...");
		mMD5Label.setText("...");
	}

	protected String doInBackground(String... vals) {

		DateFormat dateFormat = android.text.format.DateFormat
				.getDateFormat(mContext.getApplicationContext());
		DateFormat timeFormat = android.text.format.DateFormat
				.getTimeFormat(mContext.getApplicationContext());

		mTimeLabel.setText(dateFormat.format(file3.lastModified()) + " "
				+ timeFormat.format(file3.lastModified()));

		if (!file3.canRead()) {
			mDisplaySize = "---";
			return mDisplaySize;
		}

		if (file3.isFile()) {
			size = file3.length();
		} else {
			size = FileUtils.getDirSize(file3.getPath());
		}

		if (size > GB)
			mDisplaySize = String.format("%.2f GB", (double) size / GB);
		else if (size < GB && size > MG)
			mDisplaySize = String.format("%.2f MB", (double) size / MG);
		else if (size < MG && size > KB)
			mDisplaySize = String.format("%.2f KB", (double) size / KB);
		else
			mDisplaySize = String.format("%.2f B", (double) size);

		return mDisplaySize;
	}

	protected void onPostExecute(String result) {
		mSizeLabel.setText(result);

		if (file3.isFile()) {
			try {
				mMD5Label.setText(MD5Checksum.getMD5Checksum(file3.getPath()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			mMD5Label.setText("-");
		}
	}

	private static String getFilePermissions(File file) {
		String per = "";

		per += file.isDirectory() ? "d" : "-";
		per += file.canRead() ? "r" : "-";
		per += file.canWrite() ? "w" : "-";
		per += file.canExecute() ? "x" : "-";

		return per;
	}
}