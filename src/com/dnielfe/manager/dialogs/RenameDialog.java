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
import com.dnielfe.manager.FileUtils;
import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.LinuxShell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

public final class RenameDialog extends DialogFragment {

	private static String filepath;
	private static String name;

	public static final String EXTRA_FILE = null;
	public static final String EXTRA_NAME = null;

	public static DialogFragment instantiate(String dir, String name) {
		final Bundle extras = new Bundle();
		extras.putString(EXTRA_FILE, dir);
		extras.putString(EXTRA_NAME, name);

		final RenameDialog dialog = new RenameDialog();
		dialog.setArguments(extras);

		return dialog;
	}

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		final Bundle extras = this.getArguments();
		filepath = extras.getString(EXTRA_FILE);
		name = extras.getString(EXTRA_NAME);
	}

	@Override
	public Dialog onCreateDialog(Bundle state) {
		final Activity a = getActivity();

		final AlertDialog.Builder b = new AlertDialog.Builder(a);

		// Set an EditText view to get user input
		final EditText inputf = new EditText(a);
		inputf.setText(name);

		b.setTitle(R.string.rename);
		b.setView(inputf);

		b.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String newname = inputf.getText().toString();

						if (inputf.getText().length() < 1)
							dialog.dismiss();

						try {
							FileUtils.renameTarget(filepath, newname);
							Toast.makeText(a,
									getString(R.string.filewasrenamed),
									Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							if (LinuxShell.isRoot()) {
								FileUtils.renameRootTarget(
										EventHandler.getCurrentDir(), name,
										newname);
								Toast.makeText(a,
										getString(R.string.filewasrenamed),
										Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(a, getString(R.string.error),
										Toast.LENGTH_SHORT).show();
							}
						}

						dialog.dismiss();

						EventHandler.refreshDir(EventHandler.getCurrentDir());
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
