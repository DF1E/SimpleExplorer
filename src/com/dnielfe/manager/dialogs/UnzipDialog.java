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

import com.dnielfe.manager.Browser;
import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.UnZipTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

public final class UnzipDialog extends DialogFragment {

	private static String file;

	public static DialogFragment instantiate(String file1) {
		file = file1;

		final UnzipDialog dialog = new UnzipDialog();
		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle state) {
		final Activity a = getActivity();

		final AlertDialog.Builder b = new AlertDialog.Builder(a);

		// Set an EditText view to get user input
		final EditText inputf = new EditText(a);
		inputf.setText(Browser.mCurrentPath);

		b.setTitle(R.string.extractto);
		b.setView(inputf);

		b.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String newpath = inputf.getText().toString();

						dialog.dismiss();
						final UnZipTask task = new UnZipTask(a);
						task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
								file, newpath);
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
