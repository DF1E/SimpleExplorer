package com.dnielfe.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AppManager extends ListActivity {

	private static final String STAR_STATES = "mylist:star_states";
	private boolean[] mStarStates = null;

	private static String BACKUP_LOC = Environment
			.getExternalStorageDirectory().getPath() + "/Simple Explorer/Apps/";
	private static final int SET_PROGRESS = 0x00;
	private static final int FINISH_PROGRESS = 0x01;
	private static final int FLAG_UPDATED_SYS_APP = 0x80;
	private static final String[] Q = new String[] { "B", "KB", "MB", "GB",
			"TB", "PB", "EB" };

	private static ArrayList<ApplicationInfo> mAppList = null;
	private static PackageManager mPackMag = null;
	private static ProgressDialog mDialog = null;
	private static PackageInfo appinfo = null;
	private static ApplicationInfo apkinfo = null;

	private static final int ID_LAUNCH = 1;
	private static final int ID_MANAGE = 2;
	private static final int ID_UNINSTALL = 3;
	private static final int ID_BACKUP = 4;
	private static final int ID_SEND = 5;
	private static final int ID_MARKET = 6;

	private static final int BUFFER = 1024;
	private static ArrayList<ApplicationInfo> multiSelectData = null;

	private MenuItem mMenuItem;

	// Our handler object that will update the GUI from our background thread.
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case SET_PROGRESS:
				mDialog.setMessage((String) msg.obj);
				break;
			case FINISH_PROGRESS:
				mDialog.cancel();
				Toast.makeText(AppManager.this,
						getString(R.string.backupcomplete), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initializeDrawbale();
		setContentView(R.layout.appmanager);

		findViewById(R.id.backup_button_all);

		mAppList = new ArrayList<ApplicationInfo>();
		multiSelectData = new ArrayList<ApplicationInfo>();

		mPackMag = getPackageManager();
		registerForContextMenu(getListView());

		new AsyncTask<String[], Long, Long>() {

			@Override
			protected void onPreExecute() {
				ActionBar actionBar = getActionBar();
				actionBar.setSubtitle(getString(R.string.loading));
				actionBar.show();
				getOverflowMenu();
			}

			@Override
			protected Long doInBackground(String[]... params) {
				File dir = new File(BACKUP_LOC);

				if (!dir.exists())
					dir.mkdirs();

				get_downloaded_apps();
				return null;
			}

			@Override
			protected void onPostExecute(Long result) {

				setListAdapter(new TableView());

				if (savedInstanceState != null) {
					mStarStates = savedInstanceState
							.getBooleanArray(STAR_STATES);
				} else {
					mStarStates = new boolean[mAppList.size()];
				}
				updateactionbar();
				// This enable fast-scroll divider
				if (mAppList.size() > 40) {
					getListView().setFastScrollEnabled(true);
				} else {
					getListView().setFastScrollEnabled(false);
				}
			}
		}.execute();

		ActionBar actionBar = getActionBar();
		actionBar.show();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	public void updateactionbar() {
		ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(mAppList.size() + getString(R.string.apps));
		actionBar.show();
	}

	// with this you get the 3 dot menu button
	private void getOverflowMenu() {

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		menu.add(0, ID_BACKUP, 0, getString(R.string.singlebackup));
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

		case ID_BACKUP:
			backupApp(mAppList.get(index).packageName);
			Toast.makeText(AppManager.this, getString(R.string.backupcomplete),
					Toast.LENGTH_SHORT).show();
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
			if (multiSelectData.size() > 0 && multiSelectData != null) {

				int length = multiSelectData.size();
				Intent intent1 = new Intent(Intent.ACTION_VIEW);
				for (int j = 0; j < length; j++) {

					intent1.setData(Uri.parse("market://details?id="
							+ multiSelectData.get(j).packageName));
					startActivity(intent1);
				}
			} else {
				Intent intent1 = new Intent(Intent.ACTION_VIEW);
				intent1.setData(Uri.parse("market://details?id="
						+ mAppList.get(index).packageName));
				startActivity(intent1);
			}
			break;

		case ID_SEND:
			if (multiSelectData.size() > 0 && multiSelectData != null) {

				ArrayList<Uri> uris = new ArrayList<Uri>();
				int length = multiSelectData.size();
				Intent send_intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
				send_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

				send_intent.setType("image/jpeg");
				for (int j = 0; j < length; j++) {

					File file = new File(multiSelectData.get(j).sourceDir)
							.getAbsoluteFile();
					uris.add(Uri.fromFile(file));
				}
				send_intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
						uris);
				startActivity(Intent.createChooser(send_intent,
						getString(R.string.share)));

			} else {
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
			}
			break;
		}
		return false;
	}

	@SuppressWarnings("unused")
	private Intent createShareIntent() {
		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, "Shared from the ActionBar widget.");
		return Intent.createChooser(intent, "Share");
	}

	public void refreshList() {
		mAppList.clear();
		get_downloaded_apps();
		setListAdapter(new TableView());
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
				mDialog = ProgressDialog.show(AppManager.this,
						getString(R.string.backup), "", true, false);

				Thread all = new Thread(new BackgroundWork(multiSelectData));
				all.start();
			} else {
				Toast.makeText(AppManager.this, getString(R.string.noapps),
						Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position,
			long id) {
		ViewHolder viewHolder = (ViewHolder) v.getTag();

		if (viewHolder.getCheckBox().isChecked()) {
			viewHolder.getCheckBox().setChecked(false);
		} else {
			viewHolder.getCheckBox().setChecked(true);
		}
	}

	private void backupApp(String pkgname) {

		try {
			ApplicationInfo info = mPackMag.getApplicationInfo(pkgname, 0);
			String source_dir = info.sourceDir;

			String out_file = info.loadLabel(mPackMag).toString() + ".apk";
			BufferedInputStream mBuffIn;
			BufferedOutputStream mBuffOut;

			int read;
			File mDir = new File(BACKUP_LOC);
			byte[] mData;

			mData = new byte[BUFFER];

			// create dir if needed
			File d = new File(BACKUP_LOC);
			if (!d.exists()) {
				d.mkdir();

				// then create this directory
				mDir.mkdir();

			} else {
				if (!mDir.exists())
					mDir.mkdir();
			}

			try {
				mBuffIn = new BufferedInputStream(new FileInputStream(
						source_dir));
				mBuffOut = new BufferedOutputStream(new FileOutputStream(
						BACKUP_LOC + out_file));

				while ((read = mBuffIn.read(mData, 0, BUFFER)) != -1)
					mBuffOut.write(mData, 0, read);

				mBuffOut.flush();
				mBuffIn.close();
				mBuffOut.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Toast.makeText(AppManager.this,
						getString(R.string.backuperror), Toast.LENGTH_SHORT)
						.show();
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(AppManager.this,
						getString(R.string.backuperror), Toast.LENGTH_SHORT)
						.show();
			}

		} catch (NameNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(AppManager.this, getString(R.string.backuperror),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void get_downloaded_apps() {
		List<ApplicationInfo> all_apps = mPackMag
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);

		for (ApplicationInfo appInfo : all_apps) {
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
					&& (appInfo.flags & FLAG_UPDATED_SYS_APP) == 0
					&& appInfo.flags != 0)

				mAppList.add(appInfo);
		}
	}

	/*
	 * This private inner class will perform the backup of applications on a
	 * background thread, while updating the user via a message being sent to
	 * our handler object.
	 */
	private class BackgroundWork implements Runnable {

		private ArrayList<ApplicationInfo> mDataSource;
		private File mDir = new File(BACKUP_LOC);
		private byte[] mData;

		public BackgroundWork(ArrayList<ApplicationInfo> data) {
			mDataSource = data;
			mData = new byte[BUFFER];

			// create dir if needed
			File d = new File(BACKUP_LOC);
			if (!d.exists()) {
				d.mkdir();

				// then create this directory
				mDir.mkdir();

			} else {
				if (!mDir.exists())
					mDir.mkdir();
			}
		}

		public void run() {
			BufferedInputStream mBuffIn;
			BufferedOutputStream mBuffOut;
			Message msg;
			int len = mDataSource.size();
			int read;

			for (int i = 0; i < len; i++) {
				ApplicationInfo info = mDataSource.get(i);
				String source_dir = info.sourceDir;
				String out_file = info.loadLabel(mPackMag).toString() + ".apk";
				try {
					mBuffIn = new BufferedInputStream(new FileInputStream(
							source_dir));
					mBuffOut = new BufferedOutputStream(new FileOutputStream(
							BACKUP_LOC + out_file));

					while ((read = mBuffIn.read(mData, 0, BUFFER)) != -1)
						mBuffOut.write(mData, 0, read);

					mBuffOut.flush();
					mBuffIn.close();
					mBuffOut.close();

					msg = new Message();
					msg.what = SET_PROGRESS;
					msg.obj = i + getString(R.string.of) + len
							+ getString(R.string.backedup);
					mHandler.sendMessage(msg);

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (quit.mback == -1)
					break;
			}

			mHandler.sendEmptyMessage(FINISH_PROGRESS);
		}
	}

	public String getAsString(long bytes) {
		for (int i = 6; i > 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.1f %s", bytes / step, Q[i]);
		}
		return Long.toString(bytes);
	}

	class ViewHolder {
		public ImageView image = null;
		public CheckBox select = null;
		public TextView name = null;
		public TextView version = null;
		public TextView size = null;

		ViewHolder(View row) {
			name = (TextView) row.findViewById(R.id.app_name);
			select = (CheckBox) row.findViewById(R.id.select_icon);
			version = (TextView) row.findViewById(R.id.versionlabel);
			size = (TextView) row.findViewById(R.id.installdate);
			image = (ImageView) row.findViewById(R.id.icon);
		}

		void populateFrom(String s) {
			name.setText(s);
		}

		public CheckBox getCheckBox() {
			return select;
		}
	}

	private class TableView extends ArrayAdapter<ApplicationInfo> {

		private TableView() {
			super(AppManager.this, R.layout.appmanagerrow, mAppList);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			ApplicationInfo info = mAppList.get(position);
			String appname = info.loadLabel(mPackMag).toString();

			try {
				apkinfo = mPackMag.getApplicationInfo(
						mAppList.get(position).packageName, 0);

			} catch (NameNotFoundException e2) {
				e2.printStackTrace();
			}

			final String source_dir = apkinfo.sourceDir;
			File apkfile = new File(source_dir);
			final long apksize = apkfile.length();
			String apk_size = getAsString(apksize);

			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				convertView = inflater.inflate(R.layout.appmanagerrow, null);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			try {
				appinfo = mPackMag.getPackageInfo(info.packageName, 0);

			} catch (NameNotFoundException e1) {
				e1.printStackTrace();
			}

			String version = appinfo.versionName;

			holder.select.setOnCheckedChangeListener(null);
			holder.select.setChecked(mStarStates[position]);
			holder.select
					.setOnCheckedChangeListener(mStarCheckedChanceChangeListener);

			holder.name.setText(appname);
			holder.version.setText(getString(R.string.version) + version);
			holder.size.setText(String.valueOf(apk_size));

			// this should not throw the exception
			holder.image.setImageDrawable(getAppIcon(info.packageName));
			return convertView;
		}
	}

	private OnCheckedChangeListener mStarCheckedChanceChangeListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			final int position = getListView().getPositionForView(buttonView);
			if (position != ListView.INVALID_POSITION) {
				mStarStates[position] = isChecked;
			}
		}
	};

	public static class quit {
		public static int mback = 0;
	}

	private void initializequit() {
		quit.mback = -1;
	}

	public static class AppIconManager {
		private static ConcurrentHashMap<String, Drawable> cache;
	}

	private void initializeDrawbale() {
		AppIconManager.cache = new ConcurrentHashMap<String, Drawable>();
	}

	public Drawable getDrawableFromCache(String url) {
		if (AppIconManager.cache.containsKey(url)) {
			return AppIconManager.cache.get(url);
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
				AppIconManager.cache.put(packagename, drawable);

			} catch (NameNotFoundException e) {
				return getResources().getDrawable(R.drawable.appicon);
			}
			return drawable;
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
	public boolean onOptionsItemSelected(MenuItem menu) {
		switch (menu.getItemId()) {

		case android.R.id.home:
			finish();
			return true;

		case R.id.shortcut:
			createshortcut();
			return true;

		case R.id.deleteapps:
			deleteapps();
			return true;

		case R.id.actionselect:
			if (mMenuItem.getTitle().toString()
					.equals(getString(R.string.selectall))) {
				for (int i = 0; i < mAppList.size(); i++) {
					mStarStates[i] = true;
					multiSelectData.add(mAppList.get(i));
				}
				refreshList();
				mMenuItem.setTitle(getString(R.string.unselectall));
			} else {

				for (int i = 0; i < mAppList.size(); i++) {
					mStarStates[i] = false;
					multiSelectData.remove(mAppList.get(i));
				}
				refreshList();
				mMenuItem.setTitle(getString(R.string.selectall));
			}
			break;
		}
		return false;
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
						R.drawable.appmanager));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		AppManager.this.sendBroadcast(addIntent);

		Toast.makeText(AppManager.this, getString(R.string.shortcutcreated),
				Toast.LENGTH_SHORT).show();
	}

	private void deleteapps() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.deleteapps);
		builder.setMessage(R.string.deleteappsmsg);
		builder.setCancelable(true);
		builder.setPositiveButton((R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						delete();
					}
				});
		builder.setNegativeButton((R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private void delete() {
		new AsyncTask<String[], Long, Long>() {
			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				dialog = ProgressDialog.show(AppManager.this, "",
						getString(R.string.deleting));
				dialog.setCancelable(true);
			}

			@Override
			protected Long doInBackground(String[]... params) {
				File folder = new File(BACKUP_LOC);

				String[] children = folder.list();
				for (int i = 0; i < children.length; i++) {
					new File(folder, children[i]).delete();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Long result) {
				Toast.makeText(AppManager.this,
						getString(R.string.appsdeleted), Toast.LENGTH_SHORT)
						.show();

				dialog.cancel();
			}
		}.execute();
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
			initializequit();
			finish();
		}
		return;
	}
}