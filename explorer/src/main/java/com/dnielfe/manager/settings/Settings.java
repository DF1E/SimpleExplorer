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

package com.dnielfe.manager.settings;

import com.dnielfe.manager.R;
import com.stericson.RootTools.RootTools;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class Settings {

	private static boolean showthumbnail;
	private static boolean mShowHiddenFiles;
	private static boolean mRootAccess;
	public static boolean reverseList;
	public static int mListAppearance;
	public static int mSortType;
	public static int mTheme;
	public static String defaultdir;
	private static SharedPreferences p;

	public static void updatePreferences(Context context) {
		p = PreferenceManager.getDefaultSharedPreferences(context);

		mShowHiddenFiles = p.getBoolean("displayhiddenfiles", true);
		showthumbnail = p.getBoolean("showpreview", true);
		mRootAccess = p.getBoolean("enablerootaccess", false);
		reverseList = p.getBoolean("reverseList", false);
		mTheme = Integer.parseInt(p.getString("preference_theme",
				Integer.toString(R.style.ThemeLight)));
		mSortType = Integer.parseInt(p.getString("sort", "1"));
		mListAppearance = Integer.parseInt(p.getString("viewmode", "1"));
		defaultdir = p.getString("defaultdir", Environment
				.getExternalStorageDirectory().getPath());

		rootAccess();
	}

	public static boolean showThumbnail() {
		return showthumbnail;
	}

	public static boolean showHiddenFiles() {
		return mShowHiddenFiles;
	}

	public static boolean rootAccess() {
		return mRootAccess ? RootTools.isAccessGiven() : false;
	}

	public static boolean reverseListView() {
		return reverseList;
	}
}
