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

package com.dnielfe.manager.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class Bookmarks extends ContentProvider implements BaseColumns {
    private static final String TB_NAME = "bookmarks";
    public static final String NAME = "name";
    public static final String PATH = "path";

    // Only because of multiple choice delete dialog
    public static final String CHECKED = "checked";

    private static final String BASE_PATH = "bookmarks";
    private static final String PROVIDER_NAME = "com.dnielfe.manager.bookmarks";
    public static final Uri CONTENT_URI = Uri.parse("content://"
            + PROVIDER_NAME + "/" + BASE_PATH);

    private static final int BOOKMARKS = 1;
    private static final int BOOKMARK_ID = 2;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, BASE_PATH, BOOKMARKS);
        uriMatcher.addURI(PROVIDER_NAME, BASE_PATH + "/#", BOOKMARK_ID);
    }

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private static final String DATABASE_CREATE = String
            .format("CREATE TABLE %s (%s integer primary key autoincrement, "
                            + "%s text not null, %s text not null, %s integer default 0);",
                    TB_NAME, _ID, NAME, PATH, CHECKED);

    private static final String DATABASE_NAME = "com.dnielfe.manager";
    private static final int DATABASE_VERSION = 2;

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        db = dbHelper.getWritableDatabase();
        return (db != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsDeleted;

        switch (uriType) {
            case BOOKMARKS:
                rowsDeleted = db.delete(TB_NAME, selection, selectionArgs);
                break;
            case BOOKMARK_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(TB_NAME, _ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(TB_NAME, _ID + "=" + id + " and "
                            + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        long id;

        switch (uriType) {
            case BOOKMARKS:
                id = db.insert(TB_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TB_NAME);

        int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case BOOKMARKS:
                break;
            case BOOKMARK_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        Cursor c = queryBuilder.query(db, projection, selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsUpdated;

        switch (uriType) {
            case BOOKMARKS:
                rowsUpdated = db.update(TB_NAME, values, selection, selectionArgs);
                break;
            case BOOKMARK_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(TB_NAME, values, _ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(TB_NAME, values, _ID + "=" + id
                            + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        /*
         * When changing database version, you MUST change this method.
         * Currently, it would delete all users' bookmarks
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TB_NAME);
            onCreate(db);
        }
    }
}