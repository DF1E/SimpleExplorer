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

import com.dnielfe.manager.EventHandler;
import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.ZipTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

public final class ZipFilesDialog extends DialogFragment {

	private static String[] files;

	public static final String EXTRA_FILE = null;

	public static DialogFragment instantiate(String[] files1) {
		final Bundle extras = new Bundle();
		extras.putStringArray(EXTRA_FILE, files1);

		final ZipFilesDialog dialog = new ZipFilesDialog();
		dialog.setArguments(extras);

		return dialog;
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		final Bundle extras = this.getArguments();
		files = extras.getStringArray(EXTRA_FILE);
	}

	@Override
	public Dialog onCreateDialog(Bundle state) {
		final Activity a = getActivity();
		final String zipfile = EventHandler.getCurrentDir() + "/"
				+ "zipfile.zip";
		final int size = files.length;

		final AlertDialog.Builder b = new AlertDialog.Builder(a);

		// Set an EditText view to get user input
		final EditText inputf = new EditText(a);
		inputf.setText(zipfile);

		b.setTitle(getString(R.string.packing) + " (" + String.valueOf(size)
				+ ")");

		b.setView(inputf);

		b.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String newpath = inputf.getText().toString();

						dialog.dismiss();
						final ZipTask task = new ZipTask(a, newpath);
						task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
								files);
					}
				});
		b.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		return b.create();
	}
}
