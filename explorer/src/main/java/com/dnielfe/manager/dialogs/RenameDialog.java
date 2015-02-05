package com.dnielfe.manager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;

import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.RenameTask;

public final class RenameDialog extends DialogFragment {

    private static String name;

    public static DialogFragment instantiate(String name1) {
        name = name1;
        return new RenameDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        final Activity a = getActivity();

        // Set an EditText view to get user input
        final EditText inputf = new EditText(a);
        inputf.setHint(R.string.enter_name);
        inputf.setText(name);

        final AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(R.string.rename);
        b.setView(inputf);

        b.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newname = inputf.getText().toString();

                        if (inputf.getText().length() < 1)
                            dialog.dismiss();

                        dialog.dismiss();
                        final RenameTask task = new RenameTask(a);
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                name, newname);
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
