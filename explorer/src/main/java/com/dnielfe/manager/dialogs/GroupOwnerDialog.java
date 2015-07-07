package com.dnielfe.manager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.GroupOwnerTask;
import com.dnielfe.manager.utils.RootCommands;

import java.io.File;

public final class GroupOwnerDialog extends DialogFragment {

    private static File file;
    private static String oldowner, oldgroup;

    public static DialogFragment instantiate(File file1) {
        file = file1;

        String[] mFileInfo = RootCommands.getFileProperties(file1);
        if (mFileInfo != null) {
            oldowner = mFileInfo[1];
            oldgroup = mFileInfo[2];
        }
        return new GroupOwnerDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        final Activity a = getActivity();

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_groupowner, null);

        final EditText inputowner = (EditText) view.findViewById(R.id.owner);
        inputowner.setText(oldowner);

        final EditText inputgroup = (EditText) view.findViewById(R.id.group);
        inputgroup.setText(oldgroup);

        final AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(R.string.edit);
        b.setView(view);
        b.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newgroup = inputgroup.getText().toString();
                        String newowner = inputowner.getText().toString();

                        dialog.dismiss();

                        if (newgroup.length() > 1 && newowner.length() > 1) {
                            final GroupOwnerTask task = new GroupOwnerTask(a, newgroup, newowner);
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
                        }
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
