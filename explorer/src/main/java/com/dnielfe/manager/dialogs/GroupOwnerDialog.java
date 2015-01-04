/*
 * Copyright (C) 2014 Simple Explorer
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

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

        String[] mFileInfo = RootCommands.getFileProperties(file);
        oldowner = mFileInfo[1];
        oldgroup = mFileInfo[2];
        return new GroupOwnerDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        final Activity a = getActivity();

        View view = getActivity().getLayoutInflater().inflate(
                R.layout.dialog_groupowner, null);

        final EditText inputowner = (EditText) view.findViewById(R.id.owner);
        inputowner.setText(oldowner);

        final EditText inputgroup = (EditText) view.findViewById(R.id.group);
        inputgroup.setText(oldgroup);

        final AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(R.string.edit);
        b.setView(view);
        b.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newgroup = inputowner.getText().toString();
                        String newowner = inputgroup.getText().toString();

                        dialog.dismiss();

                        if (newgroup.length() > 1 && newowner.length() > 1) {
                            final GroupOwnerTask task = new GroupOwnerTask(a,
                                    newgroup, newowner);
                            task.executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR, file);
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
