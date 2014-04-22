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

package com.dnielfe.manager.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.dnielfe.manager.R;
import com.dnielfe.manager.SimpleExplorer;

public class BackupTask extends AsyncTask<File, Void, Boolean> {

	private static final int SET_PROGRESS = 01;
	private static final int FINISH_PROGRESS = 02;

	private ProgressDialog mDialog = null;
	private ArrayList<ApplicationInfo> mDataSource;
	private PackageManager mPackMag;
	private Activity mActivity;
	private static final int BUFFER = 2048;
	private byte[] mData;

	public BackupTask(Activity a, PackageManager p,
			ArrayList<ApplicationInfo> data) {
		mActivity = a;
		mPackMag = p;
		mDataSource = data;
		mData = new byte[BUFFER];

		File d = new File(SimpleExplorer.BACKUP_LOC);
		// create directory if needed
		if (!d.exists())
			d.mkdirs();
	}

	@Override
	public void onPreExecute() {
		mDialog = ProgressDialog.show(mActivity,
				mActivity.getString(R.string.backup), "", true, false);
	}

	@Override
	protected Boolean doInBackground(File... arg0) {
		BufferedInputStream mBuffIn;
		BufferedOutputStream mBuffOut;
		Message msg;
		int len = mDataSource.size();
		int read;

		for (int i = 0; i < len; i++) {
			ApplicationInfo info = mDataSource.get(i);
			String source_dir = info.sourceDir;
			String out_file = info.loadLabel(mPackMag).toString() + ".apk";
			try {
				mBuffIn = new BufferedInputStream(new FileInputStream(
						source_dir));
				mBuffOut = new BufferedOutputStream(new FileOutputStream(
						SimpleExplorer.BACKUP_LOC + out_file));

				while ((read = mBuffIn.read(mData, 0, BUFFER)) != -1)
					mBuffOut.write(mData, 0, read);

				mBuffOut.flush();
				mBuffIn.close();
				mBuffOut.close();

				msg = new Message();
				msg.what = SET_PROGRESS;
				msg.obj = i + mActivity.getString(R.string.of) + len
						+ mActivity.getString(R.string.backedup);
				mHandler.sendMessage(msg);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mHandler.sendEmptyMessage(FINISH_PROGRESS);
	}

	// this handler will update the GUI from this background thread.
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SET_PROGRESS:
				mDialog.setMessage((String) msg.obj);
				return;
			case FINISH_PROGRESS:
				Toast.makeText(mActivity,
						mActivity.getString(R.string.backupcomplete),
						Toast.LENGTH_SHORT).show();
				break;
			}

			mDialog.cancel();
		}
	};
}