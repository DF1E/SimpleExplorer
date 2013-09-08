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

import java.io.File;
import java.text.SimpleDateFormat;

import android.os.Bundle;
import android.os.AsyncTask;
import android.content.Intent;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InfoDialog extends Activity {
	private String infopath;
	private TextView mNameLabel, mPathLabel, mTimeLabel, mSizeLabel,
			mPermissionLabel;
	private File file3;

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

	private class BackgroundWork extends AsyncTask<String, Void, Long> {
		final int KB = 1024;
		final int MG = KB * KB;
		final int GB = MG * KB;
		long size = 0;
		private String mDisplaySize;
		private String mPermissions;
		FileUtils flmg = new FileUtils();

		protected void onPreExecute() {
			mNameLabel.setText(file3.getName());
		}

		protected Long doInBackground(String... vals) {

			mPermissions = getFilePermissions(file3);

			if (!file3.canRead())
				mDisplaySize = "---";

			if (file3.isFile()) {
				double size = file3.length();
				if (size > GB)
					mDisplaySize = String.format("%.2f GB", (double) size / GB);
				else if (size < GB && size > MG)
					mDisplaySize = String.format("%.2f MB", (double) size / MG);
				else if (size < MG && size > KB)
					mDisplaySize = String.format("%.2f KB", (double) size / KB);
				else
					mDisplaySize = String.format("%.2f B", (double) size);

			} else {
				size = flmg.getDirSize(vals[0]);

				if (size > GB)
					mDisplaySize = String.format("%.2f GB", (double) size / GB);
				else if (size < GB && size > MG)
					mDisplaySize = String.format("%.2f MB", (double) size / MG);
				else if (size < MG && size > KB)
					mDisplaySize = String.format("%.2f KB", (double) size / KB);
				else
					mDisplaySize = String.format("%.2f B", (double) size);
			}

			return size;
		}

		protected void onPostExecute(Long result) {
			SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

			mPathLabel.setText(file3.getAbsolutePath());
			mTimeLabel.setText(sdf1.format(file3.lastModified()));
			mSizeLabel.setText(mDisplaySize);
			mPermissionLabel.setText(mPermissions);
		}
	}

	public static String getFilePermissions(File file) {
		String per = "";

		per += file.isDirectory() ? "d" : "-";
		per += file.canRead() ? "r" : "-";
		per += file.canWrite() ? "w" : "-";

		return per;
	}
}