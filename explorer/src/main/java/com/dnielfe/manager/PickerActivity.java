package com.dnielfe.manager;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.dnielfe.manager.fragments.BrowserFragment;

public class PickerActivity extends AbstractBrowserActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        initToolbar();
        setToolbarTitle(getText(R.string.picker_choose_one_file));
        initDrawer();
        initBrowserFragment();
    }

    @Override
    protected BrowserFragment getCurrentBrowserFragment() {
        return (BrowserFragment) getFragmentManager()
                .findFragmentByTag(BrowserFragment.TAG_PRIMARY_BROWSER_LIST_FRAGMENT);
    }

    private void initBrowserFragment() {
        final Bundle args = new Bundle();
        args.putBoolean(BrowserFragment.KEY_IS_GET_CONTENT, true);

        final BrowserFragment browserFragment = new BrowserFragment();
        browserFragment.setArguments(args);
        getFragmentManager().beginTransaction()
                .add(R.id.browser_fragment_container, browserFragment, BrowserFragment.TAG_PRIMARY_BROWSER_LIST_FRAGMENT)
                .commit();
    }

    private void setToolbarTitle(CharSequence title) {
        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}
