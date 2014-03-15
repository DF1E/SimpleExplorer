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
import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.controller.ActionModeController;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DirectoryInfoDialog;
import com.dnielfe.manager.dialogs.UnzipDialog;
import com.dnielfe.manager.fileobserver.FileObserverCache;
import com.dnielfe.manager.fileobserver.MultiFileObserver;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.settings.SettingsActivity;
import com.dnielfe.manager.tasks.PasteTask;
import com.dnielfe.manager.utils.Bookmarks;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.SimpleUtils;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;

public final class Browser extends ListActivity implements
		MultiFileObserver.OnEventListener {

	public static final String ACTION_WIDGET = "com.dnielfe.manager.Main.ACTION_WIDGET";
	public static final String EXTRA_SHORTCUT = "shortcut_path";

	public static final String PREF_DIR = "defaultdir";

	private static final int directorytextsize = 16;

	private ActionModeController mActionController;
	private static Handler sHandler;
	private MultiFileObserver mObserver;
	private FileObserverCache mObserverCache;
	private Runnable mLastRunnable;

	private static BrowserListAdapter mTable;

	private boolean mReturnIntent = false;
	private boolean mUseBackKey = true;

	private static String[] drawerTitles;
	public static ArrayList<String> mDataSource;

	private String defaultdir;
	public static String mCurrentPath;
	private View mActionView;
	private LinearLayout mDirectoryButtons, mDrawer;
	private MenuItem mMenuItemPaste;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private AbsListView mListView;
	private ActionBar mActionBar;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		init();

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

		File dir = new File(defaultdir);

		if (dir.exists() && dir.isDirectory())
			navigateTo(dir.getAbsolutePath());
	}

	@Override
	public void onResume() {
		super.onResume();

		Settings.updatePreferences(this);

		// refresh directory
		updateDirectory(setDirectory(mCurrentPath));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("location", mCurrentPath);
		super.onSaveInstanceState(outState);
	}

	private void init() {
		Settings.updatePreferences(this);

		mDataSource = new ArrayList<String>();

		// new ArrayAdapter
		mTable = new BrowserListAdapter(this, mDataSource);

		mObserverCache = FileObserverCache.getInstance();

		this.mActionController = new ActionModeController(this);

		if (sHandler == null) {
			sHandler = new Handler(this.getMainLooper());
		}

		checkEnvironment();
		setupDrawer();

		SearchIntent(getIntent());

		// get ListView
		mListView = getListView();

		mActionController.setListView(mListView);

		// Set ListAdapters
		mListView.setAdapter(mTable);

		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
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
			// AppManager
			Intent intent1 = new Intent(Browser.this, AppManager.class);
			startActivity(intent1);
			break;
		case 1:
			// Preferences
			Intent intent2 = new Intent(Browser.this, SettingsActivity.class);
			startActivity(intent2);
			break;
		case 2:
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
			Toast.makeText(Browser.this, getString(R.string.sdcardnotfound),
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
			sHandler.post(mLastRunnable = new NavigateRunnable(mCurrentPath));
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
		File currentDirectory = new File(mCurrentPath);

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
		final String item = (mListView.getAdapter().getItem(position))
				.toString();
		final File file = new File(mCurrentPath + "/" + item);

		if (file.isDirectory()) {
			navigateTo(file.getAbsolutePath());

			if (!mUseBackKey)
				mUseBackKey = true;

		} else {
			listItemAction(file, item);
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

	// Options menu start here
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		mMenuItemPaste = menu.findItem(R.id.paste);
		mMenuItemPaste.setVisible(!ClipBoard.isEmpty());
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
		case R.id.folderinfo:
			final DialogFragment pid = DirectoryInfoDialog
					.instantiate(new File(mCurrentPath));
			pid.show(getFragmentManager(), "dialog");
			return true;
		case R.id.search:
			// TODO optimize
			Intent intent1 = new Intent(Browser.this, SearchActivity.class);
			intent1.putExtra("current", mCurrentPath);
			startActivity(intent1);
			// this.onSearchRequested();
			return true;
		case R.id.paste:
			final PasteTask ptc = new PasteTask(this, mCurrentPath);
			ptc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
							Toast.makeText(Browser.this,
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

		AlertDialog.Builder builder3 = new AlertDialog.Builder(Browser.this);
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
			pr_dialog = ProgressDialog.show(Browser.this, "",
					getString(R.string.search));
			pr_dialog.setCanceledOnTouchOutside(true);
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			file_name = params[0];

			ArrayList<String> found = SimpleUtils.searchInDirectory(
					mCurrentPath, file_name);
			return found;
		}

		// This is called when the background thread is finished
		@Override
		protected void onPostExecute(final ArrayList<String> file) {
			final CharSequence[] names;
			int len = file != null ? file.size() : 0;

			pr_dialog.dismiss();

			if (len == 0) {
				Toast.makeText(Browser.this, R.string.itcouldntbefound,
						Toast.LENGTH_SHORT).show();
			} else {
				names = new CharSequence[len];

				for (int i = 0; i < len; i++) {
					String entry = file.get(i);
					names[i] = entry.substring(entry.lastIndexOf("/") + 1,
							entry.length());
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(
						Browser.this);
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
			updateDirectory(setDirectory(target));
		}
	}

	public static void updateDirectory(ArrayList<String> content) {
		if (!mDataSource.isEmpty())
			mDataSource.clear();

		for (String data : content)
			mDataSource.add(data);

		mTable.notifyDataSetChanged();
	}

	// need it to update from other classes
	public static void refreshDir(String dir) {
		updateDirectory(setDirectory(dir));
	}

	public static ArrayList<String> setDirectory(String path) {
		mCurrentPath = path;
		return SimpleUtils.listFiles(path);
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
			finish();
			return false;
		}
		return false;
	}
}
