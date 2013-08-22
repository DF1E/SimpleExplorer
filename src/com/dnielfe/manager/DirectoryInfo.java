package com.dnielfe.manager;

import java.io.File;
import java.text.SimpleDateFormat;

import android.os.Bundle;
import android.os.AsyncTask;
import android.os.StatFs;
import android.os.Environment;
import android.content.Intent;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DirectoryInfo extends Activity {
	private static final int KB = 1024;
	private static final int MG = KB * KB;
	private static final int GB = MG * KB;
	private String mPathName;
	private TextView mNameLabel, mPathLabel, mDirLabel, mFileLabel, mTimeLabel,
			mUsedLabel, mAvaibleLabel, mFreeLabel;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.folderinfo);

		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);

		Intent i = getIntent();
		if (i != null) {
			if (i.getAction() != null
					&& i.getAction().equals(Intent.ACTION_VIEW)) {
				mPathName = i.getData().getPath();

				if (mPathName == null)
					mPathName = "";
			} else {
				mPathName = i.getExtras().getString("PATH_NAME");
			}
		}

		mNameLabel = (TextView) findViewById(R.id.name_label);
		mPathLabel = (TextView) findViewById(R.id.path_label);
		mDirLabel = (TextView) findViewById(R.id.dirs_label);
		mFileLabel = (TextView) findViewById(R.id.files_label);
		mTimeLabel = (TextView) findViewById(R.id.time_stamp);
		mUsedLabel = (TextView) findViewById(R.id.total_size);
		mFreeLabel = (TextView) findViewById(R.id.freespace);
		mAvaibleLabel = (TextView) findViewById(R.id.avaible_size);

		new BackgroundWork().execute(mPathName);
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}

	private class BackgroundWork extends AsyncTask<String, Void, Long> {
		File dir = new File(mPathName);

		private ProgressDialog dialog;
		private String mDisplaySize;
		private String mAvaibleSize;
		private String mFreeSpace;
		private int mFileCount = 0;
		private int mDirCount = 0;
		long test = dir.getTotalSpace();
		long freespace = dir.getFreeSpace();

		protected void onPreExecute() {
			dialog = ProgressDialog.show(DirectoryInfo.this, "",
					getString(R.string.calcinfo));
			dialog.setCancelable(true);
		}

		@SuppressWarnings("deprecation")
		protected Long doInBackground(String... vals) {
			FileUtils flmg = new FileUtils();
			File dir = new File(vals[0]);
			long size = 0;
			int len = 0;

			File[] list = dir.listFiles();
			if (list != null)
				len = list.length;

			for (int i = 0; i < len; i++) {
				if (list[i].isFile())
					mFileCount++;
				else if (list[i].isDirectory())
					mDirCount++;
			}

			if (vals[0].equals("/")) {
				StatFs fss = new StatFs(Environment.getRootDirectory()
						.getPath());
				size = fss.getAvailableBlocks() * (fss.getBlockSize() / KB);

				mDisplaySize = (size > GB) ? String.format("%.2f GB",
						(double) size / MG) : String.format("%.2f MB",
						(double) size / KB);

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

			// get free space on SDcard
			if (freespace > GB)
				mFreeSpace = String.format("%.2f GB", (double) freespace / GB);
			else if (freespace < GB && freespace > MG)
				mFreeSpace = String.format("%.2f MB", (double) freespace / MG);
			else if (freespace < MG && freespace > KB)
				mFreeSpace = String.format("%.2f KB", (double) freespace / KB);
			else
				mFreeSpace = String.format("%.2f B", (double) freespace);

			// get total size of SDcard
			if (test > GB)
				mAvaibleSize = String.format("%.2f GB", (double) test / GB);
			else if (test < GB && test > MG)
				mAvaibleSize = String.format("%.2f MB", (double) test / MG);
			else if (test < MG && test > KB)
				mAvaibleSize = String.format("%.2f KB", (double) test / KB);
			else
				mAvaibleSize = String.format("%.2f B", (double) test);

			return size;
		}

		protected void onPostExecute(Long result) {
			File dir = new File(mPathName);

			String avaible = String.valueOf(mAvaibleSize);
			SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

			mNameLabel.setText(dir.getName());
			mPathLabel.setText(dir.getAbsolutePath());
			mDirLabel.setText(mDirCount + getString(R.string.folders));
			mFileLabel.setText(mFileCount + getString(R.string.files));
			mTimeLabel.setText(sdf1.format(dir.lastModified()));
			mUsedLabel.setText(mDisplaySize);
			mFreeLabel.setText(mFreeSpace);
			mAvaibleLabel.setText(avaible);

			dialog.cancel();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch (menu.getItemId()) {

		case android.R.id.home:
			this.finish();
			return true;
		}
		return false;
	}
}