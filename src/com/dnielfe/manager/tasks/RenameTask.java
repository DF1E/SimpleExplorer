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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dnielfe.manager.EventHandler;
import com.dnielfe.manager.FileUtils;
import com.dnielfe.manager.R;
import com.stericson.RootTools.RootTools;

import org.jetbrains.annotations.NotNull;

public final class RenameTask extends AsyncTask<String, Void, List<String>> {

	private final WeakReference<Activity> activity;

	private ProgressDialog dialog;

	private String location, filepath, name, newname;

	private boolean succes = false;

	public RenameTask(final Activity activity, String filepath, String name,
			String newname) {
		this.activity = new WeakReference<Activity>(activity);
		this.filepath = filepath;
		this.name = name;
		this.newname = newname;
	}

	@Override
	protected void onPreExecute() {
		final Activity activity = this.activity.get();

		if (activity != null) {
			this.dialog = new ProgressDialog(activity);
			this.dialog.setMessage(activity.getString(R.string.rename));
			this.dialog.setCancelable(true);
			this.dialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							cancel(false);
						}
					});
			if (!activity.isFinishing()) {
				this.dialog.show();
			}
		}
	}

	@NotNull
	@Override
	protected List<String> doInBackground(String... files) {
		final List<String> failed = new ArrayList<String>();

		location = EventHandler.getCurrentDir();

		try {
			if (FileUtils.renameTarget(filepath, newname)) {
				succes = true;
			} else {
				if (RootTools.isRootAvailable()) {
					FileUtils.renameRootTarget(EventHandler.getCurrentDir(),
							name, newname);
					succes = true;
				}
			}
		} catch (Exception e) {
			failed.add(name);
			succes = false;
		}
		return failed;
	}

	@Override
	protected void onPostExecute(@NotNull final List<String> failed) {
		super.onPostExecute(failed);
		this.finish(failed);
	}

	@Override
	protected void onCancelled(@NotNull final List<String> failed) {
		super.onCancelled(failed);
		this.finish(failed);
	}

	private void finish(@NotNull final List<String> failed) {
		if (this.dialog != null) {
			this.dialog.dismiss();
		}

		EventHandler.refreshDir(location);

		final Activity activity = this.activity.get();

		if (succes)
			Toast.makeText(activity,
					activity.getString(R.string.filewasrenamed),
					Toast.LENGTH_LONG).show();

		if (activity != null && !failed.isEmpty()) {
			Toast.makeText(activity, activity.getString(R.string.cantopenfile),
					Toast.LENGTH_SHORT).show();
			if (!activity.isFinishing()) {
				dialog.show();
			}
		}
	}
}
