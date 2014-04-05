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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.dnielfe.manager.Browser;
import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.SimpleUtils;

public final class CreateFolderDialog extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Activity a = getActivity();

		final AlertDialog.Builder b = new AlertDialog.Builder(a);
		b.setTitle(R.string.createnewfolder);
		b.setMessage(R.string.createmsg);

		// Set an EditText view to get user input
		final EditText inputf = new EditText(a);

		b.setView(inputf);
		b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = inputf.getText().toString();

				if (name.length() >= 1) {
					try {
						SimpleUtils.createDir(Browser.mCurrentPath + "/", name);
						Toast.makeText(a, name + getString(R.string.created),
								Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						Toast.makeText(a,
								getString(R.string.newfolderwasnotcreated),
								Toast.LENGTH_SHORT).show();
					}
				} else {
					dialog.dismiss();
					Toast.makeText(a,
							getString(R.string.newfolderwasnotcreated),
							Toast.LENGTH_SHORT).show();
				}

				dialog.dismiss();
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
