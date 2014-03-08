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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import org.jetbrains.annotations.NotNull;

import com.dnielfe.manager.FileUtils;
import com.dnielfe.manager.R;
import com.dnielfe.manager.commands.Permissions;
import com.dnielfe.manager.commands.RootCommands;
import com.dnielfe.manager.utils.MD5Checksum;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public final class FilePropertiesDialog extends DialogFragment {

	private Activity activity;
	private static File mFile;
	private PropertiesAdapter mAdapter;

	public static DialogFragment instantiate(File file) {
		mFile = file;

		final FilePropertiesDialog dialog = new FilePropertiesDialog();
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle state) {
		activity = getActivity();
		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		mAdapter = new PropertiesAdapter(activity, mFile);
		builder.setTitle(mFile.getName());
		builder.setNeutralButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final FilePermissionsPagerItem fragment = (FilePermissionsPagerItem) mAdapter
								.getItem(1);
						fragment.applyPermissions(getActivity());
					}
				});
		final View content = activity.getLayoutInflater().inflate(
				R.layout.dialog_properties_container, null);
		this.initView(content);
		builder.setView(content);
		final AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(final DialogInterface dialog) {
				((AlertDialog) dialog).getButton(
						DialogInterface.BUTTON_POSITIVE).setVisibility(
						View.GONE);
			}
		});
		return dialog;
	}

	private void initView(@NotNull final View view) {
		final ViewPager pager = (ViewPager) view
				.findViewById(R.id.tabsContainer);
		pager.setAdapter(mAdapter);

		final CompoundButton tab1 = (CompoundButton) view
				.findViewById(R.id.tab1);
		final CompoundButton tab2 = (CompoundButton) view
				.findViewById(R.id.tab2);

		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				tab1.setChecked(position == 0);
				tab2.setChecked(position == 1);
				((AlertDialog) getDialog())
						.getButton(DialogInterface.BUTTON_POSITIVE)
						.setVisibility(
								position == 0
										|| !((FilePermissionsPagerItem) mAdapter
												.getItem(1)).areBoxesEnabled() ? View.GONE
										: View.VISIBLE);
			}
		});

		tab1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tab1.setChecked(true);
				tab2.setChecked(false);
				pager.setCurrentItem(0);
			}
		});

		tab2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tab2.setChecked(true);
				tab1.setChecked(false);
				pager.setCurrentItem(1);
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		mAdapter.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
		mAdapter.onStop();
	}

	private interface PagerItem {

		void onStart();

		void onStop();

		@NotNull
		View onCreateView(@NotNull LayoutInflater inflater);
	}

	private static final class PropertiesAdapter extends PagerAdapter {

		private final LayoutInflater mLayoutInflater;
		private final File mFile;
		private final PagerItem[] mItems;

		private PropertiesAdapter(@NotNull final Activity context,
				@NotNull final File file) {
			mLayoutInflater = context.getLayoutInflater();
			mFile = file;
			mItems = new PagerItem[] {
					new FilePropertiesPagerItem(context, mFile),
					new FilePermissionsPagerItem(mFile) };
		}

		void onStart() {
			for (final PagerItem item : mItems) {
				item.onStart();
			}
		}

		void onStop() {
			for (final PagerItem item : mItems) {
				item.onStop();
			}
		}

		@NotNull
		PagerItem getItem(final int position) {
			return mItems[position];
		}

		@Override
		public Object instantiateItem(final ViewGroup container,
				final int position) {
			final PagerItem item = mItems[position];
			final View view = item.onCreateView(mLayoutInflater);
			container.addView(view);
			item.onStart();
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return mItems.length;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}

	private static final class FilePropertiesPagerItem implements PagerItem {

		final int KB = 1024;
		final int MG = KB * KB;
		final int GB = MG * KB;

		long size = 0;

		private String mDisplaySize;
		private File file3;
		private Context mContext;
		private TextView mPathLabel, mTimeLabel, mSizeLabel, mMD5Label;
		private View mView;
		private LoadFsTask mTask;

		private FilePropertiesPagerItem(Activity ac, File file) {
			this.mContext = ac;
			this.file3 = file;
		}

		@NotNull
		@Override
		public View onCreateView(@NotNull final LayoutInflater inflater) {
			mView = inflater.inflate(R.layout.dialog_properties, null);
			initView(mView);
			return mView;
		}

		@Override
		public void onStart() {
			if (mView != null) {
				if (mTask == null) {
					mTask = new LoadFsTask();
				}
				if (mTask.getStatus() != AsyncTask.Status.RUNNING) {
					mTask.execute(mFile);
				}
			}
		}

		@Override
		public void onStop() {
			if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
				mTask.cancel(false);
			}
		}

		private void initView(View table) {
			this.mPathLabel = (TextView) mView.findViewById(R.id.path_label);
			this.mTimeLabel = (TextView) mView.findViewById(R.id.time_stamp);
			this.mSizeLabel = (TextView) mView.findViewById(R.id.total_size);
			this.mMD5Label = (TextView) mView.findViewById(R.id.md5_summary);
		}

		private final class LoadFsTask extends AsyncTask<File, Void, String> {
			private LoadFsTask() {
			}

			@Override
			protected void onPreExecute() {
				mPathLabel.setText(file3.getAbsolutePath());
				mTimeLabel.setText("...");
				mSizeLabel.setText("...");
				mMD5Label.setText("...");
			}

			@Override
			protected String doInBackground(final File... params) {
				DateFormat dateFormat = android.text.format.DateFormat
						.getDateFormat(mContext.getApplicationContext());
				DateFormat timeFormat = android.text.format.DateFormat
						.getTimeFormat(mContext.getApplicationContext());

				mTimeLabel.setText(dateFormat.format(file3.lastModified())
						+ " " + timeFormat.format(file3.lastModified()));

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

			@Override
			protected void onPostExecute(final String result) {
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
	}

	private static final class FilePermissionsPagerItem implements PagerItem,
			CompoundButton.OnCheckedChangeListener {

		/**
		 * Permissions that file had when FilePermissionsController was created
		 */
		private Permissions mInputPermissions = null;

		/**
		 * Currently modified permissions
		 */
		private Permissions mModifiedPermissions = null;

		/**
		 * User: read, write, execute
		 */
		private CompoundButton ur;
		private CompoundButton uw;
		private CompoundButton ux;

		/**
		 * Group: read, write, execute
		 */
		private CompoundButton gr;
		private CompoundButton gw;
		private CompoundButton gx;

		/**
		 * Others: read, write, execute
		 */
		private CompoundButton or;
		private CompoundButton ow;
		private CompoundButton ox;

		private final File mFile;
		private View mView;
		private TextView mOwner;
		private LoadFsTask mTask;
		private TextView mGroup;
		private Permissions mPermission;
		private static String[] mFileInfo;

		private FilePermissionsPagerItem(final File file) {
			mFile = file;
		}

		@NotNull
		@Override
		public View onCreateView(@NotNull final LayoutInflater inflater) {
			mView = inflater.inflate(R.layout.dialog_permissions, null);
			initView(mView);
			return mView;
		}

		@Override
		public void onStart() {
			if (mView != null) {
				if (mTask == null) {
					mTask = new LoadFsTask(this);
				}
				if (mTask.getStatus() != AsyncTask.Status.RUNNING) {
					mTask.execute(mFile);
				}
			}
		}

		@Override
		public void onStop() {
			if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
				mTask.cancel(false);
			}
		}

		private void initView(View table) {
			this.mOwner = (TextView) mView.findViewById(R.id.owner);
			this.mGroup = (TextView) mView.findViewById(R.id.group);
			this.ur = (CompoundButton) mView.findViewById(R.id.uread);
			this.uw = (CompoundButton) mView.findViewById(R.id.uwrite);
			this.ux = (CompoundButton) mView.findViewById(R.id.uexecute);
			this.gr = (CompoundButton) mView.findViewById(R.id.gread);
			this.gw = (CompoundButton) mView.findViewById(R.id.gwrite);
			this.gx = (CompoundButton) mView.findViewById(R.id.gexecute);
			this.or = (CompoundButton) mView.findViewById(R.id.oread);
			this.ow = (CompoundButton) mView.findViewById(R.id.owrite);
			this.ox = (CompoundButton) mView.findViewById(R.id.oexecute);

			try {
				getPermissions(mFile);
				if (!(mFileInfo == null)) {
					mOwner.setText(mFileInfo[1]);
					mGroup.setText(mFileInfo[2]);
				} else {
					disableBoxes();
				}
			} catch (Exception e) {
				disableBoxes();
			}
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			this.mModifiedPermissions = new Permissions(this.ur.isChecked(),
					this.uw.isChecked(), this.ux.isChecked(),
					this.gr.isChecked(), this.gw.isChecked(),
					this.gx.isChecked(), this.or.isChecked(),
					this.ow.isChecked(), this.ox.isChecked());
		}

		private class LoadFsTask extends AsyncTask<File, Void, String> {

			private final WeakReference<FilePermissionsPagerItem> mItemRef;

			private LoadFsTask(@NotNull final FilePermissionsPagerItem item) {
				this.mItemRef = new WeakReference<FilePermissionsPagerItem>(
						item);
			}

			@Override
			protected String doInBackground(final File... params) {
				return params[0].getAbsolutePath();
			}

			@Override
			protected void onPostExecute(final String file) {
				final FilePermissionsPagerItem item = mItemRef.get();
				if (item != null) {
					if (file == null) {
						item.disableBoxes();
					}
					item.mView.findViewById(android.R.id.progress)
							.setVisibility(View.GONE);
					item.mView.findViewById(R.id.content).setVisibility(
							View.VISIBLE);
				}
			}
		}

		boolean areBoxesEnabled() {
			return this.ur.isEnabled();
		}

		public void applyPermissions(final Context context) {
			if (!mInputPermissions.equals(mModifiedPermissions)) {
				final ApplyTask task = new ApplyTask(context,
						mModifiedPermissions);
				task.execute(this.mFile);
			}
		}

		private void disableBoxes() {
			mOwner.setText("---");
			mGroup.setText("---");

			this.ur.setEnabled(false);
			this.uw.setEnabled(false);
			this.ux.setEnabled(false);
			this.gr.setEnabled(false);
			this.gw.setEnabled(false);
			this.gx.setEnabled(false);
			this.or.setEnabled(false);
			this.ow.setEnabled(false);
			this.ox.setEnabled(false);
		}

		private class ApplyTask extends AsyncTask<File, Void, Boolean> {

			private final Context context;
			private final Permissions target;

			private ApplyTask(Context context, Permissions target) {
				this.context = context;
				this.target = target;
			}

			@Override
			protected Boolean doInBackground(final File... params) {
				return RootCommands.applyPermissions(params[0], this.target);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result)
					Toast.makeText(
							this.context,
							this.context.getString(R.string.permissionschanged),
							Toast.LENGTH_SHORT).show();
			}

		}

		private void getPermissions(File file32) {
			try {
				getFileProperties(file32);
				mPermission = new Permissions(mFileInfo[0]);

				this.ur.setChecked(mPermission.ur);
				this.uw.setChecked(mPermission.uw);
				this.ux.setChecked(mPermission.ux);
				this.gr.setChecked(mPermission.gr);
				this.gw.setChecked(mPermission.gw);
				this.gx.setChecked(mPermission.gx);
				this.or.setChecked(mPermission.or);
				this.ow.setChecked(mPermission.ow);
				this.ox.setChecked(mPermission.ox);

				this.ur.setOnCheckedChangeListener(this);
				this.uw.setOnCheckedChangeListener(this);
				this.ux.setOnCheckedChangeListener(this);
				this.gr.setOnCheckedChangeListener(this);
				this.gw.setOnCheckedChangeListener(this);
				this.gx.setOnCheckedChangeListener(this);
				this.or.setOnCheckedChangeListener(this);
				this.ow.setOnCheckedChangeListener(this);
				this.ox.setOnCheckedChangeListener(this);

				this.mInputPermissions = mPermission;
				this.mModifiedPermissions = mPermission;
			} catch (Exception e) {
				disableBoxes();

				this.mInputPermissions = null;
				this.mModifiedPermissions = null;
			}
		}

		private static void getFileProperties(File file) {
			BufferedWriter out;
			BufferedReader in;

			try {
				String[] cmd = { "su", "-c", "ls", "-l",
						RootCommands.getCmdPath(file.getAbsolutePath()) };

				Process proc = Runtime.getRuntime().exec(cmd);
				out = new BufferedWriter(new OutputStreamWriter(
						proc.getOutputStream()));
				in = new BufferedReader(new InputStreamReader(
						proc.getInputStream()));
				String line = "";
				while ((line = in.readLine()) != null) {
					createFileInfo(line.split("\\s+"));
				}
				proc.waitFor();
				in.close();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private static String createFileInfo(String... args) {
			if (args.length == 6) {
				mFileInfo = new String[] { args[0].substring(1, 9), args[1],
						args[2], args[3] + " " + args[4], args[5] };
			} else if (args.length == 7) {
				mFileInfo = new String[] { args[0], args[1], args[2], args[3],
						args[4] + " " + args[5], args[6] };
			}
			return null;
		}
	}
}
