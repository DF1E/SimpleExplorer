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

import com.dnielfe.manager.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

public final class FileExistsDialog extends Dialog {

	public FileExistsDialog(final Context context, final String source,
			final String target, final View.OnClickListener abortAction,
			final View.OnClickListener skipAction,
			final View.OnClickListener skipAllAction,
			final View.OnClickListener replaceAction,
			final View.OnClickListener replaceAllAction) {
		super(context);

		this.setTitle(R.string.overwrite_title);
		this.setContentView(R.layout.dialog_exists);

		this.initView(source, target, abortAction, skipAction, skipAllAction,
				replaceAction, replaceAllAction);
	}

	private void initView(final String sourcePath, final String targetPath,
			final View.OnClickListener abortAction,
			final View.OnClickListener skipAction,
			final View.OnClickListener skipAllAction,
			final View.OnClickListener replaceAction,
			final View.OnClickListener replaceAllAction) {

		final TextView source = (TextView) this
				.findViewById(android.R.id.text1);
		source.setText(sourcePath);

		final TextView target = (TextView) this
				.findViewById(android.R.id.text2);
		target.setText(targetPath);

		this.findViewById(android.R.id.button1).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						replaceAction.onClick(v);
					}
				});
		this.findViewById(android.R.id.button2).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						replaceAllAction.onClick(v);
					}
				});
		this.findViewById(android.R.id.button3).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						skipAction.onClick(v);
					}
				});
		this.findViewById(R.id.button4).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						skipAllAction.onClick(v);
					}
				});
		this.findViewById(R.id.button5).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						abortAction.onClick(v);
					}
				});
		this.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				skipAllAction.onClick(findViewById(R.id.button5));
			}
		});
	}
}
