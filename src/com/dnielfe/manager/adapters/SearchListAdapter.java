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

import java.io.File;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;

import com.dnielfe.manager.R;
import com.dnielfe.manager.SearchActivity;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SearchListAdapter extends ArrayAdapter<String> {
	private ArrayList<String> mData;
	private Context mContext;

	public SearchListAdapter(@NotNull final Context context,
			ArrayList<String> data) {
		super(context, R.layout.item_search, data);
		this.mContext = context;
		this.mData = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.item_search, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final String filepath = getItem(position);
		File f = new File(filepath);

		String filename = f.getName().toLowerCase();
		String mQuery = SearchActivity.mQuery;
		String highlightString = mQuery.toLowerCase();
		int startIndex = filename.indexOf(highlightString);

		if (startIndex != -1) {
			int len = mQuery.length();
			String strPart = f.getName()
					.substring(startIndex, startIndex + len);
			filename = f.getName().replace(strPart,
					"<font color=\"#FF8800\">" + strPart + "</font>");
			holder.name.setText(Html.fromHtml(filename),
					TextView.BufferType.SPANNABLE);
			holder.info.setText(f.getPath());
		} else {
			holder.name.setText(f.getName());
			holder.info.setText(f.getPath());
		}
		return (convertView);
	}

	@Override
	public String getItem(int pos) {
		return mData.get(pos);
	}

	private class ViewHolder {
		public TextView name = null;
		public TextView info = null;

		ViewHolder(View row) {
			name = (TextView) row.findViewById(R.id.label);
			info = (TextView) row.findViewById(R.id.info);
		}
	}
}