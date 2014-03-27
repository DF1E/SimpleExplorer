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

import com.dnielfe.manager.adapters.SearchListAdapter;
import com.dnielfe.manager.utils.ActionBarNavigation;
import com.dnielfe.manager.utils.SimpleUtils;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SearchActivity extends ThemableActivity {

	private ActionBarNavigation mActionBarNavigation;

	public static String mQuery;
	private static String mSearchDirectory;

	private ListView mListView;
	private SearchTask mTask;
	private ArrayList<String> mData;
	private SearchListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		init();

		mData = new ArrayList<String>();
		mAdapter = new SearchListAdapter(this, mData);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mOnItemClickListener);

		if (savedInstanceState != null) {
			restart(savedInstanceState);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("foundlist", mData);
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		SearchIntent(intent);
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Object filepath = mListView.getAdapter().getItem(position);
			File f = new File(filepath.toString());

			if (f.isDirectory()) {
				finish();
				Browser.mCurrentPath = f.getPath();
				mActionBarNavigation.setDirectoryButtons(f.getPath());
			} else if (f.isFile()) {
				SimpleUtils.openFile(SearchActivity.this, f);
			}
		}
	};

	private void init() {
		Intent intent = getIntent();

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.show();

		mActionBarNavigation = Browser.getNavigation();

		if (intent != null) {
			mSearchDirectory = intent.getStringExtra("current");
			mQuery = intent.getStringExtra("query");

			if (mQuery != null) {
				mTask = new SearchTask();
				mTask.execute(mQuery);
			}
		}

		SearchIntent(intent);
	}

	// this is needed when phone will be rotated
	private void restart(Bundle savedInstanceState) {
		if (!mData.isEmpty())
			mData.clear();

		for (String data : savedInstanceState.getStringArrayList("foundlist"))
			mData.add(data);

		mAdapter.notifyDataSetChanged();
	}

	private void SearchIntent(Intent intent) {
		setIntent(intent);

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			mQuery = intent.getStringExtra(SearchManager.QUERY);

			if (mQuery.toString().length() > 0) {
				mTask = new SearchTask();
				mTask.execute(mQuery);
			} else {
				return;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);
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
		public ProgressDialog pr_dialog = null;
		private String file_name;

		private SearchTask() {
		}

		@Override
		protected void onPreExecute() {
			pr_dialog = ProgressDialog.show(SearchActivity.this, "",
					getString(R.string.search));
			pr_dialog.setCanceledOnTouchOutside(true);
		}

		@Override
		protected ArrayList<String> doInBackground(String... params) {
			file_name = params[0];

			ArrayList<String> found = SimpleUtils.searchInDirectory(
					mSearchDirectory, file_name);
			return found;
		}

		@Override
		protected void onPostExecute(final ArrayList<String> files) {
			int len = files != null ? files.size() : 0;

			if (len == 0) {
				Toast.makeText(SearchActivity.this, R.string.itcouldntbefound,
						Toast.LENGTH_SHORT).show();
			} else {
				if (!mData.isEmpty())
					mData.clear();

				for (String data : files)
					mData.add(data);

				mAdapter.notifyDataSetChanged();
			}

			pr_dialog.dismiss();
		}
	}

	@Override
	public void onBackPressed() {
		finish();
		return;
	}
}
