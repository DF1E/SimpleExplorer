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

import org.apache.commons.io.FilenameUtils;

import com.dnielfe.manager.adapters.BookmarksAdapter;
import com.dnielfe.manager.adapters.DrawerListAdapter;
import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.adapters.MergeAdapter;
import com.dnielfe.manager.controller.ActionModeController;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DirectoryInfoDialog;
import com.dnielfe.manager.dialogs.UnpackDialog;
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
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
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
import android.widget.ListView;
import android.widget.Toast;

public final class Browser extends ThemableActivity implements OnEventListener,
		OnNavigateListener {

	public static final String EXTRA_SHORTCUT = "shortcut_path";
	public static final String TAG_DIALOG = "dialog";

	private static Handler sHandler;
	private static ActionBarNavigation mNavigation;
	private ActionModeController mActionController;
	private MultiFileObserver mObserver;
	private FileObserverCache mObserverCache;
	private Runnable mLastRunnable;

	private static BookmarksAdapter mBookmarksAdapter;
	private static MergeAdapter mMergeAdapter;
	private static BrowserListAdapter mListAdapter;
	private static DrawerListAdapter mMenuAdapter;

	private boolean mUseBackKey = true;

	public static ArrayList<String> mDataSource;
	public static String mCurrentPath;

	private FragmentManager fm;
	private Cursor mBookmarksCursor;
	private ListView mDrawer;
	private MenuItem mMenuItemPaste;
	private AbsListView mListView;

	private ActionBar mActionBar;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);

		Intent intent = getIntent();

		if (savedInstanceState == null) {
			savedInstanceState = intent.getBundleExtra(EXTRA_SAVED_STATE);
		}

		init();
		initDirectory(savedInstanceState, intent);
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

		final Fragment f = fm.findFragmentByTag(TAG_DIALOG);

		if (f != null) {
			fm.beginTransaction().remove(f).commit();
			fm.executePendingTransactions();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mObserver != null) {
			mObserver.stopWatching();
			mObserver.removeOnEventListener(this);
		}

		if (mNavigation != null)
			mNavigation.removeOnNavigateListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("location", mCurrentPath);
	}

	private void initDirectory(Bundle savedInstanceState, Intent intent) {
		String defaultdir;

		if (savedInstanceState != null) {
			// get directory when you rotate your phone
			defaultdir = savedInstanceState.getString("location");
		} else {
			try {
				File dir = new File(intent.getStringExtra(EXTRA_SHORTCUT));

				if (dir.exists() && dir.isDirectory()) {
					defaultdir = dir.getAbsolutePath();
				} else {
					if (dir.exists() && dir.isFile())
						listItemAction(dir);
					// you need to call it when shortcut-dir not exists
					defaultdir = Settings.defaultdir;
				}
			} catch (Exception e) {
				defaultdir = Settings.defaultdir;
			}
		}

		File dir = new File(defaultdir);

		if (dir.exists() && dir.isDirectory())
			navigateTo(dir.getAbsolutePath());
	}

	private void init() {
		fm = getFragmentManager();
		mDataSource = new ArrayList<String>();
		mObserverCache = FileObserverCache.getInstance();
		mNavigation = new ActionBarNavigation(this);
		mActionController = new ActionModeController(this);

		// new ArrayAdapter
		mListAdapter = new BrowserListAdapter(this, mDataSource);

		if (sHandler == null) {
			sHandler = new Handler(this.getMainLooper());
		}

		initActionBar();
		setupDrawer();
		initDrawerList();

		// get the browser list
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
			final File file = new File(
					(mListView.getAdapter().getItem(position)).toString());

			if (file.isDirectory()) {
				navigateTo(file.getAbsolutePath());

				// go to the top of the ListView
				mListView.setSelection(0);

				if (!mUseBackKey)
					mUseBackKey = true;

			} else {
				listItemAction(file);
			}
		}
	};

	private void initActionBar() {
		mActionBar = this.getActionBar();
		mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO
				| ActionBar.DISPLAY_HOME_AS_UP);

		// set custom ActionBar layout
		final View mActionView = getLayoutInflater().inflate(
				R.layout.activity_browser_actionbar, null);
		mActionBar.setCustomView(mActionView);
		mActionBar.show();
	}

	private void setupDrawer() {
		final TypedArray array = obtainStyledAttributes(new int[] { R.attr.themeId });
		final int themeId = array.getInteger(0, SimpleExplorer.THEME_ID_LIGHT);
		array.recycle();

		mDrawer = (ListView) findViewById(R.id.left_drawer);

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
						| ActionBar.DISPLAY_SHOW_TITLE);
				mActionBar.setTitle(R.string.app_name);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_HOME_AS_UP
						| ActionBar.DISPLAY_SHOW_CUSTOM);
				invalidateOptionsMenu();
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}

	private void initDrawerList() {
		mBookmarksCursor = getBookmarksCursor();
		mBookmarksAdapter = new BookmarksAdapter(this, mBookmarksCursor);
		mMenuAdapter = new DrawerListAdapter(this);

		// create MergeAdapter to combine multiple adapter
		mMergeAdapter = new MergeAdapter();
		mMergeAdapter.addAdapter(mBookmarksAdapter);
		mMergeAdapter.addAdapter(mMenuAdapter);

		mDrawer.setAdapter(mMergeAdapter);
		mDrawer.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mMergeAdapter.getAdapter(position)
						.equals(mBookmarksAdapter)) {
					// handle bookmark items
					if (mDrawerLayout.isDrawerOpen(mDrawer))
						mDrawerLayout.closeDrawer(mDrawer);

					if (mBookmarksCursor.moveToPosition((int) position)) {
						File file = new File(mBookmarksCursor
								.getString(mBookmarksCursor
										.getColumnIndex(Bookmarks.PATH)));

						if (file != null) {
							if (file.isDirectory()) {
								mCurrentPath = file.getAbsolutePath();
								// go to the top of the ListView
								mListView.setSelection(0);
							} else {
								listItemAction(file);
							}
						}
					}
				} else if (mMergeAdapter.getAdapter(position).equals(
						mMenuAdapter)) {
					// handle menu items
					switch ((int) mMergeAdapter.getItemId(position)) {
					case 0:
						Intent intent1 = new Intent(Browser.this,
								AppManager.class);
						startActivity(intent1);
						break;
					case 1:
						Intent intent2 = new Intent(Browser.this,
								SettingsActivity.class);
						startActivity(intent2);
						break;
					case 2:
						finish();
					}
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

		listDirectory(path);

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

	@Override
	public void onEvent(int event, String path) {
		// this will automatically update the directory when an action like this
		// will be performed
		switch (event & FileObserver.ALL_EVENTS) {
		case FileObserver.CREATE:
		case FileObserver.CLOSE_WRITE:
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

	private void listItemAction(File file) {
		String item_ext = FilenameUtils.getExtension(file.getName());

		if (item_ext.equalsIgnoreCase("zip")
				|| item_ext.equalsIgnoreCase("rar")) {
			final DialogFragment dialog = UnpackDialog.instantiate(file);
			dialog.show(fm, TAG_DIALOG);
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
			final DialogFragment dialog1 = new CreateFileDialog();
			dialog1.show(fm, TAG_DIALOG);
			return true;
		case R.id.createfolder:
			final DialogFragment dialog2 = new CreateFolderDialog();
			dialog2.show(fm, TAG_DIALOG);
			return true;
		case R.id.folderinfo:
			final DialogFragment dirInfo = new DirectoryInfoDialog();
			dirInfo.show(fm, TAG_DIALOG);
			return true;
		case R.id.search:
			Intent sintent = new Intent(Browser.this, SearchActivity.class);
			startActivity(sintent);
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
			listDirectory(target);
		}
	}

	public static void listDirectory(String path) {
		ArrayList<String> ab = SimpleUtils.listFiles(path);
		mCurrentPath = path;

		if (!mDataSource.isEmpty())
			mDataSource.clear();

		for (String data : ab)
			mDataSource.add(data);

		mListAdapter.notifyDataSetChanged();
	}

	public static ActionBarNavigation getNavigation() {
		return mNavigation;
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {

		if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !mCurrentPath.equals("/")) {
			File file = new File(mCurrentPath);
			navigateTo(file.getParent());

			// get position of the previous folder in ListView
			mListView.setSelection(mListAdapter.getPosition(file.getPath()));
			return true;
		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& mCurrentPath.equals("/")) {
			Toast.makeText(Browser.this,
					getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			mUseBackKey = false;
			return false;
		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& mCurrentPath.equals("/")) {
			finish();
			return false;
		}

		return super.onKeyDown(keycode, event);
	}
}
