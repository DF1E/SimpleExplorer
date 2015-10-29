package com.dnielfe.manager.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dnielfe.manager.R;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;

public final class PickerFragment extends AbstractBrowserFragment {
    public static final String TAG = "PickerFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browser, container, false);

        initList(inflater, rootView);
        initFab(rootView);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.picker_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pick_cancel:
                getActivity().finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void listItemAction(File file) {
        final Uri pickedUri = FileProvider.getUriForFile(getActivity(), "com.dnielfe.manager.fileprovider", file);

        final Intent result = new Intent();
        result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        result.setData(pickedUri);

        final Activity activity = getActivity();
        activity.setResult(Activity.RESULT_OK, result);
        activity.finish();
    }

    @Override
    protected void initFab(View rootView) {
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fabbutton);
        fab.setVisibility(View.GONE);
    }
}
