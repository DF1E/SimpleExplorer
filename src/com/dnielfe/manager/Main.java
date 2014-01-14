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
import java.util.ArrayList;

import com.dnielfe.utils.Bookmarks;
import com.dnielfe.utils.Compress;
import com.dnielfe.utils.Decompress;
import com.dnielfe.utils.DrawerListAdapter;
import com.dnielfe.utils.LinuxShell;

import android.app.ActionBar;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;

public final class Main extends ListActivity {

	public static final String ACTION_WIDGET = "com.dnielfe.manager.Main.ACTION_WIDGET";
	public static final String EXTRA_SHORTCUT = "shortcut_path";

	public static final String PREF_HIDDEN = "displayhiddenfiles";
	public static final String PREF_PREVIEW = "showpreview";
	public static final String PREFS_SORT = "sort";
	public static final String PREFS_VIEW = "viewmode";
	public static final String PREF_DIR = "defaultdir";

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
	private static final int COPY_TYPE = 35;

	private static final int directorytextsize = 16;
	private static final String appdir = Environment
			.getExternalStorageDirectory().getPath() + "/Simple Explorer";

	private static EventHandler mHandler;
	private static EventHandler.TableRow mTable;

	private boolean mReturnIntent = false;
	private boolean mHoldingFile = false;
	private boolean mUseBackKey = true;
	private boolean delete_after_copy = false;

	public ActionMode mActionMode;

	private static String mCopiedTarget;
	private static String mSelectedListItem;
	private static String[] multidata;
	private static String[] drawerTitles;

	private SharedPreferences mSettings;
	private String defaultdir;
	private View mActionView;
	private LinearLayout mDirectoryButtons, mDrawer;
	private MenuItem mMenuItemPaste;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBar mActionBar;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		checkEnvironment();
		setupDrawer();

		mHandler = new EventHandler(this);

		loadPreferences();

		// new ArrayAdapter
		mTable = mHandler.new TableRow();

		// Set List Adapter
		mHandler.setListAdapter(mTable);
		setListAdapter(mTable);

		// register context menu for our ListView
		registerForContextMenu(getListView());

		SearchIntent(getIntent());

		if (savedInstanceState != null) {
			// get directory when you rotate your phone
			defaultdir = savedInstanceState.getString("location");
		} else {
			// If other apps want to choose a file you do it with this action
			if (getIntent().getAction().equals(Intent.ACTION_GET_CONTENT)) {
				mReturnIntent = true;

			} else if (getIntent().getAction().equals(ACTION_WIDGET)) {
				mHandler.updateDirectory(mHandler.getNextDir(getIntent()
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
			mHandler.updateDirectory(mHandler.setHomeDir(dir.getAbsolutePath()));

		listView(false);
	}

	@Override
	public void onResume() {
		super.onResume();

		loadPreferences();

		// refresh directory
		mHandler.updateDirectory(mHandler.setHomeDir(mHandler.getCurrentDir()));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("location", mHandler.getCurrentDir());
		super.onSaveInstanceState(outState);
	}

	private void getDefaultDir() {
		// get default directory from preferences
		defaultdir = mSettings.getString("defaultdir", Environment
				.getExternalStorageDirectory().getPath());
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
			info.putExtra("PATH_NAME", mHandler.getCurrentDir());
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

	// loading shared preferences
	private void loadPreferences() {
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean hidden = mSettings.getBoolean(PREF_HIDDEN, true);
		boolean thumb = mSettings.getBoolean(PREF_PREVIEW, true);
		String value = mSettings.getString("sort", "1");
		String viewmode = mSettings.getString("viewmode", "1");

		int sort = Integer.parseInt(value);
		int viewm = Integer.parseInt(viewmode);

		mHandler.setShowHiddenFiles(hidden);
		mHandler.setSortType(sort);
		mHandler.setViewMode(viewm);
		mHandler.setShowThumbnails(thumb);
	}

	// check if SDcard is present
	private void checkEnvironment() {
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);

		if (sdCardExist) {
			// nothing
		} else {
			Toast.makeText(Main.this, getString(R.string.sdcardnotfound),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		SearchIntent(intent);
	}

	private void SearchIntent(Intent intent) {
		setIntent(intent);

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query.length() > 0) {
				// start search in background
				new BackgroundWork(SEARCH_TYPE).execute(query);
			}
		}
	}

	// get ListView options
	private void listView(boolean toTop) {

		if (toTop) {
			// go to top of ListView
			getListView().setSelection(0);
		}

		// Update the Buttons showing the path
		setDirectoryButtons();
	}

	// set directory buttons showing path
	private void setDirectoryButtons() {
		File currentDirectory = new File(mHandler.getCurrentDir());

		HorizontalScrollView scrolltext = (HorizontalScrollView) findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		mDirectoryButtons.removeAllViews();

		String[] parts = currentDirectory.getAbsolutePath().split("/");

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

				mHandler.updateDirectory(mHandler.setHomeDir("/"));
				listView(true);
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

					mHandler.updateDirectory(mHandler.setHomeDir(dir1));

					listView(true);
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
		final File file = new File(mHandler.getCurrentDir() + "/" + item);

		// If the user has multi-select on, we just need to record the file not
		// make an intent for it.
		if (multiSelect) {
			mTable.addMultiPosition(position, file.getPath(), true);

		} else {
			if (file.isDirectory()) {
				if (file.canRead()) {
					mHandler.updateDirectory(mHandler.getNextDir(item, false));

					listView(true);

					if (!mUseBackKey)
						mUseBackKey = true;

				} else {
					if (LinuxShell.isRoot()) {
						mHandler.updateDirectory(mHandler.getNextDir(item,
								false));

						listView(true);

						if (!mUseBackKey)
							mUseBackKey = true;

					} else {
						Toast.makeText(this,
								getString(R.string.cantreadfolder),
								Toast.LENGTH_SHORT).show();
					}
				}
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
				final String zipPath = mHandler.getCurrentDir() + "/" + item;
				final String unZipPath = appdir + "/" + item;

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				AlertDialog alert;
				CharSequence[] option = { getString(R.string.extract),
						getString(R.string.extractto) };

				builder.setTitle(file.getName());
				builder.setItems(option, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							new BackgroundWork(UNZIP_TYPE).execute(zipPath,
									unZipPath);
							break;
						case 1:
							extractzipto(zipPath, mHandler.getCurrentDir());
							break;
						}
					}
				});

				alert = builder.create();
				alert.show();
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
		return true;
	}

	// Hide ActionBar Items, if Drawer is open
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawer);

		menu.findItem(R.id.paste).setVisible(mHoldingFile);
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
		case R.id.create:
			createDialog();
			return true;
		case R.id.bookmarks:
			bookmarkDialog();
			return true;
		case R.id.multis:
			mHandler.multiselect();
			startActionMode(mActionModeCallback);
			return true;

		case R.id.search:
			this.onSearchRequested();
			return true;

		case R.id.paste:
			boolean multi_select = mHandler.hasMultiSelectData();

			if (multi_select) {
				copyFileMultiSelect(mHandler.getCurrentDir());

			} else if (mHoldingFile && mCopiedTarget.length() > 1) {

				String[] data = { mCopiedTarget, mHandler.getCurrentDir() };

				new BackgroundWork(COPY_TYPE).execute(data);
			}

			mHoldingFile = false;
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

	private void openaction(final File file) {

		final CharSequence[] openAsOperations = { getString(R.string.text),
				getString(R.string.image), getString(R.string.video),
				getString(R.string.music), getString(R.string.file) };

		AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
		builder.setItems(openAsOperations,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dInterface, int item) {

						switch (item) {
						case 0:
							Intent txtIntent = new Intent(Intent.ACTION_VIEW);
							txtIntent.setDataAndType(Uri.fromFile(file),
									"text/plain");
							startActivity(txtIntent);
							break;
						case 1:
							Intent imageIntent = new Intent(Intent.ACTION_VIEW);
							imageIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(imageIntent);
							break;

						case 2:
							Intent movieIntent = new Intent(Intent.ACTION_VIEW);
							movieIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(movieIntent);
							break;
						case 3:

							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setDataAndType(Uri.fromFile(file), "audio/*");
							startActivity(i);
							break;

						case 4:
							Intent i1 = new Intent(Intent.ACTION_VIEW);
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
						String name = editt.getText().toString();

						File file = new File(mHandler.getCurrentDir()
								+ File.separator + name);

						if (file.exists()) {
							Toast.makeText(Main.this,
									getString(R.string.fileexists),
									Toast.LENGTH_SHORT).show();
						} else {
							try {
								if (name.length() >= 1) {
									file.createNewFile();

									Toast.makeText(Main.this,
											R.string.filecreated,
											Toast.LENGTH_SHORT).show();
								} else {
									Toast.makeText(Main.this, R.string.error,
											Toast.LENGTH_SHORT).show();
								}
							} catch (Exception e) {
								if (LinuxShell.isRoot()) {
									FileUtils.createRootFile(
											mHandler.getCurrentDir(), name);
								} else {
									Toast.makeText(Main.this, R.string.error,
											Toast.LENGTH_SHORT).show();
								}
							}

							mHandler.updateDirectory(mHandler.getNextDir(
									mHandler.getCurrentDir(), true));
						}
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

	// Will be displayed when long clicking ListView
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);

		boolean multi_data = mHandler.hasMultiSelectData();
		AdapterContextMenuInfo _info = (AdapterContextMenuInfo) info;
		mSelectedListItem = mHandler.getData(_info.position);
		File item = new File(mHandler.getCurrentDir() + "/" + mSelectedListItem);

		// is it a directory and is multi-select turned off
		if (item.isDirectory() && !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, D_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, D_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, D_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, D_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, D_MENU_ZIP, 0, getString(R.string.zipfolder));
			menu.add(0, D_MENU_PASTE, 0, getString(R.string.pasteintofolder))
					.setEnabled(mHoldingFile || multi_data);
			menu.add(0, D_MENU_SHORTCUT, 0, getString(R.string.shortcut));
			menu.add(0, D_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, D_MENU_DETAILS, 0, getString(R.string.details));

			// is it a file and is multi-select turned off
		} else if (!item.isDirectory() && !mHandler.isMultiSelected()) {
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
		final File file = new File(mHandler.getCurrentDir() + "/"
				+ mSelectedListItem);

		switch (item.getItemId()) {

		case F_MENU_OPENAS:
			openaction(file);
			return true;

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
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(mSelectedListItem);
			builder.setIcon(R.drawable.warning);
			builder.setMessage(getString(R.string.cannotbeundoneareyousureyouwanttodelete));

			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.setPositiveButton(getString(R.string.delete),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							new BackgroundWork(DELETE_TYPE).execute(file
									.getPath());
						}
					});
			AlertDialog alert_d = builder.create();
			alert_d.show();
			return true;

		case D_MENU_DETAILS:
		case F_MENU_DETAILS:
			// This will open a new Class wich shows Details of file/folder
			Intent info = new Intent(Main.this, InfoDialog.class);
			info.putExtra("FILE_NAME", file.getPath());
			Main.this.startActivity(info);
			return true;

		case D_MENU_RENAME:
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
							String newname = inputf.getText().toString();

							if (inputf.getText().length() < 1)
								dialog.dismiss();

							if (FileUtils.renameTarget(file.getPath(), newname) == 0) {
								Toast.makeText(Main.this,
										getString(R.string.filewasrenamed),
										Toast.LENGTH_LONG).show();
							} else {
								if (LinuxShell.isRoot()) {
									FileUtils.renameRootTarget(
											mHandler.getCurrentDir(),
											mSelectedListItem, newname);
								} else {
									Toast.makeText(getBaseContext(),
											getString(R.string.error),
											Toast.LENGTH_SHORT).show();
								}
							}
							dialog.dismiss();
							String temp = mHandler.getCurrentDir();
							mHandler.updateDirectory(mHandler.getNextDir(temp,
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
			String item_ext = null;

			try {
				item_ext = mSelectedListItem.substring(
						mSelectedListItem.lastIndexOf("."),
						mSelectedListItem.length());
			} catch (Exception e) {
				item_ext = ".empty";
			}

			try {
				Intent i = new Intent(Intent.ACTION_SEND);

				if (item_ext.equalsIgnoreCase(".mp3")
						|| item_ext.equalsIgnoreCase(".m4a")
						|| item_ext.equalsIgnoreCase(".aiff")
						|| item_ext.equalsIgnoreCase(".wma")
						|| item_ext.equalsIgnoreCase(".caf")) {
					i.setType("audio/*");
				} else if (item_ext.equalsIgnoreCase(".3gp")
						|| item_ext.equalsIgnoreCase(".wmv")
						|| item_ext.equalsIgnoreCase(".mp4")
						|| item_ext.equalsIgnoreCase(".mpeg")
						|| item_ext.equalsIgnoreCase(".mpg")
						|| item_ext.equalsIgnoreCase(".mov")
						|| item_ext.equalsIgnoreCase(".avi")
						|| item_ext.equalsIgnoreCase(".ogg")) {
					i.setType("video/*");
				} else if (item_ext.equalsIgnoreCase(".jpg")
						|| item_ext.equalsIgnoreCase(".jpeg")
						|| item_ext.equalsIgnoreCase(".png")
						|| item_ext.equalsIgnoreCase(".gif")
						|| item_ext.equalsIgnoreCase(".raw")
						|| item_ext.equalsIgnoreCase(".psd")
						|| item_ext.equalsIgnoreCase(".bmp")) {
					i.setType("image/*");
				} else if (item_ext.equalsIgnoreCase(".apk")) {
					i.setType("application/vnd.android.package-archive");
				} else if (item_ext.equalsIgnoreCase(".empty")) {
					i.setType("*/*");
				}

				i.putExtra(Intent.EXTRA_SUBJECT, mSelectedListItem);
				i.putExtra(Intent.EXTRA_BCC, "");
				i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
				startActivity(Intent
						.createChooser(i, getString(R.string.share)));
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.error),
						Toast.LENGTH_SHORT).show();
			}
			return true;

		case F_MENU_MOVE:
		case D_MENU_MOVE:
			delete_after_copy = true;

			mHoldingFile = true;
			mMenuItemPaste.setVisible(true);

			mCopiedTarget = file.getPath();
			return true;

		case F_MENU_COPY:
		case D_MENU_COPY:
			delete_after_copy = false;

			mHoldingFile = true;
			mMenuItemPaste.setVisible(true);

			mCopiedTarget = file.getPath();
			return true;

		case D_MENU_PASTE:
			boolean multi_select = mHandler.hasMultiSelectData();

			if (multi_select) {
				copyFileMultiSelect(file.getPath());

			} else if (mHoldingFile && mCopiedTarget.length() > 1) {

				String[] data = { mCopiedTarget, file.getPath() };

				new BackgroundWork(COPY_TYPE).execute(data);
			}

			mHoldingFile = false;
			mMenuItemPaste.setVisible(false);
			return true;

		case D_MENU_ZIP:
			new BackgroundWork(ZIP_TYPE).execute(file.getPath());
			return true;

		case D_MENU_SHORTCUT:
			FileUtils.createShortcut(this, file.getPath(), mSelectedListItem);
			return true;
		}
		return false;
	}

	// Show a Dialog with your bookmarks
	private void bookmarkDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		final Cursor bookmarksCursor = getBookmarks();

		builder.setTitle(R.string.bookmark);
		builder.setCursor(bookmarksCursor,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (bookmarksCursor.moveToPosition(item)) {
							String path = bookmarksCursor
									.getString(bookmarksCursor
											.getColumnIndex(Bookmarks.PATH));
							File file = new File(path);
							if (file != null) {
								if (file.isDirectory()) {
									mHandler.updateDirectory(mHandler
											.setHomeDir(path));
									listView(true);
								} else {
									listItemAction(file, path);
								}
							}
						} else {
							Toast.makeText(Main.this,
									getString(R.string.error),
									Toast.LENGTH_SHORT).show();
						}

						mDrawerLayout.closeDrawer(mDrawer);
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
					createfile();
					mDrawerLayout.closeDrawer(mDrawer);
					return;

				case 1:
					final EditText input = new EditText(Main.this);

					AlertDialog.Builder alert = new AlertDialog.Builder(
							Main.this);
					alert.setTitle(getString(R.string.createnewfolder));
					alert.setMessage(R.string.createmsg);
					alert.setView(input);
					alert.setPositiveButton(getString(R.string.ok),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									if (input.getText().length() >= 1) {
										if (FileUtils.createDir(
												mHandler.getCurrentDir() + "/",
												input.getText().toString()) == 0)
											Toast.makeText(
													Main.this,
													input.getText().toString()
															+ getString(R.string.created),
													Toast.LENGTH_LONG).show();
										else
											Toast.makeText(
													Main.this,
													getString(R.string.newfolderwasnotcreated),
													Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(
												Main.this,
												getString(R.string.newfolderwasnotcreated),
												Toast.LENGTH_SHORT).show();
									}

									dialog.dismiss();
									String temp = mHandler.getCurrentDir();
									mHandler.updateDirectory(mHandler
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
	}

	// this will start when multiButton is clicked
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
					mMenuItemPaste.setVisible(true);
					mode.finish();
				} catch (Exception e) {
					mode.finish();
				}
				return true;

			case R.id.actionmove:
				try {
					multimove();
					mMenuItemPaste.setVisible(true);
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
				for (int i = 0; i < mHandler.mDataSource.size(); i++) {
					File file = new File(mHandler.getCurrentDir() + "/"
							+ mHandler.mDataSource.get(i));

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

		Intent mail_int = new Intent();
		mail_int.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
		mail_int.setType("*/*");
		mail_int.putExtra(Intent.EXTRA_BCC, "");
		mail_int.putExtra(Intent.EXTRA_SUBJECT, " ");
		mail_int.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		startActivity(Intent.createChooser(mail_int, getString(R.string.share)));
	}

	// delete multiple files
	private void multidelete() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		int size = mHandler.mMultiSelectData.size();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.delete) + " ("
				+ String.valueOf(size) + ")");
		builder.setMessage(R.string.cannotbeundoneareyousureyouwanttodelete);
		builder.setPositiveButton((R.string.delete),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new BackgroundWork(DELETE_TYPE).execute(data);
					}
				});
		builder.setNegativeButton((R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	// zip multiple files
	private void multizipfiles() {
		final String[] data = new String[mHandler.mMultiSelectData.size()];
		int at = 0;

		for (String string : mHandler.mMultiSelectData)
			data[at++] = string;

		multidata = data;

		final String dir = mHandler.getCurrentDir();

		new BackgroundWork(MULTIZIP_TYPE).execute(dir);
	}

	private void multicopy() {
		if (mHandler.mMultiSelectData == null
				|| mHandler.mMultiSelectData.isEmpty()) {
			mTable.killMultiSelect(true, true);
		}
		delete_after_copy = false;
		mTable.killMultiSelect(false, true);
		mHandler.updateDirectory(mHandler.getNextDir(mHandler.getCurrentDir(),
				true));
	}

	// move multiple files
	private void multimove() {
		if (mHandler.mMultiSelectData == null
				|| mHandler.mMultiSelectData.isEmpty()) {
			mTable.killMultiSelect(true, true);
		}
		delete_after_copy = true;
		mTable.killMultiSelect(false, true);
		mHandler.updateDirectory(mHandler.getNextDir(mHandler.getCurrentDir(),
				true));
	}

	// copy multiple files
	private void copyFileMultiSelect(String newLocation) {
		String[] data;
		int index = 1;

		if (mHandler.mMultiSelectData.size() > 0) {
			data = new String[mHandler.mMultiSelectData.size() + 1];
			data[0] = newLocation;

			for (String s : mHandler.mMultiSelectData)
				data[index++] = s;

			new BackgroundWork(COPY_TYPE).execute(data);
		}
	}

	private void extractzipto(final String zipPath, final String unZipPath) {

		AlertDialog.Builder alertf = new AlertDialog.Builder(this);
		// Set an EditText view to get user input
		final EditText inputf = new EditText(this);
		inputf.setText(unZipPath);

		alertf.setTitle(getString(R.string.unzipping));
		alertf.setView(inputf);

		alertf.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String newpath = inputf.getText().toString();

						try {
							new BackgroundWork(UNZIP_TYPE).execute(zipPath,
									newpath);
						} catch (Exception e) {
							Toast.makeText(Main.this,
									getString(R.string.error),
									Toast.LENGTH_SHORT).show();
						}
					}
				});

		alertf.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancel
					}
				});

		alertf.show();
	}

	// Backgroundwork for:
	// search
	// delete
	// multizip
	// unzip file
	// ZIP Folder
	private class BackgroundWork extends
			AsyncTask<String, Void, ArrayList<String>> {
		public ProgressDialog pr_dialog = null;
		private String file_name;
		private int type;
		private int copy_rtn;

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
				pr_dialog.setCanceledOnTouchOutside(true);
				break;
			case DELETE_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.deleting));
				pr_dialog.setCancelable(true);
				break;
			case MULTIZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.packing));
				pr_dialog.setCancelable(true);
				break;
			case UNZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.unzipping));
				pr_dialog.setCancelable(true);
				break;
			case ZIP_TYPE:
				pr_dialog = ProgressDialog.show(Main.this, "",
						getString(R.string.zipping));
				pr_dialog.setCancelable(true);
				break;
			case COPY_TYPE:
				if (delete_after_copy) {
					pr_dialog = ProgressDialog.show(Main.this, "",
							getString(R.string.moving));
				} else {
					pr_dialog = ProgressDialog.show(Main.this, "",
							getString(R.string.copying));
				}
				pr_dialog.setCancelable(true);
				break;
			}
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {
			String dir1 = mHandler.getCurrentDir();

			switch (type) {
			case SEARCH_TYPE:
				file_name = params[0];

				ArrayList<String> found = FileUtils.searchInDirectory(
						mHandler.getCurrentDir(), file_name);
				return found;
			case DELETE_TYPE:
				int size = params.length;

				for (int i = 0; i < size; i++)
					FileUtils.deleteTarget(params[i], dir1);

				return null;
			case MULTIZIP_TYPE:
				final String zipfile = params[0] + "/" + "zipfile.zip";

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
				FileUtils.createZipFile(params[0]);
				return null;
			case COPY_TYPE:
				int len = params.length;

				if (mHandler.mMultiSelectData != null
						&& !mHandler.mMultiSelectData.isEmpty()) {
					for (int i = 1; i < len; i++) {
						copy_rtn = FileUtils.copyToDirectory(params[i],
								params[0]);

						if (delete_after_copy)
							if (copy_rtn != -2)
								FileUtils.deleteTarget(params[i], dir1);
					}
				} else {
					copy_rtn = FileUtils.copyToDirectory(params[0], params[1]);

					if (delete_after_copy)
						if (copy_rtn != -2)
							FileUtils.deleteTarget(params[0], dir1);
				}
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
									mHandler.updateDirectory(mHandler
											.setHomeDir(path.substring(0,
													path.lastIndexOf("/"))));
									listView(true);
								}
							});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				pr_dialog.dismiss();
				break;
			case DELETE_TYPE:
				pr_dialog.dismiss();
				break;
			case MULTIZIP_TYPE:
				pr_dialog.dismiss();
				break;
			case UNZIP_TYPE:
				pr_dialog.dismiss();
				break;
			case ZIP_TYPE:
				pr_dialog.dismiss();
				break;
			case COPY_TYPE:
				if (copy_rtn == -2 & delete_after_copy)
					Toast.makeText(Main.this, R.string.movefail,
							Toast.LENGTH_SHORT).show();

				else if (delete_after_copy)
					Toast.makeText(Main.this, R.string.movesuccsess,
							Toast.LENGTH_SHORT).show();

				else if (copy_rtn == 0)
					Toast.makeText(Main.this, R.string.copysuccsess,
							Toast.LENGTH_SHORT).show();

				else if (copy_rtn == -2)
					Toast.makeText(Main.this, R.string.fileexists,
							Toast.LENGTH_SHORT).show();

				else
					Toast.makeText(Main.this, R.string.copyfail,
							Toast.LENGTH_SHORT).show();

				delete_after_copy = false;
				pr_dialog.dismiss();
				break;
			}

			if (mHandler.mMultiSelectData != null
					&& !mHandler.mMultiSelectData.isEmpty()) {
				mTable.killMultiSelect(true, true);
				mHandler.multi_select_flag = false;
			}

			mHandler.updateDirectory(mHandler.getNextDir(
					mHandler.getCurrentDir(), true));

			listView(false);
		}
	}

	// On back pressed Actions
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		String current = mHandler.getCurrentDir();

		if (mDrawerLayout.isDrawerOpen(mDrawer)) {
			mDrawerLayout.closeDrawer(mDrawer);

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !current.equals("/")) {

			mHandler.updateDirectory(mHandler.getPreviousDir(current));

			listView(true);
			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& current.equals("/")) {
			Toast.makeText(Main.this, getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			// Stop holding file for move/copy
			mHoldingFile = false;
			mMenuItemPaste.setVisible(false);

			mUseBackKey = false;
			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& current.equals("/")) {
			finish();
			return false;
		}
		return false;
	}
}
