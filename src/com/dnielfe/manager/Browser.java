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

import com.dnielfe.manager.adapters.BookmarksAdapter;
import com.dnielfe.manager.adapters.DrawerListAdapter;
import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.controller.ActionModeController;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DirectoryInfoDialog;
import com.dnielfe.manager.dialogs.UnzipDialog;
import com.dnielfe.manager.fileobserver.FileObserverCache;
import com.dnielfe.manager.fileobserver.MultiFileObserver;
import com.dnielfe.manager.fileobserver.MultiFileObserver.OnEventListener;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.settings.SettingsActivity;
import com.dnielfe.manager.tasks.PasteTaskExecutor;
import com.dnielfe.manager.utils.ActionBarNavigation;
import com.dnielfe.manager.utils.ActionBarNavigation.OnNavigateListener;
import com.dnielfe.manager.utils.Bookmarks;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.SimpleUtils;
import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public final class Browser extends ThemableActivity implements OnEventListener,
		OnNavigateListener {

	public static final String ACTION_WIDGET = "com.dnielfe.manager.Main.ACTION_WIDGET";
	public static final String EXTRA_SHORTCUT = "shortcut_path";

	private static Handler sHandler;
	private static ActionBarNavigation mNavigation;
	private ActionModeController mActionController;
	private MultiFileObserver mObserver;
	private FileObserverCache mObserverCache;
	private Runnable mLastRunnable;

	private static BookmarksAdapter mBookmarksAdapter;
	private static BrowserListAdapter mListAdapter;
	private static DrawerListAdapter mMenuAdapter;

	private boolean mReturnIntent = false;
	private boolean mUseBackKey = true;

	public static ArrayList<String> mDataSource;
	public static String mCurrentPath;

	private Cursor mBookmarksCursor;
	private String defaultdir;
	private LinearLayout mDrawer;
	private MenuItem mMenuItemPaste;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList, mBookmarkList;
	private AbsListView mListView;
	private ActionBar mActionBar;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);

		if (savedInstanceState == null) {
			savedInstanceState = getIntent().getBundleExtra(EXTRA_SAVED_STATE);
		}

		init();
		restoreSavedState(savedInstanceState);

		File dir = new File(defaultdir);

		if (dir.exists() && dir.isDirectory())
			navigateTo(dir.getAbsolutePath());
	}

	@Override
	public void onResume() {
		super.onResume();
		Settings.updatePreferences(this);

		invalidateOptionsMenu();
	}

	@Override
	public void onPause() {
		super.onPause();
		mObserver.stopWatching();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("location", mCurrentPath);
	}

	private void restoreSavedState(final Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			// get directory when you rotate your phone
			defaultdir = savedInstanceState.getString("location");
		} else {
			// If other apps want to choose a file you do it with this action
			if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				mReturnIntent = true;
			} else if (getIntent().getAction().equals(ACTION_WIDGET)) {
				navigateTo(getIntent().getExtras().getString("folder"));
			}

			try {
				String shortcut = getIntent().getStringExtra(EXTRA_SHORTCUT);
				File dir = new File(shortcut);

				if (dir.exists() && dir.isDirectory()) {
					defaultdir = shortcut;
				} else {
					if (dir.exists() && dir.isFile())
						listItemAction(dir, shortcut);
					// you need to call it when shortcut-dir not exists
					defaultdir = Settings.defaultdir;
				}
			} catch (Exception e) {
				defaultdir = Settings.defaultdir;
			}
		}
	}

	private void init() {
		mDataSource = new ArrayList<String>();

		// new ArrayAdapter
		mListAdapter = new BrowserListAdapter(this, mDataSource);

		mObserverCache = FileObserverCache.getInstance();
		mNavigation = new ActionBarNavigation(this);

		this.mActionController = new ActionModeController(this);

		if (sHandler == null) {
			sHandler = new Handler(this.getMainLooper());
		}

		initActionBar();
		setupDrawer();

		SearchIntent(getIntent());

		// get ListView
		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setAdapter(mListAdapter);
		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		mListView.setOnItemClickListener(mOnItemClickListener);

		mActionController.setListView(mListView);
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			final String item = (mListView.getAdapter().getItem(position))
					.toString();
			final File file = new File(mCurrentPath + "/" + item);

			if (file.isDirectory()) {
				navigateTo(file.getAbsolutePath());

				// go to the top of the ListView
				mListView.setSelection(0);

				if (!mUseBackKey)
					mUseBackKey = true;

			} else {
				listItemAction(file, item);
			}
		}
	};

	private void initActionBar() {
		this.mActionBar = this.getActionBar();
		this.mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO
				| ActionBar.DISPLAY_HOME_AS_UP);

		// set custom ActionBar layout
		final View mActionView = getLayoutInflater().inflate(
				R.layout.activity_browser_actionbar, null);
		this.mActionBar.setCustomView(mActionView);
		this.mActionBar.show();
	}

	private void setupDrawer() {
		final TypedArray array = obtainStyledAttributes(new int[] { R.attr.themeId });
		final int themeId = array.getInteger(0, SimpleExplorer.THEME_ID_LIGHT);
		array.recycle();

		mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

		// init drawer menu list
		mDrawerList = (ListView) findViewById(R.id.drawer_list);
		mMenuAdapter = new DrawerListAdapter(this);
		mDrawerList.setAdapter(mMenuAdapter);

		initBookmarks();

		// Set shadow of navigation drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		int icon = themeId == SimpleExplorer.THEME_ID_LIGHT ? R.drawable.holo_light_ic_drawer
				: R.drawable.holo_dark_ic_drawer;

		// Add Navigation Drawer to ActionBar
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, icon,
				R.string.drawer_open, R.string.drawer_close) {

			@Override
			public void onDrawerOpened(View view) {
				super.onDrawerOpened(view);
				mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_HOME_AS_UP
						| ActionBar.DISPLAY_USE_LOGO
						| ActionBar.DISPLAY_SHOW_TITLE);
				mActionBar.setTitle(R.string.app_name);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_HOME_AS_UP
						| ActionBar.DISPLAY_SHOW_CUSTOM
						| ActionBar.DISPLAY_USE_LOGO);
				invalidateOptionsMenu();
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mDrawerList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				switch (position) {
				case 0:
					// start AppManager
					Intent intent1 = new Intent(Browser.this, AppManager.class);
					startActivity(intent1);
					break;
				case 1:
					// start Preferences
					Intent intent2 = new Intent(Browser.this,
							SettingsActivity.class);
					startActivity(intent2);
					break;
				case 2:
					// exit
					finish();
				}
			}
		});
	}

	private void initBookmarks() {
		mBookmarksCursor = getBookmarksCursor();
		mBookmarksAdapter = new BookmarksAdapter(this, mBookmarksCursor);

		mBookmarkList = (ListView) findViewById(R.id.bookmark_list);
		mBookmarkList.setAdapter(mBookmarksAdapter);
		mBookmarkList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mDrawerLayout.isDrawerOpen(mDrawer))
					mDrawerLayout.closeDrawer(mDrawer);

				if (mBookmarksCursor.moveToPosition(position)) {
					String path = mBookmarksCursor.getString(mBookmarksCursor
							.getColumnIndex(Bookmarks.PATH));
					File file = new File(path);
					if (file != null) {
						if (file.isDirectory()) {
							mCurrentPath = path;
							// go to the top of the ListView
							mListView.setSelection(0);
						} else {
							listItemAction(file, path);
						}
					}
				} else {
					Toast.makeText(Browser.this, getString(R.string.error),
							Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	private void navigateTo(String path) {
		mCurrentPath = new String(path);

		if (mObserver != null) {
			mObserver.stopWatching();
			mObserver.removeOnEventListener(this);
		}

		try {
			updateDirectory(setDirectory(path));
		} catch (Exception e) {
			Toast.makeText(Browser.this, getString(R.string.cantreadfolder),
					Toast.LENGTH_SHORT).show();
		}

		mObserver = mObserverCache.getOrCreate(path);

		// add listener for FileObserver and start watching
		if (mObserver.listeners.isEmpty())
			mObserver.addOnEventListener(this);
		mObserver.startWatching();

		// add listener for navigation view in ActionBar
		if (mNavigation.listeners.isEmpty())
			mNavigation.addonNavigateListener(this);
		mNavigation.setDirectoryButtons(path);
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
		case FileObserver.MOVED_TO:
		case FileObserver.MOVED_FROM:
		case FileObserver.ATTRIB:
		case FileObserver.DELETE:
		case FileObserver.DELETE_SELF:
			sHandler.removeCallbacks(mLastRunnable);
			sHandler.post(mLastRunnable = new NavigateRunnable(mCurrentPath));
			break;
		}
	}

	@Override
	public void onNavigate(String path) {
		// navigate to path when ActionBarNavigation button is clicked
		navigateTo(path);
		// go to the top of the ListView
		mListView.setSelection(0);
	}

	private void SearchIntent(Intent intent) {
		setIntent(intent);

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query.length() > 0) {
				// open a new class
				Intent intent1 = new Intent(Browser.this, SearchActivity.class);
				intent1.putExtra("current", mCurrentPath);
				intent1.putExtra("query", query);
				startActivity(intent1);
			}
		}
	}

	private void listItemAction(File file, String item) {
		String item_ext = null;

		try {
			item_ext = item.substring(item.lastIndexOf("."), item.length());
		} catch (IndexOutOfBoundsException e) {
			item_ext = "";
		}

		if (item_ext.equalsIgnoreCase(".zip")) {
			if (mReturnIntent) {
				returnIntentResults(file);
			} else {
				final String zipPath = mCurrentPath + "/" + item;
				final DialogFragment dialog = UnzipDialog.instantiate(zipPath);
				dialog.show(getFragmentManager(), "dialog");
			}
		} else {
			SimpleUtils.openFile(this, file);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.main, menu);

		mMenuItemPaste = menu.findItem(R.id.paste);
		mMenuItemPaste.setVisible(!ClipBoard.isEmpty());
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// this is needed when a Bookmark is selected in NavingationDrawer
		if (!mDrawerLayout.isDrawerOpen(mDrawer)) {
			// update ActionBar navigation view
			navigateTo(mCurrentPath);
		}
		return super.onPrepareOptionsMenu(menu);
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
		case R.id.createfile:
			if (mDrawerLayout.isDrawerOpen(mDrawer))
				mDrawerLayout.closeDrawer(mDrawer);

			final DialogFragment dialog1 = new CreateFileDialog();
			dialog1.show(getFragmentManager(), "dialog");
			return true;
		case R.id.createfolder:
			if (mDrawerLayout.isDrawerOpen(mDrawer))
				mDrawerLayout.closeDrawer(mDrawer);

			final DialogFragment dialog2 = new CreateFolderDialog();
			dialog2.show(getFragmentManager(), "dialog");
			return true;
		case R.id.folderinfo:
			final DialogFragment pid = DirectoryInfoDialog
					.instantiate(new File(mCurrentPath));
			pid.show(getFragmentManager(), "dialog");
			return true;
		case R.id.search:
			this.onSearchRequested();
			return true;
		case R.id.paste:
			final PasteTaskExecutor ptc = new PasteTaskExecutor(this,
					mCurrentPath);
			ptc.start();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static BookmarksAdapter getBookmarksAdapter() {
		return mBookmarksAdapter;
	}

	private Cursor getBookmarksCursor() {
		return getContentResolver().query(
				Bookmarks.CONTENT_URI,
				new String[] { Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH,
						Bookmarks.CHECKED }, null, null, null);
	}

	private static final class NavigateRunnable implements Runnable {
		private final String target;

		NavigateRunnable(final String path) {
			this.target = path;
		}

		@Override
		public void run() {
			updateDirectory(setDirectory(target));
		}
	}

	private static void updateDirectory(ArrayList<String> content) {
		if (!mDataSource.isEmpty())
			mDataSource.clear();

		for (String data : content)
			mDataSource.add(data);

		mListAdapter.notifyDataSetChanged();
	}

	// need it to update from other classes
	public static void refreshDir(String dir) {
		updateDirectory(setDirectory(dir));
	}

	private static ArrayList<String> setDirectory(String path) {
		mCurrentPath = path;
		return SimpleUtils.listFiles(path);
	}

	public static ActionBarNavigation getNavigation() {
		return mNavigation;
	}

	// On back pressed Actions
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		File file = new File(mCurrentPath);
		String parent = file.getParent();

		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !mCurrentPath.equals("/")) {

			navigateTo(parent);

			// go to the top of the ListView
			mListView.setSelection(0);
			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& mCurrentPath.equals("/")) {
			Toast.makeText(Browser.this,
					getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			if (!ClipBoard.isEmpty()) {
				ClipBoard.unlock();
				ClipBoard.clear();
			}

			mUseBackKey = false;
			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& mCurrentPath.equals("/")) {
			if (mObserver != null) {
				mObserver.stopWatching();
				mObserver.removeOnEventListener(this);
			}

			mNavigation.removeOnNavigateListener(this);
			finish();
			return false;
		}
		return false;
	}
}
