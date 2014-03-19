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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class Settings {

	private Settings() {
	}

	public static boolean thumbnail;
	public static boolean mShowHiddenFiles;
	public static int viewmode;
	public static int mSortType;
	public static String mTheme;
	public static String defaultdir;

	public static void updatePreferences(Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);

		mShowHiddenFiles = p.getBoolean("displayhiddenfiles", true);
		thumbnail = p.getBoolean("showpreview", true);
		mTheme = p.getString("preference_theme", "light");
		String sort = p.getString("sort", "1");
		String mode = p.getString("viewmode", "1");
		defaultdir = p.getString("defaultdir", Environment
				.getExternalStorageDirectory().getPath());

		mSortType = Integer.parseInt(sort);
		viewmode = Integer.parseInt(mode);
	}
}
