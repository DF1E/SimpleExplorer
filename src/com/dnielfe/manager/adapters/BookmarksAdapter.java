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

package com.dnielfe.manager.adapters;

import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.Bookmarks;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class BookmarksAdapter extends SimpleCursorAdapter {

	private static String[] fromColumns = { Bookmarks.NAME, Bookmarks.PATH };
	private static int[] toViews = { R.id.title, R.id.path };

	private LayoutInflater mLayoutInflater;

	public BookmarksAdapter(Context context, Cursor c, int i) {
		super(context, R.layout.item_bookmark, c, fromColumns, toViews, i);
		this.mLayoutInflater = LayoutInflater.from(context);
	}

	@Override
	public View newView(Context ctx, Cursor cursor, ViewGroup parent) {
		View view = mLayoutInflater.inflate(R.layout.item_bookmark, parent,
				false);
		view.setTag(new ViewHolder(view));
		return view;
	}

	@Override
	public void bindView(final View v, final Context ctx, final Cursor c) {
		ViewHolder vh = (ViewHolder) v.getTag();

		int Title_index = c.getColumnIndexOrThrow(Bookmarks.NAME);
		int Path_index = c.getColumnIndexOrThrow(Bookmarks.PATH);

		vh.title.setText(c.getString(Title_index));
		vh.path.setText(c.getString(Path_index));
		vh.remove.setOnClickListener(new OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View arg0) {
				// TODO fix delete
				Uri deleteUri = ContentUris.withAppendedId(
						Bookmarks.CONTENT_URI,
						c.getInt(c.getColumnIndex(Bookmarks._ID)));

				ctx.getContentResolver().delete(deleteUri, null, null);
				c.requery();
				notifyDataSetChanged();
			}
		});
	}

	private class ViewHolder {
		ImageButton remove;
		TextView title;
		TextView path;

		ViewHolder(View v) {
			title = (TextView) v.findViewById(R.id.title);
			path = (TextView) v.findViewById(R.id.path);
			remove = (ImageButton) v.findViewById(R.id.imageButton_remove);
		}
	}
}