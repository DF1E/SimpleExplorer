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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.Bookmark;
import com.dnielfe.manager.utils.BookmarksHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BookmarksAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private List<Bookmark> mBookmarksList = new ArrayList<>();
    private BookmarksHelper db;

    public BookmarksAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        db = new BookmarksHelper(context);

        mBookmarksList = db.getAllBooks();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        final Bookmark bm = getItem(position);

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_bookmark, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(bm.getName());
        viewHolder.path.setText(bm.getPath());
        viewHolder.remove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                db.deleteBook(bm);
                updateList();
            }
        });

        return convertView;
    }

    public void createBookmark(File file) {
        db.addBookmark(new Bookmark(file.getName(), file.getPath()));
        updateList();
    }

    // TODO: find a better solution for refreshing ListView
    private void updateList() {
        if (!mBookmarksList.isEmpty())
            mBookmarksList.clear();

        mBookmarksList = db.getAllBooks();
        notifyDataSetChanged();
    }

    @Override
    public Bookmark getItem(int pos) {
        return mBookmarksList.get(pos);
    }

    @Override
    public int getCount() {
        return mBookmarksList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private class ViewHolder {
        final ImageButton remove;
        final TextView title;
        final TextView path;

        ViewHolder(View v) {
            title = (TextView) v.findViewById(R.id.title);
            path = (TextView) v.findViewById(R.id.path);
            remove = (ImageButton) v.findViewById(R.id.imageButton_remove);
        }
    }
}
