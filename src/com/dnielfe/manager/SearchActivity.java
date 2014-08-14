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
import com.dnielfe.manager.utils.ActionBarNavigation;
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
import android.widget.AdapterView.OnItemClickListener;

public class SearchActivity extends ThemableActivity {

	private ActionBarNavigation mActionBarNavigation;

	public static String mQuery;
	private static String mDirectory;

	private ActionBar mActionBar;
	private ListView mListView;
	private SearchTask mTask;
	private ArrayList<String> mData;
	private BrowserListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);

		init(getIntent());
		initList();

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

	private void init(Intent intent) {
		mActionBar = getActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.show();

		mActionBarNavigation = Browser.getNavigation();
		mDirectory = Browser.mCurrentPath;
		SearchIntent(intent);
	}

	private void initList() {
		mData = new ArrayList<String>();
		mAdapter = new BrowserListAdapter(this, mData);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mOnItemClickListener);
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Object filepath = mListView.getAdapter().getItem(position);
			File f = new File(filepath.toString());

			if (f.isDirectory()) {
				finish();
				Browser.listDirectory(f.getPath());
				mActionBarNavigation.setDirectoryButtons(f.getPath());
			} else if (f.isFile()) {
				SimpleUtils.openFile(SearchActivity.this, f);
			}
		}
	};

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
		public ProgressDialog pr_dialog = null;
		private String file_name;
		private Context context;

		private SearchTask(Context c) {
			context = c;
		}

		@Override
		protected void onPreExecute() {
			pr_dialog = ProgressDialog.show(context, null,
					getString(R.string.search));
			pr_dialog.setCanceledOnTouchOutside(true);
		}

		@Override
		protected ArrayList<String> doInBackground(String... params) {
			file_name = params[0];

			ArrayList<String> found = SimpleUtils.searchInDirectory(mDirectory,
					file_name);
			return found;
		}

		@Override
		protected void onPostExecute(final ArrayList<String> files) {
			int len = files != null ? files.size() : 0;

			if (!mData.isEmpty())
				mData.clear();

			pr_dialog.dismiss();

			if (len == 0) {
				Toast.makeText(context, R.string.itcouldntbefound,
						Toast.LENGTH_SHORT).show();
				mActionBar.setSubtitle(null);
			} else {
				for (String data : files)
					mData.add(data);

				mActionBar.setSubtitle(String.valueOf(len)
						+ getString(R.string._files));
			}

			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}
