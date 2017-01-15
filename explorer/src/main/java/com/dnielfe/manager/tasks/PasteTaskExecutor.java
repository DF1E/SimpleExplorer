package com.dnielfe.manager.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.dnielfe.manager.R;
import com.dnielfe.manager.dialogs.FileExistsDialog;
import com.dnielfe.manager.utils.ClipBoard;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;

public final class PasteTaskExecutor implements OnClickListener {

    private final WeakReference<Activity> mActivityReference;

    private final File mLocation;
    private final LinkedList<String> mToProcess;
    private final HashMap<String, String> mExisting;

    private String current;

    public PasteTaskExecutor(final Activity activity, final String location) {
        this.mActivityReference = new WeakReference<>(activity);
        this.mLocation = new File(location);
        this.mToProcess = new LinkedList<>();
        this.mExisting = new HashMap<>();
    }

    public void start() {
        final String[] contents = ClipBoard.getClipBoardContents();
        if (contents == null) {
            return;
        }

        for (final String ab : contents) {
            File file = new File(ab);

            if (file.exists()) {
                final File testTarget = new File(mLocation, file.getName());

                if (testTarget.exists()) {
                    mExisting.put(testTarget.getPath(), file.getPath());
                } else {
                    mToProcess.add(file.getPath());
                }
            }
        }

        next();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case android.R.id.button1:
                // replace
                mToProcess.add(current);
                break;

            case android.R.id.button2:
                // replace all;
                mToProcess.add(current);
                for (String f : mExisting.keySet()) {
                    mToProcess.add(mExisting.get(f));
                }
                mExisting.clear();
                break;

            case R.id.button4:
                // skip all
                mExisting.clear();
                break;

            case R.id.button5:
                // abort
                mExisting.clear();
                mToProcess.clear();
                return;
        }

        next();
    }

    private void next() {
        final Activity a = this.mActivityReference.get();
        if (a != null) {
            if (mExisting.isEmpty()) {
                if (mToProcess.isEmpty()) {
                    ClipBoard.clear();
                } else {
                    String[] array = new String[mToProcess.size()];
                    for (int i = 0; i < mToProcess.size(); i++) {
                        array[i] = mToProcess.get(i);
                    }

                    mToProcess.toArray(array);

                    final PasteTask task = new PasteTask(a, mLocation);
                    task.execute(array);
                }
            } else {
                final String key = mExisting.keySet().iterator().next();
                this.current = mExisting.get(key);
                mExisting.remove(key);

                final Dialog dialog = new FileExistsDialog(a, current, key,
                        this, this, this, this, this);
                if (!a.isFinishing()) {
                    dialog.show();
                }
            }

            a.invalidateOptionsMenu();
        }
    }
}
