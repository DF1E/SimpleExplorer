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

import com.dnielfe.manager.adapters.BookmarksAdapter;
import com.dnielfe.manager.adapters.DrawerListAdapter;
import com.dnielfe.manager.adapters.MergeAdapter;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.settings.SettingsActivity;
import com.dnielfe.manager.utils.Bookmarks;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public final class Browser extends ThemableActivity {

	public static final String EXTRA_SHORTCUT = "shortcut_path";
	public static final String TAG_DIALOG = "dialog";

	private ActionBar mActionBar;
	private static MergeAdapter mMergeAdapter;
	private static BookmarksAdapter mBookmarksAdapter;
	private static DrawerListAdapter mMenuAdapter;
	private ListView mDrawer;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private Cursor mBookmarksCursor;

	private FragmentManager fm;
	private BrowserFragment mBrowserFragment;

	// TODO fix savedinstance npe
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_browser);

		Intent intent = getIntent();

		if (savedInstanceState == null) {
			savedInstanceState = intent.getBundleExtra(EXTRA_SAVED_STATE);
		}

		init(savedInstanceState);
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
		final Fragment f = fm.findFragmentByTag(TAG_DIALOG);

		if (f != null) {
			fm.beginTransaction().remove(f).commit();
			fm.executePendingTransactions();
		}
	}

	private void init(Bundle savedInstanceState) {
		fm = getFragmentManager();

		mActionBar = this.getActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.show();

		setupDrawer();
		initDrawerList();

		if (savedInstanceState == null && mBrowserFragment == null) {
			mBrowserFragment = new BrowserFragment();
			addFragment(mBrowserFragment);
		}
	}

	private void setupDrawer() {
		final TypedArray array = obtainStyledAttributes(new int[] { R.attr.themeId });
		final int themeId = array.getInteger(0, SimpleExplorer.THEME_ID_LIGHT);
		array.recycle();
		int icon;

		mDrawer = (ListView) findViewById(R.id.left_drawer);

		// Set shadow of navigation drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);

		if (themeId == SimpleExplorer.THEME_ID_LIGHT)
			icon = R.drawable.holo_light_ic_drawer;
		else
			icon = R.drawable.holo_dark_ic_drawer;

		// Add Navigation Drawer to ActionBar
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, icon,
				R.string.drawer_open, R.string.drawer_close) {

			@Override
			public void onDrawerOpened(View view) {
				super.onDrawerOpened(view);
				invalidateOptionsMenu();
			}

			@Override
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
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

					mBrowserFragment
							.onBookmarkClick(mBookmarksCursor, position);
				} else if (mMergeAdapter.getAdapter(position).equals(
						mMenuAdapter)) {
					// handle menu items
					switch ((int) mMergeAdapter.getItemId(position)) {
					case 0:
						Intent intent2 = new Intent(Browser.this,
								SettingsActivity.class);
						startActivity(intent2);
						break;
					case 1:
						finish();
					}
				}
			}
		});
	}

	private void addFragment(Fragment f) {
		FragmentTransaction transaction = fm.beginTransaction();
		transaction.replace(R.id.container, f);
		transaction.addToBackStack(null);
		transaction.commit();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawer)) {
				mDrawerLayout.closeDrawer(mDrawer);
			} else {
				mDrawerLayout.openDrawer(mDrawer);
			}
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

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		return mBrowserFragment.onBackPressed(keycode, event);
	}
}
