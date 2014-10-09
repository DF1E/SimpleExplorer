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

package com.dnielfe.manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.dnielfe.manager.settings.Settings;

public abstract class ThemableActivity extends Activity {

    private int mCurrentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCurrentTheme = Settings.mTheme;
        setTheme(mCurrentTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCurrentTheme != Settings.mTheme) {
            restart();
        }
    }

    protected void restart() {
        final Bundle outState = new Bundle();
        onSaveInstanceState(outState);
        final Intent intent = new Intent(this, getClass());
        // intent.putExtra(EXTRA_SAVED_STATE, outState);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}
