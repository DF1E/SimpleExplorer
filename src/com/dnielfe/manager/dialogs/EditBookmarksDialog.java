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

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.Bookmarks;

public final class EditBookmarksDialog extends DialogFragment {

	private Cursor deleteBookmarksCursor;
	private List<Uri> bookmarksToDelete = new LinkedList<Uri>();
	private Activity a;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		a = getActivity();

		deleteBookmarksCursor = getBookmarksCursor();
		Builder dialog = new AlertDialog.Builder(a)
				.setTitle(R.string.bookmark)
				.setMultiChoiceItems(deleteBookmarksCursor, Bookmarks.CHECKED,
						Bookmarks.NAME,
						new DialogInterface.OnMultiChoiceClickListener() {
							@SuppressWarnings("deprecation")
							public void onClick(DialogInterface dialog,
									int item, boolean checked) {
								if (deleteBookmarksCursor.moveToPosition(item)) {
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
													(bookmarksToDelete.size() > 0) ? true
															: false);

									ContentValues checkedValues = new ContentValues();
									checkedValues.put(Bookmarks.CHECKED,
											checked ? 1 : 0);
									a.getContentResolver().update(deleteUri,
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
									a.getContentResolver().delete(uri, null,
											null);
								}
								restartBookmarksChecked();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								restartBookmarksChecked();
							}
						});
		return dialog.create();

	}

	@SuppressWarnings("deprecation")
	private void restartBookmarksChecked() {
		ContentValues checkedValues = new ContentValues();
		checkedValues.put(Bookmarks.CHECKED, 0);
		a.getContentResolver().update(Bookmarks.CONTENT_URI, checkedValues,
				null, null);
		deleteBookmarksCursor.requery();
		bookmarksToDelete.clear();
	}

	private Cursor getBookmarksCursor() {
		return a.getContentResolver().query(
				Bookmarks.CONTENT_URI,
				new String[] { Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH,
						Bookmarks.CHECKED }, null, null, null);
	}
}
