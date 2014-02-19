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
import org.jetbrains.annotations.NotNull;

public final class DeleteTask extends AsyncTask<String, Void, List<String>> {

	private final WeakReference<Activity> activity;

	private ProgressDialog dialog;

	private String location;

	public DeleteTask(final Activity activity) {
		this.activity = new WeakReference<Activity>(activity);
	}

	@Override
	protected void onPreExecute() {
		final Activity activity = this.activity.get();

		if (activity != null) {
			this.dialog = new ProgressDialog(activity);
			this.dialog.setMessage(activity.getString(R.string.deleting));
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
			int size = files.length;

			for (int i = 0; i < size; i++)
				FileUtils.deleteTarget(files[i], location);
		} catch (Exception e) {
			failed.add(files.toString());
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
		if (activity != null && !failed.isEmpty()) {
			Toast.makeText(activity, activity.getString(R.string.cantopenfile),
					Toast.LENGTH_SHORT).show();
			if (!activity.isFinishing()) {
				dialog.show();
			}
		}
	}
}
