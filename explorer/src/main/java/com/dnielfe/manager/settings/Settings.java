package com.dnielfe.manager.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.dnielfe.manager.R;
import com.stericson.RootTools.RootTools;

public final class Settings {

    private static boolean showthumbnail;
    private static boolean mShowHiddenFiles;
    private static boolean mRootAccess;
    private static boolean reverseList;
    public static int mListAppearance;
    public static int mSortType;
    public static int mTheme;
    private static String defaultdir;

    public static void updatePreferences(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);

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
        return mRootAccess && RootTools.isAccessGiven();
    }

    public static boolean reverseListView() {
        return reverseList;
    }

    public static String getDefaultDir() {
        return defaultdir;
    }
}
