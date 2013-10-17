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

import java.util.LinkedList;
import java.util.List;

import com.dnielfe.utils.Bookmarks;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.MenuItem;

public class Settings extends PreferenceActivity {

	private static final int DIALOG_DELETE_BOOKMARKS = 1;
	private Cursor deleteBookmarksCursor;
	private List<Uri> bookmarksToDelete = new LinkedList<Uri>();

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
						showDialog(DIALOG_DELETE_BOOKMARKS);
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

	@SuppressWarnings("deprecation")
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE_BOOKMARKS:
			deleteBookmarksCursor = getBookmarksCursor();
			Builder dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.bookmark)
					.setMultiChoiceItems(deleteBookmarksCursor,
							Bookmarks.CHECKED, Bookmarks.NAME,
							new DialogInterface.OnMultiChoiceClickListener() {
								public void onClick(DialogInterface dialog,
										int item, boolean checked) {
									if (deleteBookmarksCursor
											.moveToPosition(item)) {
										Uri deleteUri = ContentUris
												.withAppendedId(
														Bookmarks.CONTENT_URI,
														deleteBookmarksCursor
																.getInt(deleteBookmarksCursor
																		.getColumnIndex(Bookmarks._ID)));
										if (checked)
											bookmarksToDelete.add(deleteUri);
										else
											bookmarksToDelete.remove(deleteUri);

										((AlertDialog) dialog)
												.getButton(
														AlertDialog.BUTTON_POSITIVE)
												.setEnabled(
														(bookmarksToDelete
																.size() > 0) ? true
																: false);

										ContentValues checkedValues = new ContentValues();
										checkedValues.put(Bookmarks.CHECKED,
												checked ? 1 : 0);
										getContentResolver().update(deleteUri,
												checkedValues, null, null);
										deleteBookmarksCursor.requery();
									}
									((AlertDialog) dialog).getListView()
											.invalidate();
								}
							})
					.setPositiveButton(R.string.delete,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									for (Uri uri : bookmarksToDelete) {
										getContentResolver().delete(uri, null,
												null);
									}
									restartBookmarksChecked();
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									restartBookmarksChecked();
								}
							});
			return dialog.create();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	private void restartBookmarksChecked() {
		ContentValues checkedValues = new ContentValues();
		checkedValues.put(Bookmarks.CHECKED, 0);
		getContentResolver().update(Bookmarks.CONTENT_URI, checkedValues, null,
				null);
		deleteBookmarksCursor.requery();
		bookmarksToDelete.clear();
	}

	private Cursor getBookmarksCursor() {
		return getContentResolver().query(
				Bookmarks.CONTENT_URI,
				new String[] { Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH,
						Bookmarks.CHECKED }, null, null, null);
	}
}