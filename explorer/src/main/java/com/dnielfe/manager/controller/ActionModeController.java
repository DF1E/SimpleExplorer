package com.dnielfe.manager.controller;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;

import com.dnielfe.manager.BrowserActivity;
import com.dnielfe.manager.R;
import com.dnielfe.manager.SearchActivity;
import com.dnielfe.manager.adapters.BookmarksAdapter;
import com.dnielfe.manager.dialogs.DeleteFilesDialog;
import com.dnielfe.manager.dialogs.FilePropertiesDialog;
import com.dnielfe.manager.dialogs.GroupOwnerDialog;
import com.dnielfe.manager.dialogs.RenameDialog;
import com.dnielfe.manager.dialogs.ZipFilesDialog;
import com.dnielfe.manager.settings.Settings;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.SimpleUtils;

import java.io.File;
import java.util.ArrayList;

public final class ActionModeController {

    private final MultiChoiceModeListener multiChoiceListener;
    private final Activity mActivity;
    private AbsListView mListView;
    private ActionMode mActionMode;

    public ActionModeController(final Activity activity) {
        this.mActivity = activity;
        this.multiChoiceListener = new MultiChoiceListener();
    }

    public void finishActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
    }

    public void setListView(AbsListView list) {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
        this.mListView = list;
        this.mListView.setMultiChoiceModeListener(this.multiChoiceListener);
    }

    public boolean isActionMode() {
        return mActionMode != null;
    }

    private final class MultiChoiceListener implements MultiChoiceModeListener {
        final String mSelected = mActivity.getString(R.string._selected);

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.clear();
            mActivity.getMenuInflater().inflate(R.menu.actionmode, menu);

            if (mActivity instanceof SearchActivity) {
                    menu.removeItem(R.id.actiongroupowner);
                    menu.removeItem(R.id.actionrename);
                    menu.removeItem(R.id.actionzip);

                if (mListView.getCheckedItemCount() > 1) {
                    menu.removeItem(R.id.actiondetails);
                }
            } else {
                if (!Settings.rootAccess())
                    menu.removeItem(R.id.actiongroupowner);

                if (mListView.getCheckedItemCount() > 1) {
                    menu.removeItem(R.id.actionrename);
                    menu.removeItem(R.id.actiongroupowner);
                    menu.removeItem(R.id.actiondetails);
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ActionModeController.this.mActionMode = null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            ActionModeController.this.mActionMode = mode;
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            final SparseBooleanArray items = mListView.getCheckedItemPositions();
            final int checkedItemSize = items.size();
            final String[] files = new String[mListView.getCheckedItemCount()];
            int index = -1;

            switch (item.getItemId()) {
                case R.id.actionmove:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }
                    ClipBoard.cutMove(files);
                    mode.finish();
                    mActivity.invalidateOptionsMenu();
                    return true;
                case R.id.actioncopy:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }
                    ClipBoard.cutCopy(files);
                    mode.finish();
                    mActivity.invalidateOptionsMenu();
                    return true;
                case R.id.actiongroupowner:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            final DialogFragment dialog9 = GroupOwnerDialog
                                    .instantiate(new File((String) mListView
                                            .getItemAtPosition(key)));
                            mode.finish();
                            dialog9.show(mActivity.getFragmentManager(), BrowserActivity.TAG_DIALOG);
                            break;
                        }
                    }
                    return true;
                case R.id.actiondelete:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }
                    final DialogFragment dialog1 = DeleteFilesDialog.instantiate(files);
                    mode.finish();
                    dialog1.show(mActivity.getFragmentManager(), BrowserActivity.TAG_DIALOG);
                    return true;
                case R.id.actionshare:
                    final ArrayList<Uri> uris = new ArrayList<>(mListView.getCheckedItemCount());
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            final File selected = new File((String) mListView.getItemAtPosition(key));
                            if (!selected.isDirectory()) {
                                uris.add(Uri.fromFile(selected));
                            }
                        }
                    }

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setType("*/*");
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    mode.finish();
                    mActivity.startActivity(Intent.createChooser(intent,
                            mActivity.getString(R.string.share)));
                    return true;
                case R.id.actionshortcut:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }

                    for (String a : files) {
                        SimpleUtils.createShortcut(mActivity, a);
                    }
                    mode.finish();
                    return true;
                case R.id.actionbookmark:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }

                    BookmarksAdapter mAdapter = BrowserActivity.getBookmarksAdapter();

                    for (String a : files) {
                        mAdapter.createBookmark(new File(a));
                    }
                    mode.finish();
                    return true;
                case R.id.actionzip:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            files[++index] = (String) mListView.getItemAtPosition(key);
                        }
                    }
                    final DialogFragment dialog = ZipFilesDialog.instantiate(files);
                    mode.finish();
                    dialog.show(mActivity.getFragmentManager(), BrowserActivity.TAG_DIALOG);
                    return true;
                case R.id.actionrename:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            final DialogFragment dialog3 = RenameDialog
                                    .instantiate(new File((String) mListView
                                            .getItemAtPosition(key)).getName());
                            mode.finish();
                            dialog3.show(mActivity.getFragmentManager(), BrowserActivity.TAG_DIALOG);
                            break;
                        }
                    }
                    return true;
                case R.id.actiondetails:
                    for (int i = 0; i < checkedItemSize; i++) {
                        final int key = items.keyAt(i);
                        if (items.get(key)) {
                            final DialogFragment dialog4 = FilePropertiesDialog
                                    .instantiate(new File((String) mListView.getItemAtPosition(key)));
                            mode.finish();
                            dialog4.show(mActivity.getFragmentManager(),
                                    BrowserActivity.TAG_DIALOG);
                            break;
                        }
                    }
                    return true;
                case R.id.actionall:
                    for (int i = 0; i < mListView.getCount(); i++) {
                        mListView.setItemChecked(i, true);
                    }

                    mode.invalidate();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position,
                                              long id, boolean checked) {
            mode.setTitle(mListView.getCheckedItemCount() + mSelected);
        }
    }
}
