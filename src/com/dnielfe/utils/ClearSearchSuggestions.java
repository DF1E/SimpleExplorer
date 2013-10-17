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

package com.dnielfe.utils;

import com.dnielfe.manager.R;

import android.content.Context;
import android.preference.Preference;
import android.provider.SearchRecentSuggestions;
import android.util.AttributeSet;
import android.widget.Toast;

/* ClearSearchSuggestions
 * 		The Special clear recent search suggestions
 * 		Needs its own special class so you can just click on it */
public class ClearSearchSuggestions extends Preference {
	// This is the constructor called by the inflater
	public ClearSearchSuggestions(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onClick() {
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
				getContext(), SearchSuggestions.AUTHORITY,
				SearchSuggestions.MODE);
		suggestions.clearHistory();

		Toast.makeText(getContext(), R.string.suggestionscleared,
				Toast.LENGTH_SHORT).show();
		notifyChanged();
	}
}