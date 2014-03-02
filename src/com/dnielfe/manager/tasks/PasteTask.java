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
import com.dnielfe.manager.utils.ClipBoard;

import org.jetbrains.annotations.NotNull;

public final class PasteTask extends AsyncTask<String, Void, List<String>> {

	private final WeakReference<Activity> activity;

	private ProgressDialog dialog;

	private String location;

	private boolean success = false;

	public PasteTask(final Activity activity, String currentDir) {
		this.activity = new WeakReference<Activity>(activity);
		this.location = currentDir;
	}

	@Override
	protected void onPreExecute() {
		final Activity activity = this.activity.get();

		if (activity != null) {
			this.dialog = new ProgressDialog(activity);

			if (ClipBoard.isMove())
				this.dialog.setMessage(activity.getString(R.string.moving));
			else
				this.dialog.setMessage(activity.getString(R.string.copying));

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

		String[] content = ClipBoard.getClipBoardContents();

		int len = content.length;

		if (len == 0) {
			failed.add(content.toString());
			return failed;
		} else if (len == 1) {
			FileUtils.copyToDirectory(content[0], location);
			success = true;
			if (ClipBoard.isMove()) {
				FileUtils.deleteTarget(content[0], location);
				success = true;
			}
		} else {
			for (int i = 0; i < len; i++) {
				FileUtils.copyToDirectory(content[i], location);
				success = true;
				if (ClipBoard.isMove()) {
					FileUtils.deleteTarget(content[i], location);
					success = true;
				}
			}
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

		final Activity activity = this.activity.get();

		if (ClipBoard.isMove()) {
			if (success)
				Toast.makeText(activity,
						activity.getString(R.string.movesuccsess),
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(activity, activity.getString(R.string.movefail),
						Toast.LENGTH_SHORT).show();
		} else {
			if (success)
				Toast.makeText(activity,
						activity.getString(R.string.copysuccsess),
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(activity, activity.getString(R.string.copyfail),
						Toast.LENGTH_SHORT).show();
		}

		ClipBoard.unlock();
		ClipBoard.clear();
		activity.invalidateOptionsMenu();

		EventHandler.refreshDir(location);

		if (activity != null && !failed.isEmpty()) {
			Toast.makeText(activity, activity.getString(R.string.cantopenfile),
					Toast.LENGTH_SHORT).show();
			if (!activity.isFinishing()) {
				dialog.show();
			}
		}
	}
}
