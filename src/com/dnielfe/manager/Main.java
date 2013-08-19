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

package com.dnielfe.manager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.dnielfe.utils.Bookmarks;
import com.dnielfe.utils.Compress;
import com.dnielfe.utils.Decompress;
import com.dnielfe.utils.SearchSuggestions;
import com.dnielfe.utils.Shortcut;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;

public final class Main extends ListActivity {

	private static final String PREF_HIDDEN = "displayhiddenfiles";
	public static final String PREF_PREVIEW = "showpreview";
	public static final String PREFS_SORT = "sort";
	public static final String PREFS_VIEW = "viewmode";
	public static final String PREF_SEARCH = "enablesearchsuggestions";
	public static final String PREFS_SHORTCUT = "shortcut";

	private static final int D_MENU_SHORTCUT = 3;
	private static final int D_MENU_BOOKMARK = 4;
	private static final int F_MENU_OPENAS = 5;
	private static final int D_MENU_DELETE = 6;
	private static final int D_MENU_RENAME = 7;
	private static final int D_MENU_COPY = 8;
	private static final int D_MENU_PASTE = 9;
	private static final int D_MENU_ZIP = 10;
	private static final int D_MENU_MOVE = 12;
	private static final int F_MENU_BOOKMARK = 13;
	private static final int F_MENU_MOVE = 14;
	private static final int F_MENU_DELETE = 15;
	private static final int F_MENU_RENAME = 16;
	private static final int F_MENU_ATTACH = 17;
	private static final int F_MENU_COPY = 18;
	private static final int F_MENU_DETAILS = 19;
	private static final int D_MENU_DETAILS = 20;

	private static final int MULTIZIP_TYPE = 30;
	private static final int UNZIP_TYPE = 31;
	private static final int DELETE_TYPE = 32;
	private static final int SEARCH_TYPE = 33;
	private static final int ZIP_TYPE = 34;

	private static FileOperations mFileMag;
	private static EventHandler mHandler;
	private static EventHandler.TableRow mTable;

	private boolean mReturnIntent = false;
	private boolean mHoldingFile = false;
	private boolean mUseBackKey = true;

	SharedPreferences mSettings;
	LinearLayout mDirectoryButtons;
	ActionMode mActionMode;
	private int directorytextsize = 15;

	static String mCopiedTarget;
	static String mSelectedListItem;
	private String display_size;
	private static String startpath = Environment.getExternalStorageDirectory()
			.getPath();
	private static final String searchdirectory = "/storage/";
	private static String[] multidata;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		// read settings
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean hidden = mSettings.getBoolean(PREF_HIDDEN, true);
		boolean thumb = mSettings.getBoolean(PREF_PREVIEW, true);
		String value = mSettings.getString("sort", "1");
		String viewmode = mSettings.getString("viewmode", "1");
		String defaultdir = mSettings.getString("defaultdir", startpath);

		int sort = Integer.parseInt(value);
		int viewm = Integer.parseInt(viewmode);

		mFileMag = new FileOperations();
		mFileMag.setShowHiddenFiles(hidden);
		mFileMag.setSortType(sort);

		if (savedInstanceState != null)
			mHandler = new EventHandler(Main.this, mFileMag,
					savedInstanceState.getString("location"));
		else
			mHandler = new EventHandler(Main.this, mFileMag);

		mHandler.setViewMode(viewm);
		mHandler.setShowThumbnails(thumb);
		mTable = mHandler.new TableRow();

		// ActionBar Settings
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// ActionBar Layout
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.actionbar, null);
		actionBar.setCustomView(v);
		actionBar.show();

		// Set List Adapter
		mHandler.setListAdapter(mTable);
		setListAdapter(mTable);

		// register context menu for our list view
		registerForContextMenu(getListView());

		Intent intent3 = getIntent();
		SearchIntent(intent3);

		getOverflowMenu();
		setsearchbutton();
		checkEnvironment();

		Intent intent = getIntent();
		// Check for Shortcut Extra
		if (intent.hasExtra("shortcut")) {
			String shortcut = (String) getIntent().getExtras().get("shortcut");
			mHandler.opendir(shortcut);
		} else {
			mHandler.opendir(defaultdir);
		}

		setDirectoryButtons();
		displayFreeSpace();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean hidden = mSettings.getBoolean(PREF_HIDDEN, true);
		boolean thumb = mSettings.getBoolean(PREF_PREVIEW, true);
		String value = mSettings.getString("sort", "1");
		String viewmode = mSettings.getString("viewmode", "1");

		int sort = Integer.parseInt(value);
		int viewm = Integer.parseInt(viewmode);

		mFileMag.setShowHiddenFiles(hidden);
		mFileMag.setSortType(sort);

		mHandler.setViewMode(viewm);
		mHandler.setShowThumbnails(thumb);

		// refresh
		mHandler.opendir(mFileMag.getCurrentDir());
	}

	// with this you get the 3 dot menu button
	private void getOverflowMenu() {

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// check if sdcard is avaible
	private void checkEnvironment() {

		File f = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (sdCardExist) {
			f = Environment.getExternalStorageDirectory();
			if (f != null) {
				startpath = f.getAbsolutePath();
			}

		} else {
			Toast.makeText(Main.this, getString(R.string.sdcardnotfound),
					Toast.LENGTH_LONG).show();
		}
	}

	private void setsearchbutton() {
		// Image Button ActionBar Search
		ImageButton mainButton = (ImageButton) findViewById(R.id.actionsearch);
		mainButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Main.this.onSearchRequested();
			}
		});

		mainButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				Toast.makeText(Main.this, getString(R.string.search),
						Toast.LENGTH_LONG).show();
				return false;
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		SearchIntent(intent);
	}

	public void SearchIntent(Intent intent) {
		setIntent(intent);

		boolean searchsuggestion = mSettings.getBoolean(PREF_SEARCH, false);

		// search action
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query.length() > 0) {
				new BackgroundWork(SEARCH_TYPE).execute(query);

				if (searchsuggestion == true) {
					SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
							this, SearchSuggestions.AUTHORITY,
							SearchSuggestions.MODE);
					suggestions.saveRecentQuery(query, null);
				} else {
					SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
							this, SearchSuggestions.AUTHORITY,
							SearchSuggestions.MODE);
					suggestions.clearHistory();
				}
			}
		}
	}

	// get ListView
	public void listview() {
		ListView listview = this.getListView();
		// go to top of ListView
		listview.setSelection(0);
	}

	public File currentDirectory() {
		return new File(mFileMag.getCurrentDir());
	}

	public void setDirectoryButtons() {

		HorizontalScrollView scrolltext = (HorizontalScrollView) findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		mDirectoryButtons.removeAllViews();

		String[] parts = currentDirectory().getAbsolutePath().split("/");

		int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
		int MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT;

		// Add home button separately
		Button bt = new Button(this);
		bt.setText("/");
		bt.setTextSize(directorytextsize);
		bt.setBackgroundResource(R.drawable.buttonaction);
		bt.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
				WRAP_CONTENT, Gravity.CENTER_VERTICAL));
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String rootsystem = "/";
				mHandler.opendir(rootsystem);
				setDirectoryButtons();

			}
		});

		mDirectoryButtons.addView(bt);
		FrameLayout fv = new FrameLayout(this);
		fv.setBackground(getResources().getDrawable(R.drawable.listmore));
		fv.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
				MATCH_PARENT, Gravity.CENTER_VERTICAL));

		mDirectoryButtons.addView(fv);

		// Add other buttons
		String dir = "";

		for (int i = 1; i < parts.length; i++) {
			dir += "/" + parts[i];

			FrameLayout fv1 = new FrameLayout(this);
			fv1.setBackground(getResources().getDrawable(R.drawable.listmore));
			fv1.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
					MATCH_PARENT, Gravity.CENTER_VERTICAL));
			Button b = new Button(this);
			b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
					WRAP_CONTENT, Gravity.CENTER_VERTICAL));
			b.setBackgroundResource(R.drawable.buttonaction);
			b.setText(parts[i]);
			b.setTextSize(directorytextsize);
			b.setTag(dir);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					String dir1 = (String) view.getTag();

					mHandler.updateDirectory(mFileMag.setHomeDir(dir1));
					setDirectoryButtons();
				}
			});

			b.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View view) {
					String dir1 = (String) view.getTag();
					savetoclip(dir1);
					return true;
				}
			});

			mDirectoryButtons.addView(b);
			mDirectoryButtons.addView(fv1);
			scrolltext.postDelayed(new Runnable() {
				public void run() {
					HorizontalScrollView hv = (HorizontalScrollView) findViewById(R.id.scroll_text);
					hv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
				}
			}, 100L);
		}
	}

	// save current string in clipboard
	private void savetoclip(String dir1) {
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = android.content.ClipData.newPlainText(
				"Copied Text", dir1);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(this,
				"'" + dir1 + "' " + getString(R.string.copiedtoclipboard),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("location", mFileMag.getCurrentDir());
	}

	// Returns the file that was selected to the intent that called this
	// activity. usually from the caller is another application.
	private void returnIntentResults(File data) {
		mReturnIntent = false;

		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		finish();
	}

	@Override
	public void onListItemClick(ListView parent, View view, int position,
			long id) {
		final String item = mHandler.getData(position);
		boolean multiSelect = mHandler.isMultiSelected();
		final File file = new File(mFileMag.getCurrentDir() + "/" + item);
		String item_ext = null;

		try {
			item_ext = item.substring(item.lastIndexOf("."), item.length());

		} catch (IndexOutOfBoundsException e) {
			item_ext = "";
		}

		// If the user has multi-select on, we just need to record the file not
		// make an intent for it.
		if (multiSelect) {
			mTable.addMultiPosition(position, file.getPath());

		} else {
			if (file.isDirectory()) {
				if (file.canRead()) {
					mHandler.updateDirectory(mFileMag.getNextDir(item, false));
					displayFreeSpace();
					setDirectoryButtons();
					listview();

					if (!mUseBackKey)
						mUseBackKey = true;

				} else {
					Toast.makeText(this,
							getString(R.string.cantreadfolderduetopermissions),
							Toast.LENGTH_SHORT).show();
				}
			}

			// music file selected--add more audio formats
			else if (item_ext.equalsIgnoreCase(".mp3")
					|| item_ext.equalsIgnoreCase(".m4a")
					|| item_ext.equalsIgnoreCase(".aiff")
					|| item_ext.equalsIgnoreCase(".wma")
					|| item_ext.equalsIgnoreCase(".caf")
					|| item_ext.equalsIgnoreCase(".flac")
					|| item_ext.equalsIgnoreCase(".mp4")
					|| item_ext.equalsIgnoreCase("m4p")
					|| item_ext.equalsIgnoreCase("amr")) {

				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					Intent i = new Intent();
					i.setAction(android.content.Intent.ACTION_VIEW);
					i.setDataAndType(Uri.fromFile(file), "audio/*");
					startActivity(i);
				}
			}

			// photo file selected
			else if (item_ext.equalsIgnoreCase(".jpeg")
					|| item_ext.equalsIgnoreCase(".jpg")
					|| item_ext.equalsIgnoreCase(".png")
					|| item_ext.equalsIgnoreCase(".gif")
					|| item_ext.equalsIgnoreCase(".raw")
					|| item_ext.equalsIgnoreCase(".psd")
					|| item_ext.equalsIgnoreCase(".bmp")
					|| item_ext.equalsIgnoreCase(".tiff")
					|| item_ext.equalsIgnoreCase(".tif")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent picIntent = new Intent();
						picIntent.setAction(android.content.Intent.ACTION_VIEW);
						picIntent.setDataAndType(Uri.fromFile(file), "image/*");
						startActivity(picIntent);
					}
				}
			}

			// video file selected--add more video formats
			else if (item_ext.equalsIgnoreCase(".m4v")
					|| item_ext.equalsIgnoreCase(".3gp")
					|| item_ext.equalsIgnoreCase(".wmv")
					|| item_ext.equalsIgnoreCase(".mp4")
					|| item_ext.equalsIgnoreCase(".mpeg")
					|| item_ext.equalsIgnoreCase(".mpg")
					|| item_ext.equalsIgnoreCase(".rm")
					|| item_ext.equalsIgnoreCase(".mov")
					|| item_ext.equalsIgnoreCase(".avi")
					|| item_ext.equalsIgnoreCase(".flv")
					|| item_ext.equalsIgnoreCase(".ogg")
					|| item_ext.equalsIgnoreCase(".wav")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent movieIntent = new Intent();
						movieIntent
								.setAction(android.content.Intent.ACTION_VIEW);
						movieIntent.setDataAndType(Uri.fromFile(file),
								"video/*");
						startActivity(movieIntent);
					}
				}
			}

			// ZIP file
			else if (item_ext.equalsIgnoreCase(".zip")) {

				if (mReturnIntent) {
					returnIntentResults(file);

				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					AlertDialog alert;
					CharSequence[] option = { getString(R.string.openwith),
							getString(R.string.extracthere) };

					builder.setTitle(getString(R.string.options));
					builder.setItems(option,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case 0:
										Intent zipIntent = new Intent();
										zipIntent
												.setAction(android.content.Intent.ACTION_VIEW);
										zipIntent.setDataAndType(
												Uri.fromFile(file),
												"application/zip");
										try {
											startActivity(zipIntent);
										} catch (Exception e) {
										}
										break;
									case 1:
										final String zipPath = mFileMag
												.getCurrentDir() + "/" + item;
										final String unZipPath = mFileMag
												.getCurrentDir() + "/unzipped/";

										new BackgroundWork(UNZIP_TYPE).execute(
												zipPath, unZipPath);
										break;
									}
								}
							});

					alert = builder.create();
					alert.show();
				}
			}

			// gzip files, this will be implemented later
			else if (item_ext.equalsIgnoreCase(".gzip")
					|| item_ext.equalsIgnoreCase(".rar")
					|| item_ext.equalsIgnoreCase(".tar")
					|| item_ext.equalsIgnoreCase(".tar.gz")
					|| item_ext.equalsIgnoreCase(".gz")) {

				if (mReturnIntent) {
					returnIntentResults(file);

				} else {
				}
			}

			// PDF file selected
			else if (item_ext.equalsIgnoreCase(".pdf")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent pdfIntent = new Intent();
						pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
						pdfIntent.setDataAndType(Uri.fromFile(file),
								"application/pdf");

						try {
							startActivity(pdfIntent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldntfindapdfviewver),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

			// Android Application file
			else if (item_ext.equalsIgnoreCase(".apk")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent apkIntent = new Intent();
						apkIntent.setAction(android.content.Intent.ACTION_VIEW);
						apkIntent.setDataAndType(Uri.fromFile(file),
								"application/vnd.android.package-archive");
						startActivity(apkIntent);
					}
				}
			}

			// HTML file
			else if (item_ext.equalsIgnoreCase(".html")
					|| item_ext.equalsIgnoreCase(".htm")
					|| item_ext.equalsIgnoreCase(".php")
					|| item_ext.equalsIgnoreCase(".xml")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent htmlIntent = new Intent();
						htmlIntent
								.setAction(android.content.Intent.ACTION_VIEW);
						htmlIntent.setDataAndType(Uri.fromFile(file),
								"text/html");

						try {
							startActivity(htmlIntent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldntfindahtmlviewver),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

			// Text file
			else if (item_ext.equalsIgnoreCase(".txt")
					|| item_ext.equalsIgnoreCase(".doc")
					|| item_ext.equalsIgnoreCase(".csv")
					|| item_ext.equalsIgnoreCase(".rtf")
					|| item_ext.equalsIgnoreCase(".text")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent txtIntent = new Intent();
						txtIntent.setAction(android.content.Intent.ACTION_VIEW);
						txtIntent.setDataAndType(Uri.fromFile(file),
								"text/plain");

						try {
							startActivity(txtIntent);
						} catch (ActivityNotFoundException e) {
							txtIntent.setType("text/*");
							startActivity(txtIntent);
						}
					}
				}
			}

			else {
				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent generic = new Intent();
						generic.setAction(android.content.Intent.ACTION_VIEW);
						generic.setDataAndType(Uri.fromFile(file), "text/plain");

						try {
							startActivity(generic);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldnt)
											+ getString(R.string.toopen)
											+ file.getName(),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		}
	}

	// Will be displayed when long clicking ListView
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);

		boolean multi_data = mHandler.hasMultiSelectData();
		AdapterContextMenuInfo _info = (AdapterContextMenuInfo) info;
		mSelectedListItem = mHandler.getData(_info.position);

		// is it a directory and is multi-select turned off
		if (mFileMag.isDirectory(mSelectedListItem)
				&& !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, D_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, D_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, D_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, D_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, D_MENU_ZIP, 0, getString(R.string.zipfolder));
			menu.add(0, D_MENU_PASTE, 0, getString(R.string.pasteintofolder))
					.setEnabled(mHoldingFile || multi_data);
			menu.add(0, D_MENU_SHORTCUT, 0, getString(R.string.createshortcut));
			menu.add(0, D_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, D_MENU_DETAILS, 0, getString(R.string.details));

			// is it a file and is multi-select turned off
		} else if (!mFileMag.isDirectory(mSelectedListItem)
				&& !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, F_MENU_OPENAS, 0, getString(R.string.openas));
			menu.add(0, F_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, F_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, F_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, F_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, F_MENU_ATTACH, 0, getString(R.string.share));
			menu.add(0, F_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, F_MENU_DETAILS, 0, getString(R.string.details));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case F_MENU_OPENAS:
			openaction();
			return true;

		case D_MENU_BOOKMARK:
		case F_MENU_BOOKMARK:
			try {
				File files = new File(mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);
				String path = files.getAbsolutePath();
				@SuppressWarnings("deprecation")
				Cursor query = managedQuery(Bookmarks.CONTENT_URI,
						new String[] { Bookmarks._ID }, Bookmarks.PATH + "=?",
						new String[] { path }, null);
				if (!query.moveToFirst()) {
					ContentValues values = new ContentValues();
					values.put(Bookmarks.NAME, files.getName());
					values.put(Bookmarks.PATH, path);
					getContentResolver().insert(Bookmarks.CONTENT_URI, values);
					Toast.makeText(Main.this, R.string.bookmarkadded,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(Main.this, R.string.bookmarkexist,
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Toast.makeText(Main.this, "Error", Toast.LENGTH_SHORT).show();
			}
			return true;

		case D_MENU_DELETE:
		case F_MENU_DELETE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(mSelectedListItem);
			builder.setIcon(R.drawable.warning);
			builder.setMessage(getString(R.string.cannotbeundoneareyousureyouwanttodelete));
			builder.setCancelable(false);

			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.setPositiveButton(getString(R.string.delete),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							new BackgroundWork(DELETE_TYPE).execute(mFileMag
									.getCurrentDir() + "/" + mSelectedListItem);
							displayFreeSpace();
						}
					});
			AlertDialog alert_d = builder.create();
			alert_d.show();
			return true;

		case D_MENU_RENAME:
			AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

			alert1.setTitle(getString(R.string.rename));

			// Set an EditText view to get user input
			final EditText input3 = new EditText(this);
			input3.setText(mSelectedListItem);
			alert1.setView(input3);

			alert1.setPositiveButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (input3.getText().length() < 1)
								dialog.dismiss();

							if (mFileMag.renameTarget(mFileMag.getCurrentDir()
									+ "/" + mSelectedListItem, input3.getText()
									.toString()) == 0) {
								Toast.makeText(Main.this,
										getString(R.string.folderwasrenamed),
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(Main.this,
										mSelectedListItem + "Error",
										Toast.LENGTH_LONG).show();

							dialog.dismiss();
							String temp = mFileMag.getCurrentDir();
							mHandler.updateDirectory(mFileMag.getNextDir(temp,
									true));
						}
					});

			alert1.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});

			alert1.show();
			return true;

		case F_MENU_DETAILS:

			filedetails();
			return true;

		case D_MENU_DETAILS:

			folderdetails();
			return true;

		case F_MENU_RENAME:
			AlertDialog.Builder alertf = new AlertDialog.Builder(this);

			alertf.setTitle(getString(R.string.rename));

			// Set an EditText view to get user input
			final EditText inputf = new EditText(this);
			inputf.setText(mSelectedListItem);
			alertf.setView(inputf);

			alertf.setPositiveButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (inputf.getText().length() < 1)
								dialog.dismiss();

							if (mFileMag.renameTarget(mFileMag.getCurrentDir()
									+ "/" + mSelectedListItem, inputf.getText()
									.toString()) == 0) {
								Toast.makeText(Main.this,
										getString(R.string.filewasrenamed),
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(Main.this,
										mSelectedListItem + "Error",
										Toast.LENGTH_LONG).show();

							dialog.dismiss();
							String temp = mFileMag.getCurrentDir();
							mHandler.updateDirectory(mFileMag.getNextDir(temp,
									true));
						}
					});

			alertf.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Cancel
						}
					});

			alertf.show();
			return true;

		case F_MENU_ATTACH:

			File sharefile = new File(mFileMag.getCurrentDir() + "/"
					+ mSelectedListItem);

			try {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, mSelectedListItem);
				i.putExtra(Intent.EXTRA_BCC, "");
				i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(sharefile));
				startActivity(Intent
						.createChooser(i, getString(R.string.share)));
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.error),
						Toast.LENGTH_SHORT).show();
			}
			return true;

		case F_MENU_MOVE:
		case D_MENU_MOVE:
		case F_MENU_COPY:
		case D_MENU_COPY:
			if (item.getItemId() == F_MENU_MOVE
					|| item.getItemId() == D_MENU_MOVE)
				mHandler.setDeleteAfterCopy(true);

			mHoldingFile = true;

			mCopiedTarget = mFileMag.getCurrentDir() + "/" + mSelectedListItem;

			return true;

		case D_MENU_PASTE:
			boolean multi_select = mHandler.hasMultiSelectData();

			if (multi_select) {
				mHandler.copyFileMultiSelect(mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);

			} else if (mHoldingFile && mCopiedTarget.length() > 1) {

				mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);
			}

			mHoldingFile = false;
			return true;

		case D_MENU_ZIP:
			String zipPath = mFileMag.getCurrentDir() + "/" + mSelectedListItem;

			new BackgroundWork(ZIP_TYPE).execute(zipPath);
			return true;

		case D_MENU_SHORTCUT:
			createfoldershortcut(mSelectedListItem);
			return true;
		}

		return false;
	}

	public void createfoldershortcut(String mSelectedListItem) {
		String path = mFileMag.getCurrentDir() + mSelectedListItem;

		Intent shortcutIntent = new Intent(Main.this, Shortcut.class);
		shortcutIntent.setAction(Intent.ACTION_MAIN);

		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mSelectedListItem);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(Main.this,
						R.drawable.ic_launcher));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		Main.this.sendBroadcast(addIntent);

		Toast.makeText(Main.this, getString(R.string.shortcutcreated),
				Toast.LENGTH_SHORT).show();

		SharedPreferences settings = getSharedPreferences(PREFS_SHORTCUT, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFS_SHORTCUT, path);
		editor.commit();
	}

	// Dialog with File details
	private void filedetails() {
		final int KB = 1024;
		final int MG = KB * KB;
		final int GB = MG * KB;

		// Get Infos for Dialog
		File file3 = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);

		if (file3.isFile()) {
			double size = file3.length();
			if (size > GB)
				setDisplay_size(String.format("%.2f GB", (double) size / GB));
			else if (size < GB && size > MG)
				setDisplay_size(String.format("%.2f MB", (double) size / MG));
			else if (size < MG && size > KB)
				setDisplay_size(String.format("%.2f KB", (double) size / KB));
			else
				setDisplay_size(String.format("%.2f B", (double) size));
		}

		SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

		// Only Dialog
		final Dialog dialog1 = new Dialog(Main.this);
		dialog1.setContentView(R.layout.details);
		dialog1.setTitle(R.string.details);
		dialog1.setCancelable(true);

		// set up text
		TextView textv = (TextView) dialog1.findViewById(R.id.name_label);
		textv.setText(mSelectedListItem);

		TextView textv0 = (TextView) dialog1.findViewById(R.id.path_label);
		textv0.setText(mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/");

		TextView textv1 = (TextView) dialog1.findViewById(R.id.time_stamp);
		textv1.setText(sdf1.format(file3.lastModified()));

		TextView textv2 = (TextView) dialog1.findViewById(R.id.total_size);
		textv2.setText(getDisplay_size() + "");

		// Set up Button
		Button button1 = (Button) dialog1.findViewById(R.id.quit);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog1.cancel();
			}
		});

		dialog1.show();
	}

	// Dialog with Folder details
	private void folderdetails() {
		// Get Info for Dialog
		File file1 = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

		// Only Dialog
		final Dialog dialog = new Dialog(Main.this);
		dialog.setContentView(R.layout.details);
		dialog.setTitle(R.string.details);
		dialog.setCancelable(true);

		// set up text
		TextView text = (TextView) dialog.findViewById(R.id.name_label);
		text.setText(mSelectedListItem);

		TextView text1 = (TextView) dialog.findViewById(R.id.path_label);
		text1.setText(mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/");

		TextView text2 = (TextView) dialog.findViewById(R.id.time_stamp);
		text2.setText(sdf.format(file1.lastModified()));

		// Set up Button
		Button button = (Button) dialog.findViewById(R.id.quit);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});

		dialog.show();

		new AsyncTask<String[], Long, Long>() {
			final TextView text3 = (TextView) dialog
					.findViewById(R.id.total_size);
			long size = 0;
			private String mDisplaySize;
			FileOperations flmg = new FileOperations();
			int KB = 1024;
			int MG = KB * KB;
			int GB = MG * KB;

			@Override
			protected void onPreExecute() {
				text3.setText("---");
			}

			@Override
			protected Long doInBackground(String[]... params) {
				String dir = mFileMag.getCurrentDir() + "/" + mSelectedListItem;
				size = flmg.getDirSize(dir);

				if (size > GB)
					mDisplaySize = String.format("%.2f GB", (double) size / GB);
				else if (size < GB && size > MG)
					mDisplaySize = String.format("%.2f MB", (double) size / MG);
				else if (size < MG && size > KB)
					mDisplaySize = String.format("%.2f KB", (double) size / KB);
				else
					mDisplaySize = String.format("%.2f B", (double) size);
				return null;
			}

			@Override
			protected void onPostExecute(Long result) {
				text3.setText(mDisplaySize);
			}
		}.execute();
	}

	// Options menu start here
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			finish();
			return true;

		case R.id.multis:
			mHandler.multiselect();
			startActionMode(mActionModeCallback);
			return true;

		case R.id.bookmarks:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			final Cursor bookmarksCursor = getBookmarks();

			builder.setTitle(R.string.bookmark);
			builder.setCursor(bookmarksCursor,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (bookmarksCursor.moveToPosition(item)) {
								String path = bookmarksCursor.getString(bookmarksCursor
										.getColumnIndex(Bookmarks.PATH));
								File file = new File(path);
								if (file != null) {
									if (file.isDirectory()) {
										mHandler.opendir(path);
										setDirectoryButtons();
										listview();
									} else {
										Intent i1 = new Intent();
										i1.setAction(android.content.Intent.ACTION_VIEW);
										i1.setDataAndType(Uri.fromFile(file),
												"*/*");
										startActivity(i1);
									}
								}
							} else {
								Toast.makeText(Main.this, R.string.error,
										Toast.LENGTH_SHORT).show();
							}
						}
					}, Bookmarks.NAME);
			builder.create();
			builder.show();
			return true;

		case R.id.create:
			final CharSequence[] create = { getString(R.string.newfile),
					getString(R.string.newd), };

			AlertDialog.Builder builder3 = new AlertDialog.Builder(Main.this);
			builder3.setItems(create, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int itemz) {
					switch (itemz) {

					case 0:
						createfile();
						return;

					case 1:
						final EditText input = new EditText(Main.this);

						AlertDialog.Builder alert = new AlertDialog.Builder(
								Main.this);
						alert.setTitle(getString(R.string.createnewfolder));
						alert.setMessage(R.string.createmsg);
						alert.setView(input);
						alert.setCancelable(true);
						alert.setPositiveButton(getString(R.string.ok),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										if (input.getText().length() >= 1) {
											if (mFileMag.createDir(
													mFileMag.getCurrentDir()
															+ "/", input
															.getText()
															.toString()) == 0)
												Toast.makeText(
														Main.this,
														input.getText()
																.toString()
																+ getString(R.string.created),
														Toast.LENGTH_LONG)
														.show();
											else
												Toast.makeText(
														Main.this,
														getString(R.string.newfolderwasnotcreated),
														Toast.LENGTH_SHORT)
														.show();
										}

										dialog.dismiss();
										String temp = mFileMag.getCurrentDir();
										mHandler.updateDirectory(mFileMag
												.getNextDir(temp, true));
									}
								});

						alert.setNegativeButton(getString(R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Cancel
									}
								});

						alert.show();
						return;
					}
				}
			});
			AlertDialog alertmore = builder3.create();
			alertmore.show();
			return true;

		case R.id.finfo:
			Intent info = new Intent(Main.this, DirectoryInfo.class);
			info.putExtra("PATH_NAME", mFileMag.getCurrentDir());
			Main.this.startActivity(info);
			return true;

		case R.id.appmanager:
			Intent intent1 = new Intent(Main.this, AppManager.class);
			startActivity(intent1);
			return true;

		case R.id.settings:
			Intent intent2 = new Intent(Main.this, Settings.class);
			startActivity(intent2);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressWarnings("deprecation")
	private Cursor getBookmarks() {
		return managedQuery(Bookmarks.CONTENT_URI, new String[] {
				Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH, }, null, null,
				null);
	}

	private void openaction() {
		final File file = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);
		final CharSequence[] openAsOperations = { getString(R.string.text),
				getString(R.string.image), getString(R.string.video),
				getString(R.string.music), getString(R.string.file) };

		AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
		builder.setItems(openAsOperations,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dInterface, int item) {

						switch (item) {
						case 0:
							Intent txtIntent = new Intent();
							txtIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							txtIntent.setDataAndType(Uri.fromFile(file),
									"text/plain");
							startActivity(txtIntent);
							break;
						case 1:
							Intent imageIntent = new Intent();
							imageIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							imageIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(imageIntent);
							break;

						case 2:
							Intent movieIntent = new Intent();
							movieIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							movieIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(movieIntent);
							break;
						case 3:

							Intent i = new Intent();
							i.setAction(android.content.Intent.ACTION_VIEW);
							i.setDataAndType(Uri.fromFile(file), "audio/*");
							startActivity(i);
							break;

						case 4:
							Intent i1 = new Intent();
							i1.setAction(android.content.Intent.ACTION_VIEW);
							i1.setDataAndType(Uri.fromFile(file), "*/*");
							startActivity(i1);
							break;
						}
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void createfile() {
		AlertDialog.Builder alertf = new AlertDialog.Builder(this);
		alertf.setTitle(getString(R.string.newfile));

		final EditText editt = new EditText(this);
		alertf.setView(editt);
		alertf.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String path = editt.getText().toString();

						File file = new File(mFileMag.getCurrentDir()
								+ File.separator + path + ".txt");
						try {
							file.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
						}

						String temp = mFileMag.getCurrentDir();
						mHandler.updateDirectory(mFileMag
								.getNextDir(temp, true));
					}
				});

		alertf.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alertf.show();
	}

	public void setDisplay_size(String display_size) {
		this.display_size = display_size;
	}

	public String getDisplay_size() {
		return display_size;
	}

	private void displayFreeSpace() {
		ProgressBar statusBar = (ProgressBar) findViewById(R.id.progressBar);

		if (statusBar == null)
			return;

		int total = ProgressbarClass.totalMemory();
		int free = ProgressbarClass.freeMemory();

		if (free / total > 0.5) {
			statusBar.setVisibility(View.GONE);
		} else {
			statusBar.setVisibility(View.VISIBLE);
			statusBar.setMax(total);
			statusBar.setProgress(free);
		}
	}

	public static class ProgressbarClass extends Application {

		@SuppressWarnings("deprecation")
		public static int totalMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
			return Total;
		}

		@SuppressWarnings("deprecation")
		public static int freeMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
			return Free;
		}

		@SuppressWarnings("deprecation")
		public static int busyMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
			int Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
			int Busy = Total - Free;
			return Busy;
		}
	}

	// this will start when multibutton is clicked
	public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			getMenuInflater().inflate(R.menu.actionmode, menu);
			mActionMode = mode;
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// Set Title
			mode.setTitle(R.string.choosefiles);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.actiondelete:
				try {
					multidelete();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actionshare:
				try {
					multishare();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actioncopy:
				try {
					mHandler.multicopy();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actionmove:
				try {
					mHandler.multimove();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actionzip:
				try {
					multizipfiles();

					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			mActionMode = null;
			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
			}
		}
	};

	// multiactions
	public void multishare() {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		int length = mHandler.mMultiSelectData.size();

		for (int i = 0; i < length; i++) {
			File file = new File(mHandler.mMultiSelectData.get(i));
			uris.add(Uri.fromFile(file));
		}

		Intent mail_int = new Intent();
		mail_int.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
		mail_int.setType("text/plain");
		mail_int.putExtra(Intent.EXTRA_BCC, "");
		mail_int.putExtra(Intent.EXTRA_SUBJECT, " ");
		mail_int.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		mHandler.mContext.startActivity(Intent.createChooser(mail_int,
				getString(R.string.share)));
		mHandler.mDelegate.killMultiSelect(true);
	}

	public void multidelete() {
		if (mHandler.mMultiSelectData == null
				|| mHandler.mMultiSelectData.isEmpty()) {
			mHandler.mDelegate.killMultiSelect(true);
		}

		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		int size = mHandler.mMultiSelectData.size();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.delete) + " ("
				+ String.valueOf(size) + ")");
		builder.setMessage(R.string.cannotbeundoneareyousureyouwanttodelete);
		builder.setCancelable(true);
		builder.setPositiveButton((R.string.delete),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new BackgroundWork(DELETE_TYPE).execute(data);
						mHandler.mDelegate.killMultiSelect(true);
					}
				});
		builder.setNegativeButton((R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mHandler.mDelegate.killMultiSelect(true);
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private void multizipfiles() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		multidata = data;

		final String dir = mFileMag.getCurrentDir();
		new BackgroundWork(MULTIZIP_TYPE).execute(dir);
	}

	// Backgroundwork for:
	// search
	// delete
	// multizip
	// unzip
	// ZIP Folder
	private class BackgroundWork extends
			AsyncTask<String, Void, ArrayList<String>> {
		public ProgressDialog pr_dialog;
		private String file_name;
		private int type;

		private BackgroundWork(int type) {
			this.type = type;
		}

		// This will show a Dialog while Action is running in Background
		@Override
		protected void onPreExecute() {

			switch (type) {
			case SEARCH_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.search));
				pr_dialog.setCancelable(true);
				break;
			case DELETE_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.deleting));
				pr_dialog.setCancelable(false);
				break;
			case MULTIZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.zipping));
				pr_dialog.setCancelable(false);
				break;
			case UNZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.unzipping));
				pr_dialog.setCancelable(false);
				break;
			case ZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.zipping));
				pr_dialog.setCancelable(false);
				break;
			}
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {

			switch (type) {
			case SEARCH_TYPE:
				file_name = params[0];
				ArrayList<String> found = mFileMag.searchInDirectory(
						searchdirectory, file_name);
				return found;
			case DELETE_TYPE:
				int size = params.length;

				for (int i = 0; i < size; i++)
					mFileMag.deleteTarget(params[i]);

				return null;
			case MULTIZIP_TYPE:
				final String zipfile = mFileMag.getCurrentDir() + "/"
						+ "zipfile.zip";

				final Compress compress = new Compress(multidata, zipfile);

				try {
					compress.zip(params[0]);
				} catch (Exception e) {

				}
				return null;
			case UNZIP_TYPE:
				final Decompress decompress = new Decompress(params[0],
						params[1]);
				try {
					decompress.unzip();
				} catch (Exception e) {

				}
				return null;
			case ZIP_TYPE:
				mFileMag.createZipFile(params[0]);
				return null;
			}
			return null;
		}

		// This is called when the background thread is finished
		@Override
		protected void onPostExecute(final ArrayList<String> file) {
			final CharSequence[] names;
			int len = file != null ? file.size() : 0;

			switch (type) {
			case SEARCH_TYPE:
				if (len == 0) {
					Toast.makeText(Main.this, R.string.itcouldntbefound,
							Toast.LENGTH_SHORT).show();

				} else {
					names = new CharSequence[len];

					for (int i = 0; i < len; i++) {
						String entry = file.get(i);
						names[i] = entry.substring(entry.lastIndexOf("/") + 1,
								entry.length());
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(
							Main.this);
					builder.setTitle(R.string.foundfiles);
					builder.setItems(names,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int position) {
									String path = file.get(position);
									mHandler.opendir(path.substring(0,
											path.lastIndexOf("/")));
									setDirectoryButtons();
								}
							});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				pr_dialog.dismiss();
				break;
			case DELETE_TYPE:
				if (mHandler.mMultiSelectData != null
						&& !mHandler.mMultiSelectData.isEmpty()) {
					mHandler.mMultiSelectData.clear();
					mHandler.multi_select_flag = false;
				}

				mHandler.updateDirectory(mFileMag.getNextDir(
						mFileMag.getCurrentDir(), true));
				pr_dialog.dismiss();
				break;
			case MULTIZIP_TYPE:
				mHandler.updateDirectory(mFileMag.getNextDir(
						mFileMag.getCurrentDir(), true));
				pr_dialog.dismiss();
				break;
			case UNZIP_TYPE:
				mHandler.updateDirectory(mFileMag.getNextDir(
						mFileMag.getCurrentDir(), true));
				pr_dialog.dismiss();
				break;
			case ZIP_TYPE:
				mHandler.updateDirectory(mFileMag.getNextDir(
						mFileMag.getCurrentDir(), true));
				pr_dialog.dismiss();
				break;
			}
		}
	}

	// On back pressed Actions
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		String current = mFileMag.getCurrentDir();

		if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !current.equals("/")) {

			mHandler.updateDirectory(mFileMag.getPreviousDir());
			setDirectoryButtons();
			listview();

			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& current.equals("/")) {
			Toast.makeText(Main.this, getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			mUseBackKey = false;
			setDirectoryButtons();

			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& current.equals("/")) {
			finish();

			return false;
		}
		return false;
	}
}