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

import java.io.File;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dnielfe.manager.R;
import com.dnielfe.manager.commands.RootCommands;

public class GroupOwnerTask extends AsyncTask<File, Void, Boolean> {

	private final Context context;
	private final String group, owner;

	public GroupOwnerTask(Context context, String group1, String owner1) {
		this.context = context;
		this.group = group1;
		this.owner = owner1;
	}

	@Override
	protected Boolean doInBackground(final File... params) {
		return RootCommands.changeGroupOwner((params[0]), owner, group);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result)
			Toast.makeText(this.context,
					this.context.getString(R.string.permissionschanged),
					Toast.LENGTH_SHORT).show();
	}
}