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

import java.io.File;
import java.util.ArrayList;

import com.dnielfe.manager.adapters.DrawerListAdapter;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DeleteFilesDialog;
import com.dnielfe.manager.dialogs.FilePropertiesDialog;
import com.dnielfe.manager.dialogs.RenameDialog;
import com.dnielfe.manager.dialogs.UnzipDialog;
import com.dnielfe.manager.dialogs.ZipFilesDialog;
import com.dnielfe.manager.fileobserver.FileObserverCache;
import com.dnielfe.manager.fileobserver.MultiFileObserver;
import com.dnielfe.manager.preview.MimeTypes;
import com.dnielfe.manager.tasks.PasteTask;
import com.dnielfe.manager.tasks.ZipFolderTask;
import com.dnielfe.manager.utils.Bookmarks;
import com.dnielfe.manager.utils.ClipBoard;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;

public final class Main extends ListActivity implements
		MultiFileObserver.OnEventListener {

	public static final String ACTION_WIDGET = "com.dnielfe.manager.Main.ACTION_WIDGET";
	public static final String EXTRA_SHORTCUT = "shortcut_path";

	public static final String PREF_DIR = "defaultdir";

	private static final int D_MENU_SHORTCUT = 3;
	private static final int D_MENU_BOOKMARK = 4;
	private static final int D_MENU_DELETE = 6;
	private static final int D_MENU_RENAME = 7;
	private static final int D_MENU_COPY = 8;
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

	private static final int directorytextsize = 16;

	private static Handler sHandler;
	private MultiFileObserver mObserver;
	private FileObserverCache mObserverCache;
	private Runnable mLastRunnable;

	private static EventHandler mHandler;
	private static EventHandler.TableRow mTable;

	private boolean mReturnIntent = false;
	private boolean mUseBackKey = true;

	public ActionMode mActionMode;

	private static String mSelectedListItem;
	private static String[] drawerTitles;

	private SharedPreferences mSettings;
	private String defaultdir;
	private View mActionView;
	private LinearLayout mDirectoryButtons, mDrawer;
	private MenuItem mMenuItemPaste;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList, mListView;
	private ActionBar mActionBar;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		if (sHandler == null) {
			sHandler = new Handler(this.getMainLooper());
		}

		checkEnvironment();
		setupDrawer();

		// start EventHandler
		mHandler = new EventHandler(this);

		// new ArrayAdapter
		mTable = mHandler.new TableRow();

		mObserverCache = FileObserverCache.getInstance();

		initList();

		SearchIntent(getIntent());

		if (savedInstanceState != null) {
			// get directory when you rotate your phone
			defaultdir = savedInstanceState.getString("location");
		} else {
			// If other apps want to choose a file you do it with this action
			if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				mReturnIntent = true;

			} else if (getIntent().getAction().equals(ACTION_WIDGET)) {
				EventHandler.updateDirectory(mHandler.getNextDir(getIntent()
						.getExtras().getString("folder"), true));
			}

			try {
				String shortcut = getIntent().getStringExtra(EXTRA_SHORTCUT);
				File dir = new File(shortcut);

				if (dir.exists() && dir.isDirectory())
					defaultdir = shortcut;
				else
					// you need to call it when shortcut-dir not exists
					getDefaultDir();

			} catch (Exception e) {
				getDefaultDir();
			}
		}

		File dir = new File(defaultdir);

		if (dir.exists() && dir.isDirectory())
			navigateTo(dir.getAbsolutePath());
	}

	@Override
	public void onResume() {
		super.onResume();

		mHandler.loadPreferences();

		// refresh directory
		EventHandler.refreshDir(EventHandler.getCurrentDir());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("location", EventHandler.getCurrentDir());
		super.onSaveInstanceState(outState);
	}

	private void initList() {
		// get ListView
		mListView = getListView();

		// Set ListAdapters
		mHandler.setListAdapter(mTable);
		mListView.setAdapter(mTable);

		// register context menu for our ListView
		registerForContextMenu(mListView);
	}

	private void getDefaultDir() {
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);

		// get default directory from preferences
		defaultdir = mSettings.getString("defaultdir", Environment
				.getExternalStorageDirectory().getPath());
	}

	private void navigateTo(String path) {
		if (mObserver != null) {
			mObserver.stopWatching();
			mObserver.removeOnEventListener(this);
		}

		try {
			EventHandler.refreshDir(path);
		} catch (Exception e) {
			Toast.makeText(Main.this, getString(R.string.cantreadfolder),
					Toast.LENGTH_SHORT).show();
		}

		mObserver = mObserverCache.getOrCreate(path);
		mObserver.addOnEventListener(this);
		mObserver.startWatching();

		// go to the top of the ListView
		mListView.setSelection(0);

		setDirectoryButtons();
	}

	private void setupDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

		// Set shadow of navigation drawer
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		// get Titles from Array
		drawerTitles = getResources()
				.getStringArray(R.array.drawerTitles_array);

		// Create Adapter with MenuListAdapter
		DrawerListAdapter mMenuAdapter = new DrawerListAdapter(this,
				drawerTitles);
		mDrawerList.setAdapter(mMenuAdapter);

		// Prepare ActionBar
		mActionBar = getActionBar();
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setDisplayShowTitleEnabled(false);

		// ActionBar Layout
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mActionView = inflator.inflate(R.layout.actionbar, null);
		mActionBar.setCustomView(mActionView);
		mActionBar.show();

		// Add Navigation Drawer to ActionBar
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {

			@Override
			public void onDrawerClosed(View view) {
				mActionBar.setDisplayShowTitleEnabled(false);
				mActionBar.setCustomView(mActionView);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				mActionBar.setDisplayShowTitleEnabled(true);
				mActionBar.setCustomView(null);
				invalidateOptionsMenu();
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				selectItem(position);
			}
		});
	}

	private void selectItem(int position) {
		switch (position) {
		case 0:
			// FolderInfo
			Intent info = new Intent(Main.this, DirectoryInfo.class);
			info.putExtra("PATH_NAME", EventHandler.getCurrentDir());
			Main.this.startActivity(info);
			break;
		case 1:
			// AppManager
			Intent intent1 = new Intent(Main.this, AppManager.class);
			startActivity(intent1);
			break;
		case 2:
			// Preferences
			Intent intent2 = new Intent(Main.this, Settings.class);
			startActivity(intent2);
			break;
		case 3:
			// exit
			finish();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	// Returns the file that was selected to the intent that called this
	// activity. usually from the caller is another application.
	private void returnIntentResults(File data) {
		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		finish();
	}

	// check if SDcard is present
	private void checkEnvironment() {
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);

		if (!sdCardExist) {
			Toast.makeText(Main.this, getString(R.string.sdcardnotfound),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		SearchIntent(intent);
	}

	@Override
	public void onEvent(int event, String path) {
		// this will automatically update the directory when an action like this
		// will be performed
		switch (event & FileObserver.ALL_EVENTS) {
		case FileObserver.CREATE:
		case FileObserver.CLOSE_WRITE:
		case FileObserver.MODIFY:
		case FileObserver.MOVE_SELF:
		case FileObserver.ATTRIB:
		case FileObserver.DELETE:
		case FileObserver.DELETE_SELF:
		case FileObserver.MOVED_TO:
			sHandler.removeCallbacks(mLastRunnable);
			sHandler.post(mLastRunnable = new NavigateRunnable(EventHandler
					.getCurrentDir()));
			break;
		}
	}

	private void SearchIntent(Intent intent) {
		setIntent(intent);

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query.length() > 0) {
				// start search in background
				new SearchTask().execute(query);
				// TODO move SearchTask() to tasks package
			}
		}
	}

	// set directory buttons showing path
	private void setDirectoryButtons() {
		File currentDirectory = new File(EventHandler.getCurrentDir());

		HorizontalScrollView scrolltext = (HorizontalScrollView) findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		mDirectoryButtons.removeAllViews();

		String[] parts = currentDirectory.getAbsolutePath().split("/");

		int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
		int MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT;

		// Add home button separately
		Button bt = new Button(this, null, android.R.attr.borderlessButtonStyle);
		bt.setText("/");
		bt.setTextSize(directorytextsize);
		bt.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
				WRAP_CONTENT, Gravity.CENTER_VERTICAL));
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				navigateTo("/");
			}
		});

		mDirectoryButtons.addView(bt);

		// Add other buttons
		String dir = "";

		for (int i = 1; i < parts.length; i++) {
			dir += "/" + parts[i];

			FrameLayout fv1 = new FrameLayout(this);
			fv1.setBackground(getResources().getDrawable(R.drawable.listmore));
			fv1.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
					MATCH_PARENT, Gravity.CENTER_VERTICAL));

			Button b = new Button(this, null,
					android.R.attr.borderlessButtonStyle);
			b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
					WRAP_CONTENT, Gravity.CENTER_VERTICAL));
			b.setText(parts[i]);
			b.setTextSize(directorytextsize);
			b.setTag(dir);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					String dir1 = (String) view.getTag();
					navigateTo(dir1);
				}
			});

			b.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View view) {
					String dir1 = (String) view.getTag();
					savetoclip(dir1);
					return true;
				}
			});

			mDirectoryButtons.addView(fv1);
			mDirectoryButtons.addView(b);
			scrolltext.postDelayed(new Runnable() {
				public void run() {
					HorizontalScrollView hv = (HorizontalScrollView) findViewById(R.id.scroll_text);
					hv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
				}
			}, 100L);
		}
	}

	// save current string in ClipBoard
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
	public void onListItemClick(ListView parent, View view, int position,
			long id) {
		final String item = mHandler.getData(position);
		boolean multiSelect = mHandler.isMultiSelected();
		final File file = new File(EventHandler.getCurrentDir() + "/" + item);

		// If the user has multi-select on, we just need to record the file not
		// make an intent for it.
		if (multiSelect) {
			mTable.addMultiPosition(position, file.getPath(), true);

		} else {
			if (file.isDirectory()) {
				navigateTo(file.getAbsolutePath());

				if (!mUseBackKey)
					mUseBackKey = true;

			} else {
				listItemAction(file, item);
			}
		}
	}

	private void listItemAction(File file, String item) {
		String item_ext = null;
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		try {
			item_ext = item.substring(item.lastIndexOf("."), item.length());

		} catch (IndexOutOfBoundsException e) {
			item_ext = "";
		}

		// music file selected
		if (item_ext.equalsIgnoreCase(".mp3")
				|| item_ext.equalsIgnoreCase(".m4a")
				|| item_ext.equalsIgnoreCase(".aiff")
				|| item_ext.equalsIgnoreCase(".wma")
				|| item_ext.equalsIgnoreCase(".caf")
				|| item_ext.equalsIgnoreCase(".flac")
				|| item_ext.equalsIgnoreCase(".ogg")
				|| item_ext.equalsIgnoreCase(".m4p")
				|| item_ext.equalsIgnoreCase(".amr")
				|| item_ext.equalsIgnoreCase(".aac")
				|| item_ext.equalsIgnoreCase(".wav")) {

			if (mReturnIntent) {
				returnIntentResults(file);
			} else {
				try {
					openIntent.setDataAndType(Uri.fromFile(file), "audio/*");
					startActivity(openIntent);
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.cantopenfile),
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		// imgae file selected
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
					try {
						openIntent
								.setDataAndType(Uri.fromFile(file), "image/*");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		// video file selected
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
				|| item_ext.equalsIgnoreCase(".ogg")) {

			if (file.exists()) {
				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					try {
						openIntent
								.setDataAndType(Uri.fromFile(file), "video/*");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		// ZIP file
		else if (item_ext.equalsIgnoreCase(".zip")) {

			if (mReturnIntent) {
				returnIntentResults(file);

			} else {
				final String zipPath = EventHandler.getCurrentDir() + "/"
						+ item;

				final DialogFragment dialog = UnzipDialog.instantiate(zipPath);
				dialog.show(getFragmentManager(), "dialog");
			}
		}

		// other archive packages
		else if (item_ext.equalsIgnoreCase(".rar")
				|| item_ext.equalsIgnoreCase(".gzip")
				|| item_ext.equalsIgnoreCase(".tar")
				|| item_ext.equalsIgnoreCase(".tar.gz")
				|| item_ext.equalsIgnoreCase(".gz")) {

			if (mReturnIntent) {
				returnIntentResults(file);
			} else {
				// nothing - need to add more features
			}
		}

		// PDF file selected
		else if (item_ext.equalsIgnoreCase(".pdf")) {

			if (file.exists()) {
				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					try {
						openIntent.setDataAndType(Uri.fromFile(file),
								"application/pdf");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
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
					try {
						openIntent.setDataAndType(Uri.fromFile(file),
								"application/vnd.android.package-archive");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
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
					try {
						openIntent.setDataAndType(Uri.fromFile(file),
								"text/html");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		// Text file
		else if (item_ext.equalsIgnoreCase(".txt")
				|| item_ext.equalsIgnoreCase(".asc")
				|| item_ext.equalsIgnoreCase(".doc")
				|| item_ext.equalsIgnoreCase(".csv")
				|| item_ext.equalsIgnoreCase(".prop")
				|| item_ext.equalsIgnoreCase(".rtf")
				|| item_ext.equalsIgnoreCase(".text")) {

			if (file.exists()) {
				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					try {
						openIntent.setDataAndType(Uri.fromFile(file),
								"text/plain");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		else {
			if (file.exists()) {
				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					try {
						openIntent.setDataAndType(Uri.fromFile(file), "*/*");
						startActivity(openIntent);
					} catch (Exception e) {
						Toast.makeText(Main.this,
								getString(R.string.cantopenfile),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}

	// Options menu start here
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		mMenuItemPaste = menu.findItem(R.id.paste);
		mMenuItemPaste.setVisible(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawer)) {
				mDrawerLayout.closeDrawer(mDrawer);
			} else {
				mDrawerLayout.openDrawer(mDrawer);
			}
			return true;
		case R.id.create:
			if (mDrawerLayout.isDrawerOpen(mDrawer))
				mDrawerLayout.closeDrawer(mDrawer);
			createDialog();
			return true;
		case R.id.bookmarks:
			bookmarkDialog();
			return true;
		case R.id.multis:
			mHandler.multiselect();
			mActionMode = startActionMode(new MultiController());
			return true;

		case R.id.search:
			this.onSearchRequested();
			return true;

		case R.id.paste:
			final PasteTask ptc = new PasteTask(this,
					EventHandler.getCurrentDir());
			ptc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			mMenuItemPaste.setVisible(false);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private Cursor getBookmarks() {
		return getContentResolver()
				.query(Bookmarks.CONTENT_URI,
						new String[] { Bookmarks._ID, Bookmarks.NAME,
								Bookmarks.PATH, }, null, null, null);
	}

	// Will be displayed when long clicking ListView
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);

		AdapterContextMenuInfo _info = (AdapterContextMenuInfo) info;
		mSelectedListItem = mHandler.getData(_info.position);
		File item = new File(EventHandler.getCurrentDir() + "/"
				+ mSelectedListItem);

		// is it a directory and is multi-select turned off
		if (item.isDirectory() && !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, D_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, D_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, D_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, D_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, D_MENU_ZIP, 0, getString(R.string.zipfolder));
			menu.add(0, D_MENU_SHORTCUT, 0, getString(R.string.shortcut));
			menu.add(0, D_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, D_MENU_DETAILS, 0, getString(R.string.details));

			// is it a file and is multi-select turned off
		} else if (!item.isDirectory() && !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
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
		final File file = new File(EventHandler.getCurrentDir() + "/"
				+ mSelectedListItem);

		switch (item.getItemId()) {
		case D_MENU_BOOKMARK:
		case F_MENU_BOOKMARK:
			try {
				String path = file.getAbsolutePath();
				Cursor query = getContentResolver().query(
						Bookmarks.CONTENT_URI, new String[] { Bookmarks._ID },
						Bookmarks.PATH + "=?", new String[] { path }, null);
				if (!query.moveToFirst()) {
					ContentValues values = new ContentValues();
					values.put(Bookmarks.NAME, file.getName());
					values.put(Bookmarks.PATH, path);
					getContentResolver().insert(Bookmarks.CONTENT_URI, values);
					Toast.makeText(Main.this, R.string.bookmarkadded,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(Main.this, R.string.bookmarkexist,
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Toast.makeText(Main.this, R.string.error, Toast.LENGTH_SHORT)
						.show();
			}
			return true;

		case D_MENU_DELETE:
		case F_MENU_DELETE:
			final DialogFragment dialog1 = DeleteFilesDialog
					.instantiate(new String[] { file.getPath() });
			dialog1.show(getFragmentManager(), "dialog");
			return true;

		case D_MENU_DETAILS:
		case F_MENU_DETAILS:
			final DialogFragment dialog = FilePropertiesDialog
					.instantiate(file);
			dialog.show(getFragmentManager(), "dialog");
			return true;

		case D_MENU_RENAME:
		case F_MENU_RENAME:
			final DialogFragment dialog3 = RenameDialog.instantiate(
					file.getPath(), mSelectedListItem);
			dialog3.show(getFragmentManager(), "dialog");
			return true;

		case F_MENU_ATTACH:
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType(MimeTypes.getMimeType(file));
			i.putExtra(Intent.EXTRA_SUBJECT, mSelectedListItem);
			i.putExtra(Intent.EXTRA_BCC, "");
			i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
			startActivity(Intent.createChooser(i, getString(R.string.share)));
			return true;

		case F_MENU_MOVE:
		case D_MENU_MOVE:
			String[] copyMove = new String[] { file.getPath() };

			ClipBoard.cutMove(copyMove);
			mMenuItemPaste.setVisible(true);
			return true;

		case F_MENU_COPY:
		case D_MENU_COPY:
			String[] copyFile = new String[] { file.getPath() };

			ClipBoard.cutCopy(copyFile);
			mMenuItemPaste.setVisible(true);
			return true;

		case D_MENU_ZIP:
			final ZipFolderTask task = new ZipFolderTask(this);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					file.getPath());
			return true;

		case D_MENU_SHORTCUT:
			FileUtils.createShortcut(this, file.getPath(), mSelectedListItem);
			return true;
		}
		return false;
	}

	// Show a Dialog with your bookmarks
	private void bookmarkDialog() {
		if (mDrawerLayout.isDrawerOpen(mDrawer))
			mDrawerLayout.closeDrawer(mDrawer);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		final Cursor bookmarksCursor = getBookmarks();

		builder.setTitle(R.string.bookmark);
		builder.setCursor(bookmarksCursor,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (mDrawerLayout.isDrawerOpen(mDrawer))
							mDrawerLayout.closeDrawer(mDrawer);

						if (bookmarksCursor.moveToPosition(item)) {
							String path = bookmarksCursor
									.getString(bookmarksCursor
											.getColumnIndex(Bookmarks.PATH));
							File file = new File(path);
							if (file != null) {
								if (file.isDirectory()) {
									navigateTo(path);
								} else {
									listItemAction(file, path);
								}
							}
						} else {
							Toast.makeText(Main.this,
									getString(R.string.error),
									Toast.LENGTH_SHORT).show();
						}
					}
				}, Bookmarks.NAME);
		builder.create();
		builder.show();
	}

	private void createDialog() {
		final CharSequence[] create = { getString(R.string.newfile),
				getString(R.string.newd), };

		AlertDialog.Builder builder3 = new AlertDialog.Builder(Main.this);
		builder3.setItems(create, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int itemz) {
				switch (itemz) {

				case 0:
					final DialogFragment dialog1 = new CreateFileDialog();
					dialog1.show(getFragmentManager(), "dialog");
					return;

				case 1:
					final DialogFragment dialog2 = new CreateFolderDialog();
					dialog2.show(getFragmentManager(), "dialog");
					return;
				}
			}
		});
		AlertDialog alertmore = builder3.create();
		alertmore.show();
	}

	// this will start when multiButton is clicked
	public class MultiController implements ActionMode.Callback {

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
					mode.finish();
				}
				return true;

			case R.id.actionshare:
				try {
					multishare();
					mode.finish();
				} catch (Exception e) {
					mode.finish();
				}
				return true;

			case R.id.actioncopy:
				try {
					multicopy();
					mode.finish();
				} catch (Exception e) {
					mode.finish();
				}
				return true;

			case R.id.actionmove:
				try {
					multimove();
					mode.finish();
				} catch (Exception e) {
					mode.finish();
				}
				return true;

			case R.id.actionzip:
				try {
					multizipfiles();
					mode.finish();
				} catch (Exception e) {
					mode.finish();
				}
				return true;

			case R.id.actionall:
				for (int i = 0; i < EventHandler.mDataSource.size(); i++) {
					File file = new File(EventHandler.getCurrentDir() + "/"
							+ EventHandler.mDataSource.get(i));

					mTable.addMultiPosition(i, file.getPath(), false);
				}
				return true;

			case R.id.actionrmall:
				mTable.killMultiSelect(true, false);
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			mActionMode = null;
			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true, true);
				mHandler.multi_select_flag = false;
			}
		}
	};

	// share multiple files
	private void multishare() {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		int length = mHandler.mMultiSelectData.size();

		for (int i = 0; i < length; i++) {
			File file = new File(mHandler.mMultiSelectData.get(i));
			uris.add(Uri.fromFile(file));
		}

		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType(MimeTypes.ALL_MIME_TYPES);
		intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		startActivity(Intent.createChooser(intent, getString(R.string.share)));
	}

	private void multidelete() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		final DialogFragment dialog = DeleteFilesDialog.instantiate(data);
		dialog.show(getFragmentManager(), "dialog");
	}

	private void multizipfiles() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		final DialogFragment dialog = ZipFilesDialog.instantiate(data);
		dialog.show(getFragmentManager(), "dialog");
	}

	private void multicopy() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		ClipBoard.cutCopy(data);
		mMenuItemPaste.setVisible(true);
	}

	private void multimove() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		ClipBoard.cutMove(data);
		mMenuItemPaste.setVisible(true);
	}

	private class SearchTask extends AsyncTask<String, Void, ArrayList<String>> {
		public ProgressDialog pr_dialog = null;
		private String file_name;

		private SearchTask() {
			if (mDrawerLayout.isDrawerOpen(mDrawer))
				mDrawerLayout.closeDrawer(mDrawer);
		}

		// This will show a Dialog while Action is running in Background
		@Override
		protected void onPreExecute() {
			pr_dialog = ProgressDialog.show(Main.this, "",
					getString(R.string.search));
			pr_dialog.setCanceledOnTouchOutside(true);
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			file_name = params[0];

			ArrayList<String> found = FileUtils.searchInDirectory(
					EventHandler.getCurrentDir(), file_name);
			return found;
		}

		// This is called when the background thread is finished
		@Override
		protected void onPostExecute(final ArrayList<String> file) {
			final CharSequence[] names;
			int len = file != null ? file.size() : 0;

			pr_dialog.dismiss();

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

				AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
				builder.setTitle(R.string.foundfiles);
				builder.setItems(names, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int position) {
						String path = file.get(position);
						navigateTo(path.substring(0, path.lastIndexOf("/")));
					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
	}

	private static final class NavigateRunnable implements Runnable {
		private final String target;

		NavigateRunnable(final String path) {
			this.target = path;
		}

		@Override
		public void run() {
			EventHandler.refreshDir(target);
		}
	}

	// On back pressed Actions
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		String current = EventHandler.getCurrentDir();
		File file = new File(current);
		String parent = file.getParent();

		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !current.equals("/")) {

			navigateTo(parent);
			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& current.equals("/")) {
			Toast.makeText(Main.this, getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			if (!ClipBoard.isEmpty()) {
				ClipBoard.unlock();
				ClipBoard.clear();
				mMenuItemPaste.setVisible(false);
			}

			mUseBackKey = false;
			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& current.equals("/")) {
			if (mObserver != null) {
				mObserver.stopWatching();
				mObserver.removeOnEventListener(this);
			}
			finish();
			return false;
		}
		return false;
	}
}
