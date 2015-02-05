/*
 * Copyright (C) 2013 - 2015 Daniel F.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.dnielfe.manager.adapters.BookmarksAdapter;
import com.dnielfe.manager.adapters.BrowserTabsAdapter;
import com.dnielfe.manager.adapters.DrawerListAdapter;
import com.dnielfe.manager.adapters.MergeAdapter;
import com.dnielfe.manager.fragments.BrowserFragment;
import com.dnielfe.manager.preview.IconPreview;
import com.dnielfe.manager.settings.SettingsActivity;
import com.dnielfe.manager.ui.NavigationView;
import com.dnielfe.manager.ui.NavigationView.OnNavigateListener;
import com.dnielfe.manager.ui.PageIndicator;

import java.io.File;

public final class BrowserActivity extends ThemableActivity implements
        OnNavigateListener, BrowserFragment.onUpdatePathListener {

    public static final String EXTRA_SHORTCUT = "shortcut_path";
    public static final String TAG_DIALOG = "dialog";

    private static MergeAdapter mMergeAdapter;
    private static BookmarksAdapter mBookmarksAdapter;
    private static DrawerListAdapter mMenuAdapter;
    private static NavigationView mNavigation;

    private static ListView mDrawer;
    private static DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;

    private FragmentManager fm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mNavigation != null)
            mNavigation.removeOnNavigateListener(this);
    }

    @Override
    public void onTrimMemory(int level) {
        IconPreview.clearCache();
    }

    private void init() {
        fm = getFragmentManager();
        mNavigation = new NavigationView(this);

        setupDrawer();
        initDrawerList();

        // add listener for navigation view
        if (mNavigation.listeners.isEmpty())
            mNavigation.addonNavigateListener(this);

        // start IconPreview class to get thumbnails if BrowserListAdapter
        // request them
        new IconPreview(this);

        // Instantiate a ViewPager and a PagerAdapter.
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        BrowserTabsAdapter mPagerAdapter = new BrowserTabsAdapter(fm);
        mPager.setAdapter(mPagerAdapter);

        PageIndicator mIndicator = (PageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setFades(false);
    }

    private void setupDrawer() {
        mDrawer = (ListView) findViewById(R.id.left_drawer);

        // Set shadow of navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);

        // Add Navigation Drawer to ActionBar
        mDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(this, mDrawerLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void initDrawerList() {
        mBookmarksAdapter = new BookmarksAdapter(this);
        mMenuAdapter = new DrawerListAdapter(this);

        // create MergeAdapter to combine multiple adapter
        mMergeAdapter = new MergeAdapter();
        mMergeAdapter.addAdapter(mBookmarksAdapter);
        mMergeAdapter.addAdapter(mMenuAdapter);

        mDrawer.setAdapter(mMergeAdapter);
        mDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mMergeAdapter.getAdapter(position).equals(mBookmarksAdapter)) {

                    // handle bookmark items
                    if (mDrawerLayout.isDrawerOpen(mDrawer))
                        mDrawerLayout.closeDrawer(mDrawer);

                    File file = new File(mBookmarksAdapter.getItem(position).getPath());
                    BrowserTabsAdapter.getCurrentBrowserFragment().onBookmarkClick(file);
                } else if (mMergeAdapter.getAdapter(position).equals(mMenuAdapter)) {
                    // handle menu items
                    switch ((int) mMergeAdapter.getItemId(position)) {
                        case 0:
                            Intent intent2 = new Intent(BrowserActivity.this,
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

    public static boolean isDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(mDrawer);
    }

    public static BookmarksAdapter getBookmarksAdapter() {
        return mBookmarksAdapter;
    }

    @Override
    public boolean onKeyDown(int keycode, @NonNull KeyEvent event) {
        if (keycode != KeyEvent.KEYCODE_BACK)
            return false;

        if (isDrawerOpen())
            mDrawerLayout.closeDrawer(mDrawer);
        return BrowserTabsAdapter.getCurrentBrowserFragment().onBackPressed();
    }

    @Override
    public void onNavigate(String path) {
        BrowserTabsAdapter.getCurrentBrowserFragment().onNavigate(path);
    }

    @Override
    public void onUpdatePath(String path) {
        mNavigation.setDirectoryButtons(path);
    }
}
