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

package com.dnielfe.manager;

import com.dnielfe.manager.utils.MD5Checksum;

import java.io.File;
import java.text.DateFormat;
import android.os.Bundle;
import android.os.AsyncTask;
import android.content.Intent;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InfoDialog extends Activity {

	private static File file3;
	private String infopath;
	private TextView mNameLabel, mPathLabel, mTimeLabel, mSizeLabel,
			mPermissionLabel, mMD5Label;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.details);

		Intent i = getIntent();
		if (i != null) {
			if (i.getAction() != null
					&& i.getAction().equals(Intent.ACTION_VIEW)) {
				infopath = i.getData().getPath();

				if (infopath == null)
					infopath = "";
			} else {
				infopath = i.getExtras().getString("FILE_NAME");
			}
		}

		file3 = new File(infopath);

		mNameLabel = (TextView) findViewById(R.id.name_label);
		mPathLabel = (TextView) findViewById(R.id.path_label);
		mTimeLabel = (TextView) findViewById(R.id.time_stamp);
		mSizeLabel = (TextView) findViewById(R.id.total_size);
		mPermissionLabel = (TextView) findViewById(R.id.permission1);
		mMD5Label = (TextView) findViewById(R.id.md5_summary);

		// Set up Button
		Button button1 = (Button) findViewById(R.id.quit);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InfoDialog.this.finish();
			}
		});

		new BackgroundWork().execute(infopath);
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
					.getDateFormat(getApplicationContext());
			DateFormat timeFormat = android.text.format.DateFormat
					.getTimeFormat(getApplicationContext());

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