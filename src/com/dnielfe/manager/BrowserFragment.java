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

import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.controller.ActionModeController;
import com.dnielfe.manager.dialogs.CreateFileDialog;
import com.dnielfe.manager.dialogs.CreateFolderDialog;
import com.dnielfe.manager.dialogs.DirectoryInfoDialog;
import com.dnielfe.manager.dialogs.UnpackDialog;
import com.dnielfe.manager.fileobserver.FileObserverCache;
import com.dnielfe.manager.fileobserver.MultiFileObserver;
import com.dnielfe.manager.fileobserver.MultiFileObserver.OnEventListener;
import com.dnielfe.manager.preview.IconPreview;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.tasks.PasteTaskExecutor;
import com.dnielfe.manager.utils.Bookmarks;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.NavigationView;
import com.dnielfe.manager.utils.NavigationView.OnNavigateListener;
import com.dnielfe.manager.utils.SimpleUtils;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public final class BrowserFragment extends Fragment implements OnEventListener,
		OnNavigateListener {

	private Activity mActivity;
	private FragmentManager fm;

	private MultiFileObserver mObserver;
	private FileObserverCache mObserverCache;
	private Runnable mLastRunnable;
	private static Handler sHandler;

	private ActionModeController mActionController;
	private static BrowserListAdapter mListAdapter;
	public static ArrayList<String> mDataSource;
	public static String mCurrentPath;
	private boolean mUseBackKey = true;
	private static NavigationView mNavigation;
	private AbsListView mListView;

	// TODO bug: after moving back from settings there is no fragment
	@Override
	public void onActivityCreated(Bundle state) {
		super.onActivityCreated(state);
		mActivity = (Browser) getActivity();
		Intent intent = mActivity.getIntent();
		fm = getFragmentManager();

		setHasOptionsMenu(true);

		init();
		initDirectory(state, intent);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_browser, container,
				false);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		navigateTo(mCurrentPath);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.onDestroy();
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
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("location", BrowserFragment.mCurrentPath);
	}

	private void init() {
		mDataSource = new ArrayList<String>();
		mObserverCache = FileObserverCache.getInstance();
		mNavigation = new NavigationView(mActivity);
		mActionController = new ActionModeController(mActivity);

		// start IconPreview class to get thumbnails if BrowserListAdapter
		// request them
		new IconPreview(mActivity);

		// new ArrayAdapter
		mListAdapter = new BrowserListAdapter(mActivity, mDataSource);

		if (sHandler == null) {
			sHandler = new Handler(mActivity.getMainLooper());
		}

		// get the browser list
		mListView = (ListView) mActivity.findViewById(android.R.id.list);
		mListView.setEmptyView(mActivity.findViewById(android.R.id.empty));
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

	public void navigateTo(String path) {
		mCurrentPath = path;

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

	public void listItemAction(File file) {
		String item_ext = FilenameUtils.getExtension(file.getName());

		if (item_ext.equalsIgnoreCase("zip")
				|| item_ext.equalsIgnoreCase("rar")) {
			final DialogFragment dialog = UnpackDialog.instantiate(file);
			dialog.show(fm, Browser.TAG_DIALOG);
		} else {
			SimpleUtils.openFile(mActivity, file);
		}
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);

		MenuItem paste = menu.findItem(R.id.paste);
		paste.setVisible(!ClipBoard.isEmpty());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.createfile:
			final DialogFragment dialog1 = new CreateFileDialog();
			dialog1.show(fm, Browser.TAG_DIALOG);
			return true;
		case R.id.createfolder:
			final DialogFragment dialog2 = new CreateFolderDialog();
			dialog2.show(fm, Browser.TAG_DIALOG);
			return true;
		case R.id.folderinfo:
			final DialogFragment dirInfo = new DirectoryInfoDialog();
			dirInfo.show(fm, Browser.TAG_DIALOG);
			return true;
		case R.id.search:
			Intent sintent = new Intent(mActivity, SearchActivity.class);
			startActivity(sintent);
			return true;
		case R.id.paste:
			final PasteTaskExecutor ptc = new PasteTaskExecutor(mActivity,
					mCurrentPath);
			ptc.start();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onNavigate(String path) {
		// navigate to path when ActionBarNavigation button is clicked
		navigateTo(path);
		// go to the top of the ListView
		mListView.setSelection(0);
	}

	private void initDirectory(Bundle savedInstanceState, Intent intent) {
		String defaultdir;

		if (savedInstanceState != null) {
			// get directory when you rotate your phone
			defaultdir = savedInstanceState.getString("location");
		} else {
			try {
				File dir = new File(
						intent.getStringExtra(Browser.EXTRA_SHORTCUT));

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

	public static void listDirectory(String path) {
		ArrayList<String> ab = SimpleUtils.listFiles(path);
		mCurrentPath = path;

		if (!mDataSource.isEmpty())
			mDataSource.clear();

		for (String data : ab)
			mDataSource.add(data);

		mListAdapter.notifyDataSetChanged();
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

	public static NavigationView getNavigation() {
		return mNavigation;
	}

	public void onBookmarkClick(Cursor mBookmarksCursor, int position) {
		if (mBookmarksCursor.moveToPosition(position)) {
			File file = new File(mBookmarksCursor.getString(mBookmarksCursor
					.getColumnIndex(Bookmarks.PATH)));

			if (!file.exists())
				return;

			if (file.isDirectory()) {
				if (!mUseBackKey)
					mUseBackKey = true;

				navigateTo(file.getAbsolutePath());

				// go to the top of the ListView
				mListView.setSelection(0);
			} else {
				listItemAction(file);
			}
		}
	}

	public boolean onBackPressed(int keycode, KeyEvent event) {
		if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !mCurrentPath.equals("/")) {
			File file = new File(mCurrentPath);
			navigateTo(file.getParent());

			// get position of the previous folder in ListView
			mListView.setSelection(mListAdapter.getPosition(file.getPath()));
			return true;
		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& mCurrentPath.equals("/")) {
			Toast.makeText(mActivity, getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			mUseBackKey = false;
			return false;
		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& mCurrentPath.equals("/")) {
			mActivity.finish();
			return false;
		}

		return true;
	}
}
