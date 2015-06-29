package com.dnielfe.manager;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.dnielfe.manager.fragments.PickerFragment;

public final class PickerActivity extends AbstractBrowserActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        initToolbar();
        setToolbarTitle(getText(R.string.picker_choose_one_file));
        initBrowserFragment();
    }

    @Override
    public PickerFragment getCurrentBrowserFragment() {
        return (PickerFragment) getFragmentManager()
                .findFragmentByTag(PickerFragment.TAG);
    }

    private void initBrowserFragment() {
        final PickerFragment pickerFragment = new PickerFragment();
        getFragmentManager().beginTransaction()
                .add(R.id.browser_fragment_container, pickerFragment, PickerFragment.TAG)
                .commit();
    }

    private void setToolbarTitle(CharSequence title) {
        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}
