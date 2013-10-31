/*
 * Copyright (C) 2013 Simple Explorer
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import com.dnielfe.utils.IconPreview;

import android.os.AsyncTask;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class is responsible for handling the information that is displayed from
 * the list view (the files and folder) with a a nested class TableRow. The
 * TableRow class is responsible for displaying which icon is shown for each
 * entry. For example a folder will display the folder icon, a Word doc will
 * display a word icon and so on. If more icons are to be added, the TableRow
 * class must be updated to display those changes.
 */
public class EventHandler {
	private static final int SORT_ALPHA = 0;
	private static final int SORT_TYPE = 1;
	private static final int SORT_SIZE = 2;

	private final Context mContext;
	private static int fileCount = 0;
	TableRow mTable;
	boolean multi_select_flag = false;

	private boolean thumbnail = true;
	private int viewmode;
	private boolean mShowHiddenFiles;
	private int mSortType = SORT_TYPE;

	public ArrayList<String> mMultiSelectData;
	public ArrayList<String> mDataSource;
	private Stack<String> mPathStack;

	/**
	 * Creates an EventHandler object. This object is used to communicate most
	 * work from the Main activity to the FileManager class.
	 * 
	 * @param context
	 *            The context of the main activity e.g Main
	 */
	public EventHandler(Context context) {
		mContext = context;

		mPathStack = new Stack<String>();
		mDataSource = new ArrayList<String>();

		initializeDrawbale();
	}

	/**
	 * This method is called from the Main activity and this has the same
	 * reference to the same object so when changes are made here or there they
	 * will display in the same way.
	 * 
	 * @param adapter
	 *            The TableRow object
	 */
	public void setListAdapter(TableRow adapter) {
		mTable = adapter;
	}

	/**
	 * Set this true and thumbnails will be used as the icon for image files.
	 * False will show a default image.
	 */

	public void setShowThumbnails(boolean show) {
		thumbnail = show;
	}

	// Get ViewMode from Main Activity
	public void setViewMode(int mode) {
		viewmode = mode;
	}

	// Get Sort Type int-number from Main
	public void setSortType(int type) {
		mSortType = type;
	}

	// Option to show hidden Files
	public void setShowHiddenFiles(boolean choice) {
		mShowHiddenFiles = choice;
	}

	/**
	 * Indicates whether the user wants to select multiple files or folders at a
	 * time.
	 * 
	 * false by default
	 * 
	 * @return true if the user has turned on multi-selection
	 */
	public boolean isMultiSelected() {
		return multi_select_flag;
	}

	/**
	 * Use this method to determine if the user has selected multiple
	 * files/folders
	 * 
	 * @return returns true if the user is holding multiple objects
	 *         (multi-select)
	 */
	public boolean hasMultiSelectData() {
		return (mMultiSelectData != null && mMultiSelectData.size() > 0);
	}

	/**
	 * will return the data in the ArrayList that holds the dir contents.
	 * 
	 * @param position
	 *            the indext of the arraylist holding the dir content
	 * @return the data in the ArrayList at position (position)
	 */
	public String getData(int position) {

		if (position > mDataSource.size() - 1 || position < 0)
			return null;

		return mDataSource.get(position);
	}

	/**
	 * called to update the file contents as the user navigates there phones
	 * file system.
	 * 
	 * @param content
	 *            an ArrayList of the file/folders in the current directory.
	 */
	public void updateDirectory(ArrayList<String> content) {
		if (!mDataSource.isEmpty())
			mDataSource.clear();

		for (String data : content)
			mDataSource.add(data);

		mTable.notifyDataSetChanged();
	}

	// multi-select
	public void multiselect() {
		if (multi_select_flag) {
			mTable.killMultiSelect(true, true);

		} else {
			multi_select_flag = true;
		}
	}

	// This will return a string of the current directory path
	public String getCurrentDir() {
		return mPathStack.peek();
	}

	// This will return a string of the current home path
	public ArrayList<String> setHomeDir(String path) {
		// This will eventually be placed as a settings item
		mPathStack.clear();
		mPathStack.push("/");
		mPathStack.push(path);

		return update_list();
	}

	// This will return to the previous Directory
	public ArrayList<String> getPreviousDir(String path) {

		File file = new File(path);
		String parent = file.getParent();

		mPathStack.clear();
		mPathStack.push("/");
		mPathStack.push(parent);

		return update_list();
	}

	// Get next Directory
	// return with list
	public ArrayList<String> getNextDir(String path, boolean isFullPath) {
		int size = mPathStack.size();

		if (!path.equals(mPathStack.peek()) && !isFullPath) {
			if (size == 1)
				mPathStack.push("/" + path);
			else
				mPathStack.push(mPathStack.peek() + "/" + path);
		}

		else if (!path.equals(mPathStack.peek()) && isFullPath) {
			mPathStack.push(path);
		}

		return update_list();
	}

	private static class ViewHolder {
		TextView topView;
		TextView bottomView;
		TextView dateview;
		ImageView icon;
		ImageView mSelect; // multi-select check mark icon
	}

	public static int getFileCount(File file) {
		fileCount = 0;
		calculateFileCount(file);
		return fileCount;
	}

	// Calculate number of files in directory
	private static void calculateFileCount(File file) {
		if (!file.isDirectory()) {
			fileCount++;
			return;
		}
		if (file.list() == null) {
			return;
		}
		for (String fileName : file.list()) {
			File f = new File(file.getAbsolutePath() + File.separator
					+ fileName);
			calculateFileCount(f);
		}
	}

	/**
	 * A needed class to handle displaying a custom view in the ListView that is
	 * used in the Main activity. If any icons are to be added, they must be
	 * implemented in the getView method.
	 */

	public class TableRow extends ArrayAdapter<String> {
		private final int KB = 1024;
		private final int MG = KB * KB;
		private final int GB = MG * KB;
		private String display_size;
		ArrayList<Integer> positions;

		public TableRow() {
			super(mContext, R.layout.item, mDataSource);
		}

		public void addMultiPosition(int index, String path, boolean remove) {
			if (positions == null)
				positions = new ArrayList<Integer>();

			if (mMultiSelectData == null) {
				positions.add(index);
				add_multiSelect_file(path);

			} else if (mMultiSelectData.contains(path)) {
				if (remove) {
					if (positions.contains(index))
						positions.remove(new Integer(index));

					mMultiSelectData.remove(path);
				} else {

				}
			} else {
				positions.add(index);
				add_multiSelect_file(path);
			}

			notifyDataSetChanged();
		}

		/**
		 * 
		 * @param clearData
		 *            if this is true any files/folders the user selected for
		 *            multi-select will be cleared. If false, the data will be
		 *            kept for later use.
		 * 
		 * @param disable
		 *            if this is true multiselect will be disabled
		 * 
		 */
		public void killMultiSelect(boolean clearData, boolean disable) {
			if (disable)
				multi_select_flag = false;

			if (positions != null && !positions.isEmpty())
				positions.clear();

			if (clearData)
				if (mMultiSelectData != null && !mMultiSelectData.isEmpty())
					mMultiSelectData.clear();

			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder mViewHolder;
			int num_items = 0;
			String temp = getCurrentDir();
			final File file = new File(temp + "/" + mDataSource.get(position));
			String[] list = file.list();

			if (list != null)
				num_items = list.length;

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.item, parent, false);

				mViewHolder = new ViewHolder();
				mViewHolder.topView = (TextView) convertView
						.findViewById(R.id.top_view);
				mViewHolder.bottomView = (TextView) convertView
						.findViewById(R.id.bottom_view);
				mViewHolder.dateview = (TextView) convertView
						.findViewById(R.id.dateview);
				mViewHolder.icon = (ImageView) convertView
						.findViewById(R.id.row_image);
				mViewHolder.mSelect = (ImageView) convertView
						.findViewById(R.id.multiselect_icon);

				mViewHolder.icon.getLayoutParams().height = 75;
				mViewHolder.icon.getLayoutParams().width = 75;

				convertView.setTag(mViewHolder);

			} else {
				mViewHolder = (ViewHolder) convertView.getTag();
			}

			if (viewmode > 0) {
				mViewHolder.dateview.setVisibility(TextView.VISIBLE);
			} else {
				mViewHolder.dateview.setVisibility(TextView.GONE);
			}

			if (positions != null && positions.contains(position))
				mViewHolder.mSelect.setVisibility(ImageView.VISIBLE);
			else
				mViewHolder.mSelect.setVisibility(ImageView.GONE);

			if (file != null && file.isFile()) {
				String ext = file.toString();
				String sub_ext = ext.substring(ext.lastIndexOf(".") + 1);

				// This series of else if statements will determine which icon
				// is displayed
				if (sub_ext.equalsIgnoreCase("pdf")) {
					mViewHolder.icon.setImageResource(R.drawable.pdf);

				} else if (sub_ext.equalsIgnoreCase("mp3")
						|| sub_ext.equalsIgnoreCase("wma")
						|| sub_ext.equalsIgnoreCase("3ga")
						|| sub_ext.equalsIgnoreCase("m4a")
						|| sub_ext.equalsIgnoreCase("caf")
						|| sub_ext.equalsIgnoreCase("ogg")
						|| sub_ext.equalsIgnoreCase("m4p")
						|| sub_ext.equalsIgnoreCase("aac")
						|| sub_ext.equalsIgnoreCase("wav")
						|| sub_ext.equalsIgnoreCase("amr")) {

					mViewHolder.icon.setImageResource(R.drawable.music);

				} else if (sub_ext.equalsIgnoreCase("png")
						|| sub_ext.equalsIgnoreCase("jpg")
						|| sub_ext.equalsIgnoreCase("jpeg")
						|| sub_ext.equalsIgnoreCase("gif")
						|| sub_ext.equalsIgnoreCase("psd")
						|| sub_ext.equalsIgnoreCase("raw")
						|| sub_ext.equalsIgnoreCase("tiff")) {

					if (thumbnail == true && file.length() != 0) {

						Drawable icon = mContext.getResources().getDrawable(
								R.drawable.bitmap);
						Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
						IconPreview.INSTANCE.setPlaceholder(bitmap);
						mViewHolder.icon.setTag(file.getAbsolutePath());
						IconPreview.INSTANCE.setType("image");

						IconPreview.INSTANCE.loadBitmap(file.getAbsolutePath(),
								mViewHolder.icon);

					} else {
						mViewHolder.icon.setImageResource(R.drawable.image);
					}

				} else if (sub_ext.equalsIgnoreCase("m4v")
						|| sub_ext.equalsIgnoreCase("wmv")
						|| sub_ext.equalsIgnoreCase("3gp")
						|| sub_ext.equalsIgnoreCase("mov")
						|| sub_ext.equalsIgnoreCase("avi")
						|| sub_ext.equalsIgnoreCase("mpg")
						|| sub_ext.equalsIgnoreCase("flv")
						|| sub_ext.equalsIgnoreCase("mp4")) {

					if (thumbnail == true && file.length() != 0) {
						Drawable icon = mContext.getResources().getDrawable(
								R.drawable.bitmap);
						Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
						IconPreview.INSTANCE.setPlaceholder(bitmap);
						mViewHolder.icon.setTag(file.getAbsolutePath());
						IconPreview.INSTANCE.setType("video");

						IconPreview.INSTANCE.loadBitmap(file.getAbsolutePath(),
								mViewHolder.icon);
					} else {
						mViewHolder.icon.setImageResource(R.drawable.movies);
					}

				} else if (sub_ext.equalsIgnoreCase("apk")) {

					if (thumbnail == true && file.length() != 0) {

						new AsyncTask<String[], Long, Long>() {
							Drawable icon;

							@Override
							protected Long doInBackground(String[]... params) {
								icon = getDrawableFromCache(file.getPath());

								if (icon != null) {
									return null;
								} else {
									icon = getApkDrawable(file);
								}

								if (icon == null) {
									icon = mContext.getResources().getDrawable(
											R.drawable.appicon);
								}

								AppIconManager.cache.put(file.getPath(), icon);
								return null;
							}

							@Override
							protected void onPostExecute(Long result) {
								mViewHolder.icon.setImageDrawable(icon);
							}
						}.execute();

					} else {
						mViewHolder.icon.setImageResource(R.drawable.appicon);
					}

				} else if (sub_ext.equalsIgnoreCase("zip")
						|| sub_ext.equalsIgnoreCase("gzip")
						|| sub_ext.equalsIgnoreCase("bzip2")
						|| sub_ext.equalsIgnoreCase("7z")
						|| sub_ext.equalsIgnoreCase("ar")
						|| sub_ext.equalsIgnoreCase("gz")) {

					mViewHolder.icon.setImageResource(R.drawable.zip);

				} else if (sub_ext.equalsIgnoreCase("rar")) {
					mViewHolder.icon.setImageResource(R.drawable.rar);

				} else if (sub_ext.equalsIgnoreCase("tar")) {
					mViewHolder.icon.setImageResource(R.drawable.tar);

				} else if (sub_ext.equalsIgnoreCase("doc")
						|| sub_ext.equalsIgnoreCase("docx")) {

					mViewHolder.icon.setImageResource(R.drawable.word);

				} else if (sub_ext.equalsIgnoreCase("xls")
						|| sub_ext.equalsIgnoreCase("xlsx")) {

					mViewHolder.icon.setImageResource(R.drawable.excel);

				} else if (sub_ext.equalsIgnoreCase("ppt")
						|| sub_ext.equalsIgnoreCase("pptx")) {

					mViewHolder.icon.setImageResource(R.drawable.ppt);

				} else if (sub_ext.equalsIgnoreCase("html")
						|| sub_ext.equalsIgnoreCase("htm")
						|| sub_ext.equalsIgnoreCase("php")) {

					mViewHolder.icon.setImageResource(R.drawable.html);

				} else if (sub_ext.equalsIgnoreCase("xml")) {
					mViewHolder.icon.setImageResource(R.drawable.xml32);

				} else if (sub_ext.equalsIgnoreCase("conf")
						|| sub_ext.equalsIgnoreCase("prop")) {
					mViewHolder.icon.setImageResource(R.drawable.config);

				} else if (sub_ext.equalsIgnoreCase("jar")) {
					mViewHolder.icon.setImageResource(R.drawable.jar32);

				} else if (sub_ext.equalsIgnoreCase("txt")) {
					mViewHolder.icon.setImageResource(R.drawable.text1);

				} else {
					mViewHolder.icon.setImageResource(R.drawable.blanc);
				}

			} else if (file != null && file.isDirectory()) {
				if (file.canRead() && file.list().length > 0)
					mViewHolder.icon.setImageResource(R.drawable.folder_full);
				else
					mViewHolder.icon.setImageResource(R.drawable.folder);
			}

			// Shows the size of File
			if (file.isFile()) {
				double size = file.length();
				display_size = null;

				if (size > GB)
					display_size = String.format("%.2f GB", (double) size / GB);
				else if (size < GB && size > MG)
					display_size = String.format("%.2f MB", (double) size / MG);
				else if (size < MG && size > KB)
					display_size = String.format("%.2f KB", (double) size / KB);
				else
					display_size = String.format("%.2f B", (double) size);

				if (file.isHidden())
					mViewHolder.bottomView.setText(display_size);
				else
					mViewHolder.bottomView.setText(display_size);

			} else {
				// Shows the number of Files in Folder
				String s = mContext.getString(R.string.files);
				if (file.isHidden())
					mViewHolder.bottomView.setText(num_items + s);
				else
					mViewHolder.bottomView.setText(num_items + s);
			}

			SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

			mViewHolder.topView.setText(file.getName());

			mViewHolder.dateview.setText(sdf1.format(file.lastModified()));

			return convertView;
		}

		private void add_multiSelect_file(String src) {
			if (mMultiSelectData == null)
				mMultiSelectData = new ArrayList<String>();

			mMultiSelectData.add(src);
		}
	}

	private static class AppIconManager {
		private static ConcurrentHashMap<String, Drawable> cache;
	}

	private void initializeDrawbale() {
		AppIconManager.cache = new ConcurrentHashMap<String, Drawable>();
	}

	private Drawable getDrawableFromCache(String url) {
		if (AppIconManager.cache.containsKey(url)) {
			return AppIconManager.cache.get(url);
		}
		return null;
	}

	// Sort Comparator sort by alphabet
	@SuppressWarnings("rawtypes")
	private static final Comparator alph = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			return arg0.toLowerCase().compareTo(arg1.toLowerCase());
		}
	};

	// Sort Comparator sort by size
	@SuppressWarnings("rawtypes")
	private final Comparator size = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			String dir = mPathStack.peek();
			Long first = new File(dir + "/" + arg0).length();
			Long second = new File(dir + "/" + arg1).length();

			return first.compareTo(second);
		}
	};

	// Sort Comparator sort by type
	@SuppressWarnings("rawtypes")
	private final Comparator type = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			String ext = null;
			String ext2 = null;
			int ret;

			try {
				ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length())
						.toLowerCase();
				ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length())
						.toLowerCase();

			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
			ret = ext.compareTo(ext2);

			if (ret == 0)
				return arg0.toLowerCase().compareTo(arg1.toLowerCase());

			return ret;
		}
	};

	/*
	 * (non-Javadoc) this function will take the string from the top of the
	 * directory stack and list all files/folders that are in it and return that
	 * list so it can be displayed. Since this function is called every time we
	 * need to update the the list of files to be shown to the user, this is
	 * where we do our sorting (by type, alphabetical, etc).
	 */

	private ArrayList<String> update_list() {

		ArrayList<String> mDirContent = new ArrayList<String>();

		if (!mDirContent.isEmpty())
			mDirContent.clear();

		final File file = new File(mPathStack.peek());

		if (file.exists() && file.canRead()) {
			String[] list = file.list();
			int len = list.length;

			// add files/folder to ArrayList depending on hidden status
			for (int i = 0; i < len; i++) {
				if (!mShowHiddenFiles) {
					if (list[i].toString().charAt(0) != '.')
						mDirContent.add(list[i]);

				} else {
					mDirContent.add(list[i]);
				}
			}

		} else {

			try {
				Process p = Runtime.getRuntime().exec(
						new String[] { "su", "-c",
								"ls -a \"" + file.getAbsolutePath() + "\"" });
				BufferedReader in = new BufferedReader(new InputStreamReader(
						p.getInputStream()));

				String line;
				while ((line = in.readLine()) != null) {

					if (!mShowHiddenFiles) {
						if (line.toString().charAt(0) != '.')
							mDirContent.add(line);
					} else {
						mDirContent.add(line);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		sortType(mDirContent, file.getPath());

		return mDirContent;
	}

	// Show AppIcons of stored Apps in ListView
	private Drawable getApkDrawable(File file) {
		final String filepath = file.getAbsolutePath();

		PackageManager pm = mContext.getPackageManager();
		PackageInfo packageInfo = pm.getPackageArchiveInfo(filepath,
				PackageManager.GET_ACTIVITIES);

		if (packageInfo != null) {

			final ApplicationInfo appInfo = packageInfo.applicationInfo;
			appInfo.sourceDir = filepath;
			appInfo.publicSourceDir = filepath;

			return pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo);
		} else {
			Drawable icon = mContext.getResources().getDrawable(
					R.drawable.appicon);
			return icon;
		}
	}

	@SuppressWarnings("unchecked")
	private ArrayList<String> sortType(ArrayList<String> content, String current) {
		// Set SortType
		switch (mSortType) {

		case SORT_ALPHA:
			Object[] tt = content.toArray();
			content.clear();

			Arrays.sort(tt, alph);

			for (Object a : tt) {
				content.add((String) a);
			}
			break;

		case SORT_SIZE:
			int index = 0;
			Object[] size_ar = content.toArray();

			Arrays.sort(size_ar, size);

			content.clear();
			for (Object a : size_ar) {
				if (new File(current + "/" + (String) a).isDirectory())
					content.add(index++, (String) a);
				else
					content.add((String) a);
			}
			break;

		case SORT_TYPE:
			int dirindex = 0;
			Object[] type_ar = content.toArray();

			Arrays.sort(type_ar, type);
			content.clear();

			for (Object a : type_ar) {
				if (new File(current + "/" + (String) a).isDirectory())
					content.add(dirindex++, (String) a);
				else
					content.add((String) a);
			}
			break;
		}

		return content;
	}
}