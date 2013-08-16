package com.dnielfe.manager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.dnielfe.utils.BitmapLoader;

import android.os.AsyncTask;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
	private static final int COPY_TYPE = 0x01;
	final Context mContext;
	private static int fileCount = 0;
	private final FileOperations mFileMag;
	TableRow mDelegate;
	boolean multi_select_flag = false;
	private boolean delete_after_copy = false;
	private boolean thumbnail = true;
	private int viewmode;
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
		mFileMag = manager;

		mDataSource = new ArrayList<String>(mFileMag.setHomeDir(Main.startpath));
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
		mFileMag = manager;

		mDataSource = new ArrayList<String>(mFileMag.getNextDir(location, true));
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
		thumbnail = show;
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
		ArrayList<Integer> positions;

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
			String temp = mFileMag.getCurrentDir();
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

					if (thumbnail == true && file.length() != 0) {

						Drawable icon = mContext.getResources().getDrawable(
								R.drawable.image);
						Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
						BitmapLoader.INSTANCE.setPlaceholder(bitmap);
						mViewHolder.icon.setTag(file.getAbsolutePath());

						BitmapLoader.INSTANCE.loadBitmap(
								file.getAbsolutePath(), mViewHolder.icon);

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
					mViewHolder.icon.setImageResource(R.drawable.appicon);

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
			case COPY_TYPE:
				pr_dialog = ProgressDialog.show(mContext, "", "Copying");
				pr_dialog.setCancelable(false);
				break;
			}
		}

		// Background thread here
		@Override
		protected ArrayList<String> doInBackground(String... params) {

			switch (type) {
			case COPY_TYPE:
				int len = params.length;

				if (mMultiSelectData != null && !mMultiSelectData.isEmpty()) {
					for (int i = 1; i < len; i++) {
						copy_rtn = mFileMag.copyToDirectory(params[i],
								params[0]);

						if (delete_after_copy)
							mFileMag.deleteTarget(params[i]);
					}
				} else {
					copy_rtn = mFileMag.copyToDirectory(params[0], params[1]);

					if (delete_after_copy)
						mFileMag.deleteTarget(params[0]);
				}
				return null;
			}
			return null;
		}

		// This is called when the background thread is finished.
		@Override
		protected void onPostExecute(final ArrayList<String> file) {
			switch (type) {
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
				updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(),
						true));
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

		updateDirectory(mFileMag.setHomeDir(path));
	}

	// Multiselect Delete Action
	public void multiselect() {
		if (multi_select_flag) {
			mDelegate.killMultiSelect(true);

		} else {
			multi_select_flag = true;
		}
	}

	public void multicopy() {
		if (mMultiSelectData == null || mMultiSelectData.isEmpty()) {
			mDelegate.killMultiSelect(true);
		}
		delete_after_copy = false;
		mDelegate.killMultiSelect(false);
		updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
	}

	public void multimove() {
		if (mMultiSelectData == null || mMultiSelectData.isEmpty()) {
			mDelegate.killMultiSelect(true);
		}
		delete_after_copy = true;
		mDelegate.killMultiSelect(false);
		updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
	}
}