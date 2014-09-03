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

package com.dnielfe.manager;

import java.io.File;
import java.util.ArrayList;

import com.dnielfe.manager.adapters.BrowserListAdapter;
import com.dnielfe.manager.utils.SimpleUtils;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class SearchActivity extends ThemableActivity {

	private ActionBar mActionBar;
	private ListView mListView;
	private SearchTask mTask;
	private BrowserListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		init();

		if (savedInstanceState != null) {
			mAdapter.addContent(savedInstanceState
					.getStringArrayList("savedList"));

			mActionBar.setSubtitle(String.valueOf(mAdapter.getCount())
					+ getString(R.string._files));
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		SearchIntent(intent);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("savedList", mAdapter.getContent());
	}

	private void init() {
		mActionBar = getActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.show();

		mAdapter = new BrowserListAdapter(this, getLayoutInflater());

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				File f = new File(mAdapter.getItem(position));

				if (f.isDirectory()) {
					finish();

					BrowserActivity.getCurrentlyDisplayedFragment().navigateTo(
							f.getAbsolutePath());
				} else if (f.isFile()) {
					SimpleUtils.openFile(SearchActivity.this, f);
				}
			}
		});
	}

	private void SearchIntent(Intent intent) {
		setIntent(intent);

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String mQuery = intent.getStringExtra(SearchManager.QUERY);

			if (mQuery.length() > 0) {
				mTask = new SearchTask(this);
				mTask.execute(mQuery);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.search_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			this.onBackPressed();
			return true;
		case R.id.action_search:
			this.onSearchRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class SearchTask extends AsyncTask<String, Void, ArrayList<String>> {
		public ProgressDialog pr_dialog;
		private String mLocation;
		private Context context;

		private SearchTask(Context c) {
			context = c;
			mLocation = BrowserActivity.getCurrentlyDisplayedFragment().mCurrentPath;
		}

		@Override
		protected void onPreExecute() {
			pr_dialog = ProgressDialog.show(context, null,
					getString(R.string.search));
			pr_dialog.setCanceledOnTouchOutside(true);
		}

		@Override
		protected ArrayList<String> doInBackground(String... params) {
			ArrayList<String> found = SimpleUtils.searchInDirectory(mLocation,
					params[0]);
			return found;
		}

		@Override
		protected void onPostExecute(final ArrayList<String> files) {
			int len = files != null ? files.size() : 0;

			pr_dialog.dismiss();

			if (len == 0) {
				Toast.makeText(context, R.string.itcouldntbefound,
						Toast.LENGTH_SHORT).show();
				mActionBar.setSubtitle(null);
			} else {
				mAdapter.addContent(files);

				mActionBar.setSubtitle(String.valueOf(len)
						+ getString(R.string._files));
			}
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}
