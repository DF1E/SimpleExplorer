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

import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.DeleteTask;

public final class DeleteFilesDialog extends DialogFragment {

    private static String[] files;

    public static DialogFragment instantiate(String[] files1) {
        files = files1;

        final DeleteFilesDialog dialog = new DeleteFilesDialog();
        return dialog;
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
