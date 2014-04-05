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

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public final class SettingsFragment extends PreferenceFragment {

	private static final String[] THEMES_VALUES = new String[] {
			Integer.toString(R.style.ThemeLight),
			Integer.toString(R.style.ThemeDark) };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.preferences);
		this.init();
	}

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof SettingsActivity)) {
			throw new RuntimeException(
					"Should be attached only to SettingsActivity");
		}
	}

	private void init() {
		// final SettingsActivity parent = (SettingsActivity) getActivity();
		final ListPreference theme = (ListPreference) findPreference("preference_theme");
		theme.setEntryValues(THEMES_VALUES);
		theme.setValue(String.valueOf(Settings.mTheme));
		theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				final int chosenTheme = Integer.parseInt((String) newValue);
				if (chosenTheme != Settings.mTheme) {
					Settings.mTheme = chosenTheme;
					((SettingsActivity) getActivity()).proxyRestart();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}
