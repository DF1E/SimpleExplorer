package com.dnielfe.manager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.DeleteTask;

public final class DeleteFilesDialog extends DialogFragment {

    private static String[] files;

    public static DialogFragment instantiate(String[] files1) {
        files = files1;
        return new DeleteFilesDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        final Activity a = getActivity();
        final int size = files.length;

        final AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(String.valueOf(size) + getString(R.string._files));
        b.setMessage(R.string.cannotbeundoneareyousureyouwanttodelete);
        b.setPositiveButton(R.string.delete,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final DeleteTask task = new DeleteTask(a);
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                files);
                    }
                });
        b.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return b.create();
    }
}
