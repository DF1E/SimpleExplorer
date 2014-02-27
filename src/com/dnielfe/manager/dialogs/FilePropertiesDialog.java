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

import org.jetbrains.annotations.NotNull;

import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.FileProperties;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public final class FilePropertiesDialog extends DialogFragment {

	private Activity activity;
	private static String filePath;

	public static final String EXTRA_FILE = null;

	public static DialogFragment instantiate(String file) {
		filePath = file;

		final FilePropertiesDialog dialog = new FilePropertiesDialog();
		return dialog;
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
				R.layout.file_properties, null);
		this.initView(content);
		builder.setView(content);

		return builder.create();
	}

	private void initView(@NotNull final View view) {
		FileProperties mFileProperties = new FileProperties(activity, view,
				filePath);
		mFileProperties.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}
