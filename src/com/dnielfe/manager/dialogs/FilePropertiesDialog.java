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

package com.dnielfe.manager.dialogs;

import java.io.File;
import java.text.DateFormat;

import org.jetbrains.annotations.NotNull;

import com.dnielfe.manager.FileUtils;
import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.MD5Checksum;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public final class FilePropertiesDialog extends DialogFragment {

	private Activity activity;
	private static String filePath;
	private static File file3;
	private TextView mNameLabel, mPathLabel, mTimeLabel, mSizeLabel,
			mPermissionLabel, mMD5Label;

	public static final String EXTRA_FILE = null;

	public static DialogFragment instantiate(String file) {
		final Bundle extras = new Bundle();
		extras.putString(EXTRA_FILE, file);

		file3 = new File(file);

		final FilePropertiesDialog dialog = new FilePropertiesDialog();
		dialog.setArguments(extras);

		return dialog;
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		final Bundle extras = this.getArguments();
		filePath = extras.getString(EXTRA_FILE);
	}

	@Override
	public Dialog onCreateDialog(Bundle state) {
		activity = getActivity();

		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(getString(R.string.details));
		builder.setNeutralButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		final View content = activity.getLayoutInflater().inflate(
				R.layout.details, null);
		this.initView(content);
		builder.setView(content);

		return builder.create();
	}

	private void initView(@NotNull final View view) {
		mNameLabel = (TextView) view.findViewById(R.id.name_label);
		mPathLabel = (TextView) view.findViewById(R.id.path_label);
		mTimeLabel = (TextView) view.findViewById(R.id.time_stamp);
		mSizeLabel = (TextView) view.findViewById(R.id.total_size);
		mPermissionLabel = (TextView) view.findViewById(R.id.permission1);
		mMD5Label = (TextView) view.findViewById(R.id.md5_summary);

		new BackgroundWork().execute(filePath);
	}

	private class BackgroundWork extends AsyncTask<String, Void, String> {
		final int KB = 1024;
		final int MG = KB * KB;
		final int GB = MG * KB;
		long size = 0;
		private String mDisplaySize;
		FileUtils flmg = new FileUtils();

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
					.getDateFormat(activity.getApplicationContext());
			DateFormat timeFormat = android.text.format.DateFormat
					.getTimeFormat(activity.getApplicationContext());

			mTimeLabel.setText(dateFormat.format(file3.lastModified()) + " "
					+ timeFormat.format(file3.lastModified()));

			if (!file3.canRead()) {
				mDisplaySize = "---";
				return mDisplaySize;
			}

			if (file3.isFile()) {
				size = file3.length();
			} else {
				size = flmg.getDirSize(vals[0]);
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
					mMD5Label.setText(MD5Checksum.getMD5Checksum(file3
							.getPath()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				mMD5Label.setText("-");
			}
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
