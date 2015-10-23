package com.dnielfe.manager.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.util.LinkedList;
import java.util.List;

public class BookmarksHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "BookmarksDB";
    // Books table name
    private static final String TABLE_BOOKS = "bookmarks";
    // Books Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PATH = "path";

    public BookmarksHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement to create book table
        String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " (id " +
                "INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path Text)";

        // create books table
        db.execSQL(CREATE_BOOK_TABLE);

        // add SDCard bookmark at start
        addBookmarkOnCreate(db, new Bookmark("SDCard", Environment.getExternalStorageDirectory().getPath()));
        // add root bookmark at start
        addBookmarkOnCreate(db, new Bookmark("Root", "/"));
        // add system bookmark at start
        addBookmarkOnCreate(db, new Bookmark("System", Environment.getRootDirectory().getPath()));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        this.onCreate(db);
    }

    public List<Bookmark> getAllBooks() {
        List<Bookmark> books = new LinkedList<>();

        String query = "SELECT  * FROM " + TABLE_BOOKS;

        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // go over each row, build book and add it to list
        Bookmark book;
        if (cursor.moveToFirst()) {
            do {
                book = new Bookmark();
                book.setId(Integer.parseInt(cursor.getString(0)));
                book.setName(cursor.getString(1));
                book.setPath(cursor.getString(2));
                books.add(book);
            } while (cursor.moveToNext());
        }

        return books;
    }

    public void addBookmarkOnCreate(SQLiteDatabase db, Bookmark book) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, book.getName());
        values.put(KEY_PATH, book.getPath());

        db.insert(TABLE_BOOKS, null, values);
    }

    public void addBookmark(Bookmark book) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, book.getName());
        values.put(KEY_PATH, book.getPath());

        db.insert(TABLE_BOOKS, null, values);
        db.close();
    }

    public void deleteBook(Bookmark book) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOKS, KEY_ID + " = ?", new String[]{String.valueOf(book.getId())});
        db.close();
    }
}
