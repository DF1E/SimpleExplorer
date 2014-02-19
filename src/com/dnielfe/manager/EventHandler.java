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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import org.apache.commons.io.FilenameUtils;
import com.dnielfe.manager.preview.DrawableLruCache;
import com.dnielfe.manager.preview.IconPreview;
import com.dnielfe.manager.preview.MimeTypes;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

	public static final String PREF_HIDDEN = "displayhiddenfiles";
	public static final String PREF_PREVIEW = "showpreview";
	public static final String PREFS_SORT = "sort";
	public static final String PREFS_VIEW = "viewmode";

	private final Context mContext;
	private final Resources mResources;
	private static int fileCount = 0;
	static TableRow mTable;
	private SharedPreferences mSettings;
	boolean multi_select_flag = false;

	private boolean thumbnail = true;
	private int viewmode;
	private static boolean mShowHiddenFiles;
	private static int mSortType = SORT_TYPE;

	public ArrayList<String> mMultiSelectData;
	public static ArrayList<String> mDataSource;
	private static Stack<String> mPathStack;

	/**
	 * Creates an EventHandler object. This object is used to communicate most
	 * work from the Main activity to the FileManager class.
	 * 
	 * @param context
	 *            The context of the main activity e.g Main
	 */
	public EventHandler(Context context) {
		this.mContext = context;
		this.mResources = context.getResources();

		mPathStack = new Stack<String>();
		mDataSource = new ArrayList<String>();

		loadPreferences();
	}

	// get shared preferences
	public void loadPreferences() {
		mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
		mShowHiddenFiles = mSettings.getBoolean(PREF_HIDDEN, true);
		thumbnail = mSettings.getBoolean(PREF_PREVIEW, true);
		String value = mSettings.getString(PREFS_SORT, "1");
		String mode = mSettings.getString(PREFS_VIEW, "1");

		mSortType = Integer.parseInt(value);
		viewmode = Integer.parseInt(mode);
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
	 *            the index of the arraylist holding the dir content
	 * @return the data in the ArrayList at position (position)
	 */
	public String getData(int position) {

		if (position > mDataSource.size() - 1 || position < 0)
			return null;

		return mDataSource.get(position);
	}

	public static void refreshDir(String dir) {
		updateDirectory(setHomeDir(dir));
	}

	/**
	 * called to update the file contents as the user navigates there phones
	 * file system.
	 * 
	 * @param content
	 *            an ArrayList of the file/folders in the current directory.
	 */
	public static void updateDirectory(ArrayList<String> content) {
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
	public static String getCurrentDir() {
		return mPathStack.peek();
	}

	// This will return a string of the current home path
	public static ArrayList<String> setHomeDir(String path) {
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

		// multi-select check mark icon
		RelativeLayout mLayout;
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
		private ArrayList<Integer> positions;

		private DrawableLruCache<Integer> mDrawableLruCache;
		private DrawableLruCache<String> mMimeTypeIconCache;

		public TableRow() {
			super(mContext, R.layout.item, mDataSource);

			if (mDrawableLruCache == null) {
				mDrawableLruCache = new DrawableLruCache<Integer>();
			}
			if (mMimeTypeIconCache == null) {
				mMimeTypeIconCache = new DrawableLruCache<String>();
			}
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
				mViewHolder.mLayout = (RelativeLayout) convertView
						.findViewById(R.id.item_layout);

				convertView.setTag(mViewHolder);

			} else {
				mViewHolder = (ViewHolder) convertView.getTag();
			}

			if (viewmode > 0) {
				mViewHolder.dateview.setVisibility(TextView.VISIBLE);
			} else {
				mViewHolder.dateview.setVisibility(TextView.GONE);
			}

			// Change Background Color of an Item if it is multi-selected
			if (positions != null && positions.contains(position))
				mViewHolder.mLayout.setBackgroundResource(R.color.holoblue);
			else
				mViewHolder.mLayout.setBackgroundResource(Color.TRANSPARENT);

			setIcon(file, mViewHolder.icon);

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

			DateFormat dateFormat = android.text.format.DateFormat
					.getDateFormat(mContext);
			DateFormat timeFormat = android.text.format.DateFormat
					.getTimeFormat(mContext);

			mViewHolder.topView.setText(file.getName());

			mViewHolder.dateview.setText(dateFormat.format(file.lastModified())
					+ " " + timeFormat.format(file.lastModified()));

			return convertView;
		}

		private void add_multiSelect_file(String src) {
			if (mMultiSelectData == null)
				mMultiSelectData = new ArrayList<String>();

			mMultiSelectData.add(src);
		}

		protected final void setIcon(final File file, final ImageView icon) {
			final boolean isImage = MimeTypes.isPicture(file);
			final boolean isVideo = MimeTypes.isVideo(file);
			final boolean isApk = file.getName().endsWith(".apk");

			if (file != null && file.isDirectory()) {
				if (file.canRead() && file.list().length > 0)
					icon.setImageResource(R.drawable.folder_full);
				else
					icon.setImageResource(R.drawable.folder);
			} else {
				if (thumbnail) {
					if (isImage) {
						// IconPreview.INSTANCE.setPlaceholder(bitmap);
						icon.setTag(file.getAbsolutePath());
						IconPreview.INSTANCE.loadBitmap(file, icon);
					} else if (isVideo) {
						// IconPreview.INSTANCE.setPlaceholder(bitmap);
						icon.setTag(file.getAbsolutePath());
						IconPreview.INSTANCE.loadBitmap(file, icon);
					} else if (isApk) {
						// IconPreview.INSTANCE.setPlaceholder(bitmap);
						icon.setTag(file.getAbsolutePath());
						IconPreview.INSTANCE.loadApk(file, icon, mContext);
					} else {
						loadFromRes(file, icon);
					}
				} else {
					loadFromRes(file, icon);
				}
			}
		}

		private void loadFromRes(final File file, final ImageView icon) {
			final String fileExt = FilenameUtils.getExtension(file.getName());
			Drawable mimeIcon = mMimeTypeIconCache.get(fileExt);

			if (mimeIcon == null) {
				final int mimeIconId = MimeTypes.getIconForExt(fileExt);
				if (mimeIconId != 0) {
					mimeIcon = mResources.getDrawable(mimeIconId);
					mMimeTypeIconCache.put(fileExt, mimeIcon);
				}
			}

			if (mimeIcon != null) {
				icon.setImageDrawable(mimeIcon);
			} else {
				// default icon
				icon.setImageResource(R.drawable.blanc);
			}
		}
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
	private final static Comparator size = new Comparator<String>() {
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
	private final static Comparator type = new Comparator<String>() {
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

	private static ArrayList<String> update_list() {

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

	@SuppressWarnings("unchecked")
	private static ArrayList<String> sortType(ArrayList<String> content,
			String current) {
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