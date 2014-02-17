/*
 * Copyright (C) 2013 Simple Explorer
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

package com.dnielfe.manager;

import com.dnielfe.manager.dialogs.EditBookmarksDialog;
import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.MenuItem;

public class Settings extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.preferences);

		Preference editBookmarks = findPreference("editbookmarks");
		editBookmarks
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference pref) {
						final DialogFragment dialog = new EditBookmarksDialog();
						dialog.show(getFragmentManager(), "dialog");
						return false;
					}
				});
	}

	@Override
	public void onBackPressed() {
		this.finish();
		Intent i = new Intent(getBaseContext(), Main.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		return;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch (menu.getItemId()) {

		case android.R.id.home:
			this.finish();
			Intent i = new Intent(getBaseContext(), Main.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return true;
		}
		return false;
	}
}