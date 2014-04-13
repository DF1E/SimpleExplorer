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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import com.dnielfe.manager.dialogs.DeleteFilesDialog;
import com.dnielfe.manager.tasks.BackupTask;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AppManager extends ThemableActivity {

	private static final String STAR_STATES = "selected_items";
	private boolean[] mStarStates = null;

	private static AppListAdapter mAdapter;
	private static ArrayList<ApplicationInfo> multiSelectData = null;
	private static ArrayList<ApplicationInfo> mAppList = null;
	private static PackageManager mPackMag;

	private static final int ID_LAUNCH = 1;
	private static final int ID_MANAGE = 2;
	private static final int ID_UNINSTALL = 3;
	private static final int ID_SEND = 4;
	private static final int ID_MARKET = 5;

	private static final int FLAG_UPDATED_SYS_APP = 0x80;

	private ActionBar mActionBar;
	private MenuItem mMenuItem;

	private ListView mListView;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_appmanager);

		mPackMag = getPackageManager();
		mActionBar = getActionBar();

		initActionBar();
		init();

		if (savedInstanceState != null) {
			mStarStates = savedInstanceState.getBooleanArray(STAR_STATES);
		} else {
			mStarStates = new boolean[mAppList.size()];
		}
	}

	private void init() {
		mAppList = new ArrayList<ApplicationInfo>();
		multiSelectData = new ArrayList<ApplicationInfo>();

		// new Adapter
		mAdapter = new AppListAdapter(this, mAppList);

		// get user apps
		get_downloaded_apps();

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setOnItemClickListener(mOnItemClickListener);
		mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);

		updateactionbar();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBooleanArray(STAR_STATES, mStarStates);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);

		menu.setHeaderTitle(R.string.options);
		menu.add(0, ID_LAUNCH, 0, getString(R.string.launch));
		menu.add(0, ID_MANAGE, 0, getString(R.string.manage));
		menu.add(0, ID_UNINSTALL, 0, getString(R.string.uninstallapp));
		menu.add(0, ID_MARKET, 0, getString(R.string.playstore));
		menu.add(0, ID_SEND, 0, getString(R.string.share));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int index = info.position;

		switch (item.getItemId()) {
		case ID_LAUNCH:
			Intent i = mPackMag
					.getLaunchIntentForPackage(mAppList.get(index).packageName);
			startActivity(i);
			break;

		case ID_MANAGE:
			startActivity(new Intent(
					android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
					Uri.parse("package:" + mAppList.get(index).packageName)));
			break;

		case ID_UNINSTALL:
			Intent i1 = new Intent(Intent.ACTION_DELETE);
			i1.setData(Uri.parse("package:" + mAppList.get(index).packageName));
			startActivity(i1);
			refreshList();
			break;

		case ID_MARKET:
			Intent intent1 = new Intent(Intent.ACTION_VIEW);
			intent1.setData(Uri.parse("market://details?id="
					+ mAppList.get(index).packageName));
			startActivity(intent1);
			break;

		case ID_SEND:
			try {
				ApplicationInfo info1 = mPackMag.getApplicationInfo(
						mAppList.get(index).packageName, 0);
				String source_dir = info1.sourceDir;
				File file = new File(source_dir);
				Uri uri11 = Uri.fromFile(file.getAbsoluteFile());

				Intent infointent = new Intent(Intent.ACTION_SEND);
				infointent.setType("application/zip");
				infointent.putExtra(Intent.EXTRA_STREAM, uri11);
				startActivity(Intent.createChooser(infointent,
						getString(R.string.share)));
			} catch (Exception e) {
				Toast.makeText(AppManager.this, "Error", Toast.LENGTH_SHORT)
						.show();
			}
			break;
		}
		return false;
	}

	private void initActionBar() {
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setSubtitle(getString(R.string.loading));
		mActionBar.show();
	}

	public void refreshList() {
		mAppList.clear();
		get_downloaded_apps();
		mAdapter.notifyDataSetChanged();
		updateactionbar();
	}

	public void updateactionbar() {
		mActionBar.setSubtitle(mAppList.size() + getString(R.string.apps));
		mActionBar.show();
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.backup_button_all:
			multiSelectData.clear();

			for (int i = 0; i < mAppList.size(); i++) {
				if (mStarStates[i]) {
					multiSelectData.add(mAppList.get(i));
				} else if (!mStarStates[i]) {
					multiSelectData.remove(mAppList.get(i));
				}
			}

			if (multiSelectData.size() > 0 && multiSelectData != null) {
				BackupTask all = new BackupTask(this, mPackMag, multiSelectData);
				all.execute();
			} else {
				Toast.makeText(AppManager.this, getString(R.string.noapps),
						Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			ViewHolder viewHolder = (ViewHolder) v.getTag();

			if (viewHolder.getCheckBox().isChecked()) {
				viewHolder.getCheckBox().setChecked(false);
			} else {
				viewHolder.getCheckBox().setChecked(true);
			}
		}
	};

	private void get_downloaded_apps() {
		List<ApplicationInfo> all_apps = mPackMag
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);

		for (ApplicationInfo appInfo : all_apps) {
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
					&& (appInfo.flags & FLAG_UPDATED_SYS_APP) == 0
					&& appInfo.flags != 0)

				mAppList.add(appInfo);
		}

		// Sorting ListView showing Installed Applications
		Collections.sort(mAppList, new ApplicationInfo.DisplayNameComparator(
				mPackMag));
	}

	private class ViewHolder {
		public ImageView image;
		public CheckBox select;
		public TextView name;
		public TextView version;
		public TextView size;

		ViewHolder(View row) {
			name = (TextView) row.findViewById(R.id.app_name);
			select = (CheckBox) row.findViewById(R.id.select_icon);
			version = (TextView) row.findViewById(R.id.versionlabel);
			size = (TextView) row.findViewById(R.id.installdate);
			image = (ImageView) row.findViewById(R.id.icon);
		}

		public CheckBox getCheckBox() {
			return select;
		}
	}

	private class AppListAdapter extends ArrayAdapter<ApplicationInfo> {
		private ConcurrentHashMap<String, Drawable> mIconCache;
		private ArrayList<ApplicationInfo> mAppList;
		private PackageInfo appinfo;
		private ApplicationInfo info;

		private AppListAdapter(Context context, ArrayList<ApplicationInfo> list) {
			super(context, R.layout.item_appmanager, list);
			this.mAppList = list;

			mIconCache = new ConcurrentHashMap<String, Drawable>();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			info = getItem(position);

			try {
				info = mPackMag.getApplicationInfo(info.packageName, 0);
				appinfo = mPackMag.getPackageInfo(info.packageName, 0);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}

			final String source_dir = info.sourceDir;
			File apkfile = new File(source_dir);
			final long apksize = apkfile.length();

			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.item_appmanager, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			String appname = info.loadLabel(mPackMag).toString();
			String version = appinfo.versionName;

			holder.select.setOnCheckedChangeListener(null);
			holder.select.setChecked(mStarStates[position]);
			holder.select
					.setOnCheckedChangeListener(mStarCheckedChanceChangeListener);

			holder.name.setText(appname);
			holder.version.setText(getString(R.string.version) + version);
			holder.size.setText(FileUtils.byteCountToDisplaySize(apksize));

			// this should not throw the exception
			holder.image.setImageDrawable(getAppIcon(info.packageName));
			return convertView;
		}

		@Override
		public ApplicationInfo getItem(int position) {
			return mAppList.get(position);
		}

		private OnCheckedChangeListener mStarCheckedChanceChangeListener = new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				final int position = mListView.getPositionForView(buttonView);
				if (position != ListView.INVALID_POSITION) {
					mStarStates[position] = isChecked;
				}
			}
		};

		public Drawable getDrawableFromCache(String url) {
			if (mIconCache.containsKey(url)) {
				return mIconCache.get(url);
			}
			return null;
		}

		public Drawable getAppIcon(String packagename) {
			Drawable drawable;
			drawable = getDrawableFromCache(packagename);
			if (drawable != null) {
				return drawable;
			} else {
				try {
					drawable = mPackMag.getApplicationIcon(packagename);
					mIconCache.put(packagename, drawable);

				} catch (NameNotFoundException e) {
					return getResources().getDrawable(R.drawable.type_apk);
				}
				return drawable;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.appmanager, menu);

		mMenuItem = menu.findItem(R.id.actionselect);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.shortcut:
			createshortcut();
			return true;
		case R.id.deleteapps:
			final DialogFragment dialog1 = DeleteFilesDialog
					.instantiate(new String[] { SimpleExplorer.BACKUP_LOC });
			dialog1.show(getFragmentManager(), "dialog");
			return true;
		case R.id.actionselect:
			if (multiSelectData.size() < mAppList.size())
				selectAll();
			else
				unselectAll();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void selectAll() {
		for (int i = 0; i < mAppList.size(); i++) {
			mStarStates[i] = true;
			multiSelectData.add(mAppList.get(i));
		}

		mMenuItem.setTitle(getString(R.string.unselectall));
		refreshList();
	}

	private void unselectAll() {
		for (int i = 0; i < mAppList.size(); i++) {
			mStarStates[i] = false;
		}

		multiSelectData.clear();

		mMenuItem.setTitle(getString(R.string.selectall));
		refreshList();
	}

	private void createshortcut() {
		Intent shortcutIntent = new Intent(AppManager.this, AppManager.class);
		shortcutIntent.setAction(Intent.ACTION_MAIN);

		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.appmanager));
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(AppManager.this,
						R.drawable.type_apk));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		AppManager.this.sendBroadcast(addIntent);

		Toast.makeText(AppManager.this, getString(R.string.shortcutcreated),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		if (mMenuItem.getTitle().toString()
				.equals(getString(R.string.unselectall))) {
			for (int i = 0; i < mAppList.size(); i++) {
				mStarStates[i] = false;
				multiSelectData.remove(mAppList.get(i));
			}
			refreshList();
			mMenuItem.setTitle(getString(R.string.selectall));
		} else {
			finish();
		}
		return;
	}
}