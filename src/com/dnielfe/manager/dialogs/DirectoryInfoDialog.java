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

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.StatFsCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;

public final class DirectoryInfoDialog extends DialogFragment {

	private static File mFile;
	private PartitionInfoTask mTask;
	private View mView;

	public static DialogFragment instantiate(final File file) {
		mFile = file;

		final DirectoryInfoDialog d = new DirectoryInfoDialog();
		return d;
	}

	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Activity activity = this.getActivity();
		mView = activity.getLayoutInflater().inflate(
				R.layout.dialog_directory_info, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.dir_info);
		builder.setView(mView);
		builder.setNeutralButton(R.string.cancel, null);
		return builder.create();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (mTask == null) {
			mTask = new PartitionInfoTask(mView);
		}
		if (mTask.getStatus() != AsyncTask.Status.RUNNING) {
			mTask.execute(mFile);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
			mTask.cancel(false);
		}
	}

	private static final class PartitionInfo {
		final CharSequence mPath;

		final CharSequence mTotalBytesText;
		final CharSequence mBlockSizeText;
		final CharSequence mFreeBytesText;
		final CharSequence mUsedSpaceText;

		final long mTotalBytes;
		final long mBlockSize;
		final long mFreeBytes;
		final long mUsedSpace;

		private PartitionInfo(@NotNull final CharSequence path,
				final long totalBytes, final long blockSize,
				final long freeBytes, final long usedSpace) {
			this.mPath = path;

			this.mTotalBytes = totalBytes;
			this.mBlockSize = blockSize;
			this.mFreeBytes = freeBytes;
			this.mUsedSpace = usedSpace;

			this.mTotalBytesText = FileUtils.byteCountToDisplaySize(totalBytes);
			this.mBlockSizeText = Long.toString(blockSize);
			this.mFreeBytesText = FileUtils.byteCountToDisplaySize(freeBytes);

			if (totalBytes != 0L) {
				final StringBuilder usage = new StringBuilder();
				usage.append(FileUtils.byteCountToDisplaySize(usedSpace));
				usage.append(' ');
				usage.append('(');
				usage.append(usedSpace * 100L / totalBytes);
				usage.append('%');
				usage.append(')');
				this.mUsedSpaceText = usage.toString();
			} else {
				this.mUsedSpaceText = null;
			}
		}
	}

	private static final class PartitionInfoTask extends
			AsyncTask<File, Void, PartitionInfo> {

		private final WeakReference<View> mViewRef;

		private PartitionInfoTask(@NotNull final View view) {
			this.mViewRef = new WeakReference<View>(view);
		}

		@NotNull
		@Override
		protected PartitionInfo doInBackground(final File... params) {
			final String path = params[0].getAbsolutePath();
			final StatFsCompat statFs = new StatFsCompat(path);
			final long valueTotal = statFs.getTotalBytes();
			final long valueAvail = statFs.getAvailableBytes();
			final long valueUsed = valueTotal - valueAvail;
			return new PartitionInfo(path, valueTotal,
					statFs.getBlockSizeLong(), statFs.getFreeBytes(), valueUsed);
		}

		@Override
		protected void onPostExecute(final @NotNull PartitionInfo partitionInfo) {
			final View view = mViewRef.get();
			if (view != null) {
				final TextView title = (TextView) view
						.findViewById(R.id.location);
				title.setText(partitionInfo.mPath);

				if (partitionInfo.mTotalBytes != 0L) {
					final TextView total = (TextView) view
							.findViewById(R.id.total);
					total.setText(partitionInfo.mTotalBytesText);
				}

				if (partitionInfo.mBlockSize != 0L) {
					final TextView block = (TextView) view
							.findViewById(R.id.block_size);
					block.setText(partitionInfo.mBlockSizeText);
				}

				if (partitionInfo.mFreeBytes != 0L) {
					final TextView free = (TextView) view
							.findViewById(R.id.free);
					free.setText(partitionInfo.mFreeBytesText);
				}

				if (partitionInfo.mUsedSpace != 0L) {
					final TextView used = (TextView) view
							.findViewById(R.id.used);
					used.setText(partitionInfo.mUsedSpaceText);
				}
			}
		}
	}
}
