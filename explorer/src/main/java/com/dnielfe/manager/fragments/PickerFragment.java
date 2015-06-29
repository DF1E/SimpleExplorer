package com.dnielfe.manager.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dnielfe.manager.R;

import java.io.File;

public final class PickerFragment extends AbstractBrowserFragment {
    public static final String TAG = "PickerFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_picker, container, false);

        initList(inflater, rootView);
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
        final Uri pickedUri = Uri.fromFile(file);

        final Intent result = new Intent();
        result.setData(pickedUri);

        final Activity activity = getActivity();
        activity.setResult(Activity.RESULT_OK, result);
        activity.finish();
    }
}
