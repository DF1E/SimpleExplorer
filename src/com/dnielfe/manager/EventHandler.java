package com.dnielfe.manager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is responsible for handling the information that is displayed from
 * the list view (the files and folder) with a a nested class TableRow. The
 * TableRow class is responsible for displaying which icon is shown for each
 * entry. For example a folder will display the folder icon, a Word doc will
 * display a word icon and so on. If more icons are to be added, the TableRow
 * class must be updated to display those changes.
 */
public class EventHandler {
	private static final int SEARCH_TYPE = 0x00;
	private static final int COPY_TYPE = 0x01;
	private static final int UNZIP_TYPE = 0x02;
	private static final int UNZIPTO_TYPE = 0x03;
	private static final int ZIP_TYPE = 0x04;
	private static final int DELETE_TYPE = 0x05;
	final Context mContext;
	private static int fileCount = 0;
	private final FileOperations mFileMang;
	private Thumbnails mThumbnail;
	TableRow mDelegate;
	private boolean multi_select_flag = false;
	private boolean delete_after_copy = false;
	private boolean thumbnail_flag = true;
	private static final String searchdirectory = "/storage/";
	private int viewmode;
	public static Drawable res;
	// the list used to feed info into the array adapter and when multi-select
	// is on
	ArrayList<String> mDataSource;
	ArrayList<String> mMultiSelectData;

	/**
	 * Creates an EventHandler object. This object is used to communicate most
	 * work from the Main activity to the FileManager class.
	 * 
	 * @param context
	 *            The context of the main activity e.g Main
	 * @param manager
	 *            The FileManager object that was instantiated from Main
	 */
	public EventHandler(Context context, final FileOperations manager) {
		mContext = context;
		mFileMang = manager;

		mDataSource = new ArrayList<String>(
				mFileMang.setHomeDir(Main.startpath));
		// Original
		// mDataSource = new ArrayList<String>(mFileMang.setHomeDir(Environment
		// .getExternalStorageDirectory().getPath()));

	}

	/**
	 * This constructor is called if the user has changed the screen orientation
	 * and does not want the directory to be reset to home.
	 * 
	 * @param context
	 *            The context of the main activity e.g Main
	 * @param manager
	 *            The FileManager object that was instantiated from Main
	 * @param location
	 *            The first directory to display to the user
	 */
	public EventHandler(Context context, final FileOperations manager,
			String location) {
		mContext = context;
		mFileMang = manager;

		mDataSource = new ArrayList<String>(
				mFileMang.getNextDir(location, true));
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
		mDelegate = adapter;
	}

	/**
	 * Set this true and thumbnails will be used as the icon for image files.
	 * False will show a default image.
	 * 
	 * @param show
	 */
	public void setShowThumbnails(boolean show) {
		thumbnail_flag = show;
	}

	// Get Viewmode from Main Activity
	public void setViewMode(int mode) {
		viewmode = mode;
	}

	/**
	 * If you want to move a file (cut/paste) and not just copy/paste use this
	 * method to tell the file manager to delete the old reference of the file.
	 * 
	 * @param delete
	 *            true if you want to move a file, false to copy the file
	 */
	public void setDeleteAfterCopy(boolean delete) {
		delete_after_copy = delete;
	}

	/**
	 * Indicates whether the user wants to select multiple files or folders at a
	 * time. <br>
	 * <br>
	 * false by default
	 * 
	 * @return true if the user has turned on multi selection
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
	 * Will search for a file then display all files with the search parameter
	 * in its name
	 * 
	 * @param name
	 *            the name to search for
	 */
	public void searchForFile(String name) {
		new BackgroundWork(SEARCH_TYPE).execute(name);
	}

	/**
	 * Will delete the file name that is passed on a background thread.
	 * 
	 * @param name
	 */
	public void deleteFile(String name) {
		new BackgroundWork(DELETE_TYPE).execute(name);
	}

	/**
	 * Will copy a file or folder to another location.
	 * 
	 * @param oldLocation
	 *            from location
	 * @param newLocation
	 *            to location
	 */
	public void copyFile(String oldLocation, String newLocation) {
		String[] data = { oldLocation, newLocation };

		new BackgroundWork(COPY_TYPE).execute(data);
	}

	/**
	 * 
	 * @param newLocation
	 */
	public void copyFileMultiSelect(String newLocation) {
		String[] data;
		int index = 1;

		if (mMultiSelectData.size() > 0) {
			data = new String[mMultiSelectData.size() + 1];
			data[0] = newLocation;

			for (String s : mMultiSelectData)
				data[index++] = s;

			new BackgroundWork(COPY_TYPE).execute(data);
		}
	}

	/**
	 * This will extract a zip file to the same directory.
	 * 
	 * @param file
	 *            the zip file name
	 * @param path
	 *            the path were the zip file will be extracted (the current
	 *            directory)
	 */
	public void unZipFile(String file, String path) {
		new BackgroundWork(UNZIP_TYPE).execute(file, path);
	}

	/**
	 * This method will take a zip file and extract it to another location
	 * 
	 * @param name
	 *            the name of the of the new file (the dir name is used)
	 * @param newDir
	 *            the dir where to extract to
	 * @param oldDir
	 *            the dir where the zip file is
	 */
	public void unZipFileToDir(String name, String newDir, String oldDir) {
		new BackgroundWork(UNZIPTO_TYPE).execute(name, newDir, oldDir);
	}

	/**
	 * Creates a zip file
	 * 
	 * @param zipPath
	 *            the path to the directory you want to zip
	 */
	public void zipFile(String zipPath) {
		new BackgroundWork(ZIP_TYPE).execute(zipPath);
	}

	/**
	 * this will stop our background thread that creates thumbnail icons if the
	 * thread is running. this should be stopped when ever we leave the folder
	 * the image files are in.
	 */
	public void stopThumbnailThread() {
		if (mThumbnail != null) {
			mThumbnail.setCancelThumbnails(true);
			mThumbnail = null;
		}
	}

	/**
	 * will return the data in the ArrayList that holds the dir contents.
	 * 
	 * @param position
	 *            the indext of the arraylist holding the dir content
	 * @return the data in the arraylist at position (position)
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

		mDelegate.notifyDataSetChanged();
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
	 * A nested class to handle displaying a custom view in the ListView that is
	 * used in the Main activity. If any icons are to be added, they must be
	 * implemented in the getView method. This class is instantiated once in
	 * Main and has no reason to be instantiated again.
	 */

	public class TableRow extends ArrayAdapter<String> {
		private final int KB = 1024;
		private final int MG = KB * KB;
		private final int GB = MG * KB;
		private String display_size;
		private ArrayList<Integer> positions;

		public TableRow() {
			super(mContext, R.layout.item, mDataSource);
		}

		public void addMultiPosition(int index, String path) {
			if (positions == null)
				positions = new ArrayList<Integer>();

			if (mMultiSelectData == null) {
				positions.add(index);
				add_multiSelect_file(path);

			} else if (mMultiSelectData.contains(path)) {
				if (positions.contains(index))
					positions.remove(new Integer(index));

				mMultiSelectData.remove(path);

			} else {
				positions.add(index);
				add_multiSelect_file(path);
			}

			notifyDataSetChanged();
		}

		/**
		 * This will turn off multi-select and hide the multi-select buttons at
		 * the bottom of the view.
		 * 
		 * @param clearData
		 *            if this is true any files/folders the user selected for
		 *            multi-select will be cleared. If false, the data will be
		 *            kept for later use. Note: multi-select copy and move will
		 *            usually be the only one to pass false, so we can later
		 *            paste it to another folder.
		 */
		public void killMultiSelect(boolean clearData) {
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
			String temp = mFileMang.getCurrentDir();
			File file = new File(temp + "/" + mDataSource.get(position));
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

				mViewHolder.icon.getLayoutParams().height = 65;
				mViewHolder.icon.getLayoutParams().width = 65;

				convertView.setTag(mViewHolder);

			} else {
				mViewHolder = (ViewHolder) convertView.getTag();
			}

			if (viewmode > 0)
				mViewHolder.dateview.setVisibility(TextView.VISIBLE);
			else
				mViewHolder.dateview.setVisibility(TextView.GONE);

			if (positions != null && positions.contains(position))
				mViewHolder.mSelect.setVisibility(ImageView.VISIBLE);
			else
				mViewHolder.mSelect.setVisibility(ImageView.GONE);

			if (mThumbnail == null)
				mThumbnail = new Thumbnails(52, 52);

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
						|| sub_ext.equalsIgnoreCase("m4p")
						|| sub_ext.equalsIgnoreCase("amr")) {

					mViewHolder.icon.setImageResource(R.drawable.music);

				} else if (sub_ext.equalsIgnoreCase("png")
						|| sub_ext.equalsIgnoreCase("jpg")
						|| sub_ext.equalsIgnoreCase("jpeg")
						|| sub_ext.equalsIgnoreCase("gif")
						|| sub_ext.equalsIgnoreCase("psd")
						|| sub_ext.equalsIgnoreCase("raw")
						|| sub_ext.equalsIgnoreCase("tiff")) {

					if (thumbnail_flag && file.length() != 0) {
						Bitmap thumb = mThumbnail
								.isBitmapCached(file.getPath());

						if (thumb == null) {
							final Handler handle = new Handler(
									new Handler.Callback() {
										public boolean handleMessage(Message msg) {
											notifyDataSetChanged();

											return true;
										}
									});

							mThumbnail.createNewThumbnail(mDataSource,
									mFileMang.getCurrentDir(), handle);

							if (!mThumbnail.isAlive())
								mThumbnail.start();

						} else {
							mViewHolder.icon.setImageBitmap(thumb);
						}

					} else {
						mViewHolder.icon.setImageResource(R.drawable.image);
					}

				} else if (sub_ext.equalsIgnoreCase("zip")
						|| sub_ext.equalsIgnoreCase("gzip")
						|| sub_ext.equalsIgnoreCase("tar")
						|| sub_ext.equalsIgnoreCase("gz")) {

					mViewHolder.icon.setImageResource(R.drawable.zip);

				} else if (sub_ext.equalsIgnoreCase("m4v")
						|| sub_ext.equalsIgnoreCase("wmv")
						|| sub_ext.equalsIgnoreCase("3gp")
						|| sub_ext.equalsIgnoreCase("mov")
						|| sub_ext.equalsIgnoreCase("avi")
						|| sub_ext.equalsIgnoreCase("mpg")
						|| sub_ext.equalsIgnoreCase("ogg")
						|| sub_ext.equalsIgnoreCase("flv")
						|| sub_ext.equalsIgnoreCase("mp4")) {

					mViewHolder.icon.setImageResource(R.drawable.movies);

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

					mViewHolder.icon.setImageResource(R.drawable.html32);

				} else if (sub_ext.equalsIgnoreCase("xml")) {
					mViewHolder.icon.setImageResource(R.drawable.xml32);

				} else if (sub_ext.equalsIgnoreCase("conf")) {
					mViewHolder.icon.setImageResource(R.drawable.config32);

				} else if (sub_ext.equalsIgnoreCase("apk")) {

					try {
						// Drawable icon = getApk(file);
						// mViewHolder.icon.setImageDrawable(icon);
						mViewHolder.icon.setImageResource(R.drawable.appicon);
					} catch (Exception e) {
						mViewHolder.icon.setImageResource(R.drawable.appicon);
					}

				} else if (sub_ext.equalsIgnoreCase("jar")) {
					mViewHolder.icon.setImageResource(R.drawable.jar32);

				} else {
					mViewHolder.icon.setImageResource(R.drawable.text);
				}

			} else if (file != null && file.isDirectory()) {
				if (file.canRead() && file.list().length > 0)
					mViewHolder.icon.setImageResource(R.drawable.folder_full);
				else
					mViewHolder.icon.setImageResource(R.drawable.folder);
			}

			if (file.isFile()) {
				double size = file.length();
				if (size > GB)
					setDisplay_size(String.format("%.2f GB ", (double) size
							/ GB));
				else if (size < GB && size > MG)
					setDisplay_size(String.format("%.2f MB ", (double) size
							/ MG));
				else if (size < MG && size > KB)
					setDisplay_size(String.format("%.2f KB ", (double) size
							/ KB));
				else
					setDisplay_size(String.format("%.2f B ", (double) size));

				if (file.isHidden())
					mViewHolder.bottomView.setText(getDisplay_size());
				else
					mViewHolder.bottomView.setText(getDisplay_size());

			} else {
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

		public String getDisplay_size() {
			return display_size;
		}

		public void setDisplay_size(String display_size) {
			this.display_size = display_size;
		}
	}

	/**
	 * A private inner class of EventHandler used to perform time extensive
	 * operations. So the user does not think the the application has hung,
	 * operations such as copy/past, search, unzip and zip will all be performed
	 * in the background. This class extends AsyncTask in order to give the user
	 * a progress dialog to show that the app is working properly.
	 * 
	 * (note): this class will eventually be changed from using AsyncTask to
	 * using Handlers and messages to perform background operations.
	 */
	private class BackgroundWork extends
			AsyncTask<String, Void, ArrayList<String>> {
		private String file_name;
		public ProgressDialog pr_dialog;
		private int type;
		private int copy_rtn;

		private BackgroundWork(int type) {
			this.type = type;
		}

		// This will show a Dialog while Action is running in Background
		@Override
		protected void onPreExecute() {

			switch (type) {
			case SEARCH_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Searching");
				pr_dialog.setCancelable(true);
				break;

			case COPY_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Copying");
				pr_dialog.setCancelable(false);
				break;

			case UNZIP_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Unzipping");
				pr_dialog.setCancelable(false);
				break;

			case UNZIPTO_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Unzipping");
				pr_dialog.setCancelable(false);
				break;

			case ZIP_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Zipping");
				pr_dialog.setCancelable(false);
				break;

			case DELETE_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Deleting");
				pr_dialog.setCancelable(false);
				break;
			}
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {

			switch (type) {
			case SEARCH_TYPE:
				file_name = params[0];
				ArrayList<String> found = mFileMang.searchInDirectory(
						searchdirectory, file_name);
				return found;

			case COPY_TYPE:
				int len = params.length;

				if (mMultiSelectData != null && !mMultiSelectData.isEmpty()) {
					for (int i = 1; i < len; i++) {
						copy_rtn = mFileMang.copyToDirectory(params[i],
								params[0]);

						if (delete_after_copy)
							mFileMang.deleteTarget(params[i]);
					}
				} else {
					copy_rtn = mFileMang.copyToDirectory(params[0], params[1]);

					if (delete_after_copy)
						mFileMang.deleteTarget(params[0]);
				}

				return null;

			case UNZIP_TYPE:
				mFileMang.extractZipFiles(params[0], params[1]);
				return null;

			case UNZIPTO_TYPE:
				mFileMang.extractZipFilesFromDir(params[0], params[1],
						params[2]);
				return null;

			case ZIP_TYPE:
				mFileMang.createZipFile(params[0]);
				return null;

			case DELETE_TYPE:
				int size = params.length;

				for (int i = 0; i < size; i++)
					mFileMang.deleteTarget(params[i]);

				return null;
			}
			return null;
		}

		// This is called when the background thread is finished.
		@Override
		protected void onPostExecute(final ArrayList<String> file) {
			final CharSequence[] names;
			int len = file != null ? file.size() : 0;

			switch (type) {
			case SEARCH_TYPE:
				if (len == 0) {
					Toast.makeText(mContext, R.string.itcouldntbefound,
							Toast.LENGTH_SHORT).show();

				} else {
					names = new CharSequence[len];

					for (int i = 0; i < len; i++) {
						String entry = file.get(i);
						names[i] = entry.substring(entry.lastIndexOf("/") + 1,
								entry.length());
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mContext);
					builder.setTitle(R.string.foundfiles);
					builder.setItems(names,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int position) {
									String path = file.get(position);
									opendir(path.substring(0,
											path.lastIndexOf("/")));
									mDelegate.notifyDataSetChanged();
								}
							});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
				pr_dialog.dismiss();
				break;

			case COPY_TYPE:
				if (mMultiSelectData != null && !mMultiSelectData.isEmpty()) {
					multi_select_flag = false;
					mMultiSelectData.clear();
				}

				if (delete_after_copy) {
					Toast.makeText(mContext, R.string.movesuccsess,
							Toast.LENGTH_SHORT).show();
				}

				else if (copy_rtn == 0)
					Toast.makeText(mContext, R.string.copysuccsess,
							Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext, R.string.copyfail,
							Toast.LENGTH_SHORT).show();

				delete_after_copy = false;
				pr_dialog.dismiss();
				updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(),
						true));
				break;

			case UNZIP_TYPE:
				updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(),
						true));
				pr_dialog.dismiss();
				break;

			case UNZIPTO_TYPE:
				updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(),
						true));
				pr_dialog.dismiss();
				break;

			case ZIP_TYPE:
				updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(),
						true));
				pr_dialog.dismiss();
				break;

			case DELETE_TYPE:
				if (mMultiSelectData != null && !mMultiSelectData.isEmpty()) {
					mMultiSelectData.clear();
					multi_select_flag = false;
				}

				updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(),
						true));
				pr_dialog.dismiss();
				break;
			}
		}
	}

	// Choose Directory Option
	public void opendir(String path) {
		if (multi_select_flag) {
			mDelegate.killMultiSelect(true);
			Toast.makeText(mContext, R.string.multioff, Toast.LENGTH_SHORT)
					.show();
		}

		stopThumbnailThread();
		updateDirectory(mFileMang.setHomeDir(path));
	}

	// Multiselect Delete Action
	public void multiselect() {
		if (multi_select_flag) {
			mDelegate.killMultiSelect(true);

		} else {
			multi_select_flag = true;
		}
	}

	public void multidelete() {
		if (mMultiSelectData == null || mMultiSelectData.isEmpty()) {
			mDelegate.killMultiSelect(true);
		}

		final String[] data = new String[mMultiSelectData.size()];
		int at = 0;

		for (String string : mMultiSelectData)
			data[at++] = string;

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(R.string.delete);
		builder.setMessage(R.string.cannotbeundoneareyousureyouwanttodelete);
		builder.setCancelable(true);
		builder.setPositiveButton((R.string.delete),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new BackgroundWork(DELETE_TYPE).execute(data);
						mDelegate.killMultiSelect(true);
					}
				});
		builder.setNegativeButton((R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mDelegate.killMultiSelect(true);
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	public void multicopy() {
		if (mMultiSelectData == null || mMultiSelectData.isEmpty()) {
			mDelegate.killMultiSelect(true);
		}
		delete_after_copy = false;
		mDelegate.killMultiSelect(false);
		updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(), true));
	}

	public void multimove() {
		if (mMultiSelectData == null || mMultiSelectData.isEmpty()) {
			mDelegate.killMultiSelect(true);
		}
		delete_after_copy = true;
		mDelegate.killMultiSelect(false);
		updateDirectory(mFileMang.getNextDir(mFileMang.getCurrentDir(), true));

	}

	private Drawable getApk(File file2) {
		try {
			String path = mFileMang.getCurrentDir();
			File file = new File(path);
			String[] list = file.list();

			for (String str : list) {
				String not_installed_apk_file = path + "/" + str;
				PackageManager pm = mContext.getPackageManager();
				PackageInfo pi = pm.getPackageArchiveInfo(
						not_installed_apk_file, 0);
				if (pi == null)
					continue;
				// the secret are these two lines....
				pi.applicationInfo.sourceDir = not_installed_apk_file;
				pi.applicationInfo.publicSourceDir = not_installed_apk_file;
				//
				res = pi.applicationInfo.loadIcon(pm);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}
}