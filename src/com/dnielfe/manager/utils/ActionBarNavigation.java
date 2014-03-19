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

package com.dnielfe.manager.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.dnielfe.manager.R;

public class ActionBarNavigation {

	private Set<OnNavigateListener> listeners;

	private static int directorytextsize = 16;

	private LinearLayout mDirectoryButtons;
	private Activity mActivity;

	public interface OnNavigateListener {
		void onNavigate(String path);
	}

	public ActionBarNavigation(Activity activity) {
		this.mActivity = activity;
		this.listeners = new HashSet<OnNavigateListener>();
	}

	public void setDirectoryButtons(String path) {
		File currentDirectory = new File(path);

		HorizontalScrollView scrolltext = (HorizontalScrollView) mActivity
				.findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) mActivity
				.findViewById(R.id.directory_buttons);
		mDirectoryButtons.removeAllViews();

		String[] parts = currentDirectory.getAbsolutePath().split("/");

		int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
		int MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT;

		// Add home button separately
		Button bt = new Button(mActivity, null,
				android.R.attr.borderlessButtonStyle);
		bt.setText("/");
		bt.setTextSize(directorytextsize);
		bt.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
				WRAP_CONTENT, Gravity.CENTER_VERTICAL));
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				for (final OnNavigateListener listener : listeners) {
					listener.onNavigate("/");
				}
			}
		});

		mDirectoryButtons.addView(bt);

		// Add other buttons
		String dir = "";

		for (int i = 1; i < parts.length; i++) {
			dir += "/" + parts[i];

			FrameLayout fv1 = new FrameLayout(mActivity);
			fv1.setBackground(mActivity.getResources().getDrawable(
					R.drawable.listmore));
			fv1.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
					MATCH_PARENT, Gravity.CENTER_VERTICAL));

			Button b = new Button(mActivity, null,
					android.R.attr.borderlessButtonStyle);
			b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
					WRAP_CONTENT, Gravity.CENTER_VERTICAL));
			b.setText(parts[i]);
			b.setTextSize(directorytextsize);
			b.setTag(dir);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					String dir1 = (String) view.getTag();
					for (final OnNavigateListener listener : listeners) {
						listener.onNavigate(dir1);
					}
				}
			});

			b.setOnLongClickListener(new View.OnLongClickListener() {
				public boolean onLongClick(View view) {
					String dir1 = (String) view.getTag();
					SimpleUtils.savetoClipBoard(mActivity, dir1);
					return true;
				}
			});

			mDirectoryButtons.addView(fv1);
			mDirectoryButtons.addView(b);
			scrolltext.postDelayed(new Runnable() {
				public void run() {
					HorizontalScrollView hv = (HorizontalScrollView) mActivity
							.findViewById(R.id.scroll_text);
					hv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
				}
			}, 100L);
		}
	}

	public void addonNavigateListener(final OnNavigateListener listener) {
		this.listeners.add(listener);
	}

	public void removeOnNavigateListener(final OnNavigateListener listener) {
		this.listeners.remove(listener);
	}
}