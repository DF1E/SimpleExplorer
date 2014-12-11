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
import android.widget.EditText;

import com.dnielfe.manager.BrowserActivity;
import com.dnielfe.manager.R;
import com.dnielfe.manager.tasks.UnZipTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public final class UnpackDialog extends DialogFragment {

    private static File file;
    private static String ext;

    public static DialogFragment instantiate(File file1) {
        file = file1;
        ext = FilenameUtils.getExtension(file1.getName());
        return new UnpackDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        final Activity a = getActivity();

        // Set an EditText view to get user input
        final EditText inputf = new EditText(a);
        inputf.setHint(R.string.enter_name);
        inputf.setText(BrowserActivity.getCurrentlyDisplayedFragment().mCurrentPath + "/");

        final AlertDialog.Builder b = new AlertDialog.Builder(a);
        b.setTitle(R.string.extractto);
        b.setView(inputf);
        b.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String newpath = inputf.getText().toString();

                        dialog.dismiss();

                        if (ext.equals("zip")) {
                            final UnZipTask task = new UnZipTask(a);
                            task.executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR,
                                    file.getPath(), newpath);
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
