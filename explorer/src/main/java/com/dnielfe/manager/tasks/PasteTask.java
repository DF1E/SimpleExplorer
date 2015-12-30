package com.dnielfe.manager.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dnielfe.manager.R;
import com.dnielfe.manager.utils.ClipBoard;
import com.dnielfe.manager.utils.MediaStoreUtils;
import com.dnielfe.manager.utils.SimpleUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class PasteTask extends AsyncTask<String, Void, List<String>> {

    private final WeakReference<Activity> activity;

    private ProgressDialog dialog;

    private final File location;

    private boolean success = false;

    public PasteTask(final Activity activity, File currentDir) {
        this.activity = new WeakReference<>(activity);
        this.location = currentDir;
    }

    @Override
    protected void onPreExecute() {
        final Activity activity = this.activity.get();

        if (activity != null) {
            this.dialog = new ProgressDialog(activity);

            if (ClipBoard.isMove())
                this.dialog.setMessage(activity.getString(R.string.moving));
            else
                this.dialog.setMessage(activity.getString(R.string.copying));

            this.dialog.setCancelable(true);
            this.dialog
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cancel(false);
                        }
                    });
            if (!activity.isFinishing()) {
                this.dialog.show();
            }
        }
    }

    @Override
    protected List<String> doInBackground(String... content) {
        final List<String> failed = new ArrayList<>();
        final Activity activity = this.activity.get();
        ClipBoard.lock();

        for (String s : content) {
            String file_name = s.substring(s.lastIndexOf("/"), s.length());
            if (ClipBoard.isMove()) {
                SimpleUtils.moveToDirectory(new File(s), new File(location, file_name), activity);
                success = true;
            } else {
                SimpleUtils.copyFile(new File(s), new File(location, file_name), activity);
                success = true;
            }
        }

        if (location.canRead()) {
            for (File file : location.listFiles())
                MediaStoreUtils.addFileToMediaStore(file.getPath(), activity);
        }
        return failed;
    }

    @Override
    protected void onPostExecute(final List<String> failed) {
        super.onPostExecute(failed);
        this.finish(failed);
    }

    @Override
    protected void onCancelled(final List<String> failed) {
        super.onCancelled(failed);
        this.finish(failed);
    }

    private void finish(final List<String> failed) {
        if (this.dialog != null) {
            this.dialog.dismiss();
        }

        final Activity activity = this.activity.get();

        if (ClipBoard.isMove()) {
            if (success)
                Toast.makeText(activity,
                        activity.getString(R.string.movesuccsess),
                        Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(activity, activity.getString(R.string.movefail),
                        Toast.LENGTH_SHORT).show();
        } else {
            if (success)
                Toast.makeText(activity,
                        activity.getString(R.string.copysuccsess),
                        Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(activity, activity.getString(R.string.copyfail),
                        Toast.LENGTH_SHORT).show();
        }

        ClipBoard.unlock();
        ClipBoard.clear();
        activity.invalidateOptionsMenu();

        if (!failed.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.cantopenfile),
                    Toast.LENGTH_SHORT).show();
            if (!activity.isFinishing()) {
                dialog.show();
            }
        }
    }
}
