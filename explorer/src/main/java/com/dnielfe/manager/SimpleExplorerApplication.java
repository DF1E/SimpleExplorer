/*
 * Copyright (C) 2013 - 2015 Daniel F.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
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

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.dnielfe.manager.settings.Settings;

import io.fabric.sdk.android.Fabric;

public final class SimpleExplorerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // get default preferences at start - we need this for setting the theme
        Settings.updatePreferences(this);

        if (Settings.isReleaseVersion() && Settings.getErrorReports()) {
            Fabric.with(this, new Crashlytics());
        }
    }
}
