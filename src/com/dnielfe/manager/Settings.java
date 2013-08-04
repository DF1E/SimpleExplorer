package com.dnielfe.manager;

import java.util.LinkedList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.MenuItem;

public class Settings extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String PREFS_DISPLAYHIDDENFILES = "displayhiddenfiles";
	public static final String PREFS_PREVIEW = "showpreview";
	public static final String PREFS_SEARCH = "enablesearchsuggestions";
	public static final String PREFS_SORT = "sort";
	public static final String PREFS_VIEW = "viewmode";
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
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	}

	@SuppressWarnings("deprecation")
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DELETE_BOOKMARKS:
			deleteBookmarksCursor = getBookmarksCursor();
			AlertDialog dialog = new AlertDialog.Builder(this)
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
							}).create();
			return dialog;
		}
		return super.onCreateDialog(id);
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

	@SuppressWarnings("deprecation")
	private Cursor getBookmarksCursor() {
		return managedQuery(Bookmarks.CONTENT_URI, new String[] {
				Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH,
				Bookmarks.CHECKED }, null, null, null);
	}
}