package com.dnielfe.manager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Toast;

public final class Main extends ListActivity implements
		OnSharedPreferenceChangeListener {

	private static final String PREFS_THUMBNAIL = "thumbnail";
	private static final String PREF_HIDDEN = "displayhiddenfiles";
	public static final String PREF_PREVIEW = "showpreview";
	public static final String PREFS_SORT = "sort";
	public static final String PREFS_VIEW = "viewmode";

	private static final int D_MENU_BOOKMARK = 4;
	private static final int F_MENU_OPENAS = 5;
	private static final int D_MENU_DELETE = 6;
	private static final int D_MENU_RENAME = 7;
	private static final int D_MENU_COPY = 8;
	private static final int D_MENU_PASTE = 9;
	private static final int D_MENU_ZIP = 10;
	private static final int D_MENU_UNZIP = 11;
	private static final int D_MENU_MOVE = 12;
	private static final int F_MENU_BOOKMARK = 13;
	private static final int F_MENU_MOVE = 14;
	private static final int F_MENU_DELETE = 15;
	private static final int F_MENU_RENAME = 16;
	private static final int F_MENU_ATTACH = 17;
	private static final int F_MENU_COPY = 18;
	private static final int F_MENU_DETAILS = 19;
	private static final int D_MENU_DETAILS = 20;
	private SharedPreferences mSettings;
	private static FileOperations mFileMag;
	private static EventHandler mHandler;
	private static EventHandler.TableRow mTable;
	private boolean mReturnIntent = false;
	private boolean mHoldingFile = false;
	private boolean mHoldingZip = false;
	private boolean mUseBackKey = true;
	private static String mCopiedTarget;
	private static String mZippedTarget;
	private static String mSelectedListItem;
	private TextView mDetailLabel;
	static boolean hidden;
	static boolean preview;
	static int viewm;
	static LinearLayout mDirectoryButtons;
	static ActionMode mActionMode;
	static int directorytextsize = 16;
	private final int KB = 1024;
	private final int MG = KB * KB;
	private final int GB = MG * KB;
	private String display_size;
	static final String[] Q = new String[] { "B", "KB", "MB", "GB", "T", "P",
			"E" };
	static String startpath = Environment.getExternalStorageDirectory()
			.getPath();

	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		// read settings
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mSettings.registerOnSharedPreferenceChangeListener(this);
		hidden = mSettings.getBoolean(PREF_HIDDEN, false);
		boolean thumb = mSettings.getBoolean(PREF_PREVIEW, false);
		String value = mSettings.getString("sort", "1");
		int sort = Integer.parseInt(value);
		String viewmode = mSettings.getString("viewmode", "1");
		viewm = Integer.parseInt(viewmode);

		mFileMag = new FileOperations();
		mFileMag.setSortType(sort);

		if (savedInstanceState != null)
			mHandler = new EventHandler(Main.this, mFileMag,
					savedInstanceState.getString("location"));
		else
			mHandler = new EventHandler(Main.this, mFileMag);

		mHandler.setShowThumbnails(thumb);
		mTable = mHandler.new TableRow();

		// ActionBar Settings
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		// actionBar.setBackgroundDrawable(new ColorDrawable(COLOR.BLUE));

		// ActionBar Layout
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.actionbar, null);
		actionBar.setCustomView(v);
		actionBar.show();

		// Set List Adapter
		mHandler.setListAdapter(mTable);
		setListAdapter(mTable);

		// register context menu for our list view
		registerForContextMenu(getListView());

		// Image Button ActionBar Search
		ImageButton mainButton = (ImageButton) findViewById(R.id.actionsearch);
		mainButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				searchaction();
			}
		});

		Intent intent3 = getIntent();
		SearchIntent(intent3);

		mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		HorizontalScrollView scrolltext = (HorizontalScrollView) findViewById(R.id.scroll_text);

		checkEnvironment();
		setDirectoryButtons();
		displayFreeSpace();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(),
				true));
		displayFreeSpace();
		setDirectoryButtons();
	}

	public void searchaction() {
		this.onSearchRequested();
	}

	public void listview() {
		ListView listview = this.getListView();
		listview.setSelection(0);
	}

	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		SearchIntent(intent);
	}

	public void SearchIntent(Intent intent) {
		setIntent(intent);

		// search action
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query.length() > 0)
				mHandler.searchForFile(query);
		}
	}

	private void checkEnvironment() {

		File f = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (sdCardExist) {
			f = Environment.getExternalStorageDirectory();
			if (f != null) {
				startpath = f.getAbsolutePath();
			}

		} else {
			Toast.makeText(Main.this, getString(R.string.sdcardnotfound),
					Toast.LENGTH_LONG).show();
		}
	}

	public File currentDirectory() {
		return new File(mFileMag.getCurrentDir());
	}

	public void setDirectoryButtons() {
		HorizontalScrollView scrolltext = (HorizontalScrollView) findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) findViewById(R.id.directory_buttons);
		mDirectoryButtons.removeAllViews();

		String[] parts = currentDirectory().getAbsolutePath().split("/");

		int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;
		int MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT;

		// Add home button separately
		Button bt = new Button(this);
		bt.setText("/");
		bt.setTextSize(directorytextsize);
		bt.setBackgroundResource(R.drawable.buttonaction);
		bt.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
				WRAP_CONTENT, Gravity.CENTER_VERTICAL));
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				String rootsystem = "/";
				mHandler.opendir(rootsystem);
				setDirectoryButtons();
			}
		});

		mDirectoryButtons.addView(bt);
		FrameLayout fv = new FrameLayout(this);
		fv.setBackground(getResources().getDrawable(R.drawable.listmore));
		fv.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
				MATCH_PARENT, Gravity.CENTER_VERTICAL));

		mDirectoryButtons.addView(fv);

		// Add other buttons
		String dir = "";

		for (int i = 1; i < parts.length; i++) {
			dir += "/" + parts[i];

			FrameLayout fv1 = new FrameLayout(this);
			fv1.setBackground(getResources().getDrawable(R.drawable.listmore));
			fv1.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT,
					MATCH_PARENT, Gravity.CENTER_VERTICAL));
			Button b = new Button(this);
			b.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT,
					WRAP_CONTENT, Gravity.CENTER_VERTICAL));
			b.setBackgroundResource(R.drawable.buttonaction);
			b.setText(parts[i]);
			b.setTextSize(directorytextsize);
			b.setTag(dir);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					String dir1 = (String) view.getTag();

					mHandler.stopThumbnailThread();
					mHandler.updateDirectory(mFileMag.setHomeDir(dir1));
					setDirectoryButtons();
				}
			});

			mDirectoryButtons.addView(b);
			mDirectoryButtons.addView(fv1);
			scrolltext.postDelayed(new Runnable() {
				public void run() {
					HorizontalScrollView hv = (HorizontalScrollView) findViewById(R.id.scroll_text);
					hv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
				}
			}, 100L);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("location", mFileMag.getCurrentDir());
	}

	/*
	 * Returns the file that was selected to the intent that called this
	 * activity. usually from the caller is another application.
	 */

	private void returnIntentResults(File data) {
		mReturnIntent = false;

		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		finish();
	}

	public String getAsString(long bytes) {
		for (int i = 6; i >= 0; i--) {
			double step = Math.pow(1024, i);
			if (bytes > step)
				return String.format("%3.2f %s", bytes / step, Q[i]);
		}
		return Long.toString(bytes);
	}

	public static String formatSize(Context context, long sizeInBytes) {
		return Formatter.formatFileSize(context, sizeInBytes);
	}

	@Override
	public void onListItemClick(ListView parent, View view, int position,
			long id) {
		final String item = mHandler.getData(position);
		boolean multiSelect = mHandler.isMultiSelected();
		File file = new File(mFileMag.getCurrentDir() + "/" + item);
		String item_ext = null;

		try {
			item_ext = item.substring(item.lastIndexOf("."), item.length());

		} catch (IndexOutOfBoundsException e) {
			item_ext = "";
		}

		// If the user has multi-select on, we just need to record the file not
		// make an intent for it.

		if (multiSelect) {
			mTable.addMultiPosition(position, file.getPath());

		} else {
			if (file.isDirectory()) {
				if (file.canRead()) {
					mHandler.stopThumbnailThread();
					mHandler.updateDirectory(mFileMag.getNextDir(item, false));
					displayFreeSpace();
					setDirectoryButtons();
					listview();

					if (!mUseBackKey)
						mUseBackKey = true;

				} else {
					Toast.makeText(this,
							getString(R.string.cantreadfolderduetopermissions),
							Toast.LENGTH_SHORT).show();
				}
			}

			// music file selected--add more audio formats
			else if (item_ext.equalsIgnoreCase(".mp3")
					|| item_ext.equalsIgnoreCase(".m4a")
					|| item_ext.equalsIgnoreCase(".aiff")
					|| item_ext.equalsIgnoreCase(".wma")
					|| item_ext.equalsIgnoreCase(".caf")
					|| item_ext.equalsIgnoreCase(".flac")
					|| item_ext.equalsIgnoreCase(".mp4")
					|| item_ext.equalsIgnoreCase("m4p")
					|| item_ext.equalsIgnoreCase("amr")) {

				if (mReturnIntent) {
					returnIntentResults(file);
				} else {
					Intent i = new Intent();
					i.setAction(android.content.Intent.ACTION_VIEW);
					i.setDataAndType(Uri.fromFile(file), "audio/*");
					startActivity(i);
				}
			}

			// photo file selected
			else if (item_ext.equalsIgnoreCase(".jpeg")
					|| item_ext.equalsIgnoreCase(".jpg")
					|| item_ext.equalsIgnoreCase(".png")
					|| item_ext.equalsIgnoreCase(".gif")
					|| item_ext.equalsIgnoreCase(".raw")
					|| item_ext.equalsIgnoreCase(".psd")
					|| item_ext.equalsIgnoreCase(".bmp")
					|| item_ext.equalsIgnoreCase(".tiff")
					|| item_ext.equalsIgnoreCase(".tif")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent picIntent = new Intent();
						picIntent.setAction(android.content.Intent.ACTION_VIEW);
						picIntent.setDataAndType(Uri.fromFile(file), "image/*");
						startActivity(picIntent);
					}
				}
			}

			// video file selected--add more video formats
			else if (item_ext.equalsIgnoreCase(".m4v")
					|| item_ext.equalsIgnoreCase(".3gp")
					|| item_ext.equalsIgnoreCase(".wmv")
					|| item_ext.equalsIgnoreCase(".mp4")
					|| item_ext.equalsIgnoreCase(".mpeg")
					|| item_ext.equalsIgnoreCase(".mpg")
					|| item_ext.equalsIgnoreCase(".rm")
					|| item_ext.equalsIgnoreCase(".mov")
					|| item_ext.equalsIgnoreCase(".avi")
					|| item_ext.equalsIgnoreCase(".flv")
					|| item_ext.equalsIgnoreCase(".ogg")
					|| item_ext.equalsIgnoreCase(".wav")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent movieIntent = new Intent();
						movieIntent
								.setAction(android.content.Intent.ACTION_VIEW);
						movieIntent.setDataAndType(Uri.fromFile(file),
								"video/*");
						startActivity(movieIntent);
					}
				}
			}

			// zip file
			else if (item_ext.equalsIgnoreCase(".zip")) {

				if (mReturnIntent) {
					returnIntentResults(file);

				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					AlertDialog alert;
					mZippedTarget = mFileMag.getCurrentDir() + "/" + item;
					CharSequence[] option = { getString(R.string.extracthere) };

					builder.setTitle(getString(R.string.options));
					builder.setItems(option,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case 0:
										String dir = mFileMag.getCurrentDir();
										mHandler.unZipFile(item, dir + "/");
										break;
									}
								}
							});

					alert = builder.create();
					alert.show();
				}
			}

			// gzip files, this will be implemented later
			else if (item_ext.equalsIgnoreCase(".gzip")
					|| item_ext.equalsIgnoreCase(".rar")
					|| item_ext.equalsIgnoreCase(".tar")
					|| item_ext.equalsIgnoreCase(".tar.gz")
					|| item_ext.equalsIgnoreCase(".gz")) {

				if (mReturnIntent) {
					returnIntentResults(file);

				} else {
				}
			}

			// PDF file selected
			else if (item_ext.equalsIgnoreCase(".pdf")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent pdfIntent = new Intent();
						pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
						pdfIntent.setDataAndType(Uri.fromFile(file),
								"application/pdf");

						try {
							startActivity(pdfIntent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldntfindapdfviewver),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

			// Android Application file
			else if (item_ext.equalsIgnoreCase(".apk")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent apkIntent = new Intent();
						apkIntent.setAction(android.content.Intent.ACTION_VIEW);
						apkIntent.setDataAndType(Uri.fromFile(file),
								"application/vnd.android.package-archive");
						startActivity(apkIntent);
					}
				}
			}

			// HTML file
			else if (item_ext.equalsIgnoreCase(".html")
					|| item_ext.equalsIgnoreCase(".htm")
					|| item_ext.equalsIgnoreCase(".php")
					|| item_ext.equalsIgnoreCase(".xml")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent htmlIntent = new Intent();
						htmlIntent
								.setAction(android.content.Intent.ACTION_VIEW);
						htmlIntent.setDataAndType(Uri.fromFile(file),
								"text/html");

						try {
							startActivity(htmlIntent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldntfindahtmlviewver),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}

			// Text file
			else if (item_ext.equalsIgnoreCase(".txt")
					|| item_ext.equalsIgnoreCase(".doc")
					|| item_ext.equalsIgnoreCase(".csv")
					|| item_ext.equalsIgnoreCase(".rtf")
					|| item_ext.equalsIgnoreCase(".text")) {

				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent txtIntent = new Intent();
						txtIntent.setAction(android.content.Intent.ACTION_VIEW);
						txtIntent.setDataAndType(Uri.fromFile(file),
								"text/plain");

						try {
							startActivity(txtIntent);
						} catch (ActivityNotFoundException e) {
							txtIntent.setType("text/*");
							startActivity(txtIntent);
						}
					}
				}
			}

			else {
				if (file.exists()) {
					if (mReturnIntent) {
						returnIntentResults(file);

					} else {
						Intent generic = new Intent();
						generic.setAction(android.content.Intent.ACTION_VIEW);
						generic.setDataAndType(Uri.fromFile(file), "text/plain");

						try {
							startActivity(generic);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(
									this,
									getString(R.string.sorrycouldnt)
											+ getString(R.string.toopen)
											+ file.getName(),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		SharedPreferences.Editor editor = mSettings.edit();
		boolean thumbnail;

		thumbnail = data.getBooleanExtra("THUMBNAIL", true);
		editor.putBoolean(PREFS_THUMBNAIL, thumbnail);
		editor.commit();

		String value = mSettings.getString("sort", "1");
		int sort = Integer.parseInt(value);

		String viewmode = mSettings.getString("viewmode", "1");
		viewm = Integer.parseInt(viewmode);

		mFileMag = new FileOperations();
		mFileMag.setSortType(sort);
		mHandler.setShowThumbnails(thumbnail);

		mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(),
				true));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);

		boolean multi_data = mHandler.hasMultiSelectData();
		AdapterContextMenuInfo _info = (AdapterContextMenuInfo) info;
		mSelectedListItem = mHandler.getData(_info.position);

		// is it a directory and is multi-select turned off
		if (mFileMag.isDirectory(mSelectedListItem)
				&& !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, D_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, D_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, D_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, D_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, D_MENU_ZIP, 0, getString(R.string.zipfolder));
			menu.add(0, D_MENU_PASTE, 0, getString(R.string.pasteintofolder))
					.setEnabled(mHoldingFile || multi_data);
			menu.add(0, D_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, D_MENU_DETAILS, 0, getString(R.string.details));

			// is it a file and is multi-select turned off
		} else if (!mFileMag.isDirectory(mSelectedListItem)
				&& !mHandler.isMultiSelected()) {
			menu.setHeaderTitle(mSelectedListItem);
			menu.add(0, F_MENU_OPENAS, 0, getString(R.string.openas));
			menu.add(0, F_MENU_DELETE, 0, getString(R.string.delete));
			menu.add(0, F_MENU_RENAME, 0, getString(R.string.rename));
			menu.add(0, F_MENU_COPY, 0, getString(R.string.copy));
			menu.add(0, F_MENU_MOVE, 0, getString(R.string.move));
			menu.add(0, F_MENU_ATTACH, 0, getString(R.string.share));
			menu.add(0, F_MENU_BOOKMARK, 0, getString(R.string.createbookmark));
			menu.add(0, F_MENU_DETAILS, 0, getString(R.string.details));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case F_MENU_OPENAS:
			openaction();
			return true;

		case D_MENU_BOOKMARK:
		case F_MENU_BOOKMARK:
			try {
				File files = new File(mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);
				String path = files.getAbsolutePath();
				@SuppressWarnings("deprecation")
				Cursor query = managedQuery(Bookmarks.CONTENT_URI,
						new String[] { Bookmarks._ID }, Bookmarks.PATH + "=?",
						new String[] { path }, null);
				if (!query.moveToFirst()) {
					ContentValues values = new ContentValues();
					values.put(Bookmarks.NAME, files.getName());
					values.put(Bookmarks.PATH, path);
					getContentResolver().insert(Bookmarks.CONTENT_URI, values);
					Toast.makeText(Main.this, R.string.bookmarkadded,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(Main.this, R.string.bookmarkexist,
							Toast.LENGTH_SHORT).show();
				}
			} catch (Exception e) {
				Toast.makeText(Main.this, "Error", Toast.LENGTH_SHORT).show();
			}
			return true;

		case D_MENU_DELETE:
		case F_MENU_DELETE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(mSelectedListItem);
			builder.setIcon(R.drawable.warning);
			builder.setMessage(getString(R.string.cannotbeundoneareyousureyouwanttodelete));
			builder.setCancelable(false);

			builder.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.setPositiveButton(getString(R.string.delete),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							mHandler.deleteFile(mFileMag.getCurrentDir() + "/"
									+ mSelectedListItem);

							mHandler.updateDirectory(mFileMag.getNextDir(
									mFileMag.getCurrentDir(), true));
							displayFreeSpace();
						}
					});
			AlertDialog alert_d = builder.create();
			alert_d.show();
			return true;

		case D_MENU_RENAME:
			AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

			alert1.setTitle(getString(R.string.rename));

			// Set an EditText view to get user input
			final EditText input3 = new EditText(this);
			input3.setText(mSelectedListItem);
			alert1.setView(input3);

			alert1.setPositiveButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (input3.getText().length() < 1)
								dialog.dismiss();

							if (mFileMag.renameTarget(mFileMag.getCurrentDir()
									+ "/" + mSelectedListItem, input3.getText()
									.toString()) == 0) {
								Toast.makeText(Main.this,
										getString(R.string.folderwasrenamed),
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(Main.this,
										mSelectedListItem + " was not renamed",
										Toast.LENGTH_LONG).show();

							dialog.dismiss();
							String temp = mFileMag.getCurrentDir();
							mHandler.updateDirectory(mFileMag.getNextDir(temp,
									true));
						}
					});

			alert1.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Canceled.
						}
					});

			alert1.show();
			return true;

		case F_MENU_DETAILS:

			filedetails();
			return true;

		case D_MENU_DETAILS:

			folderdetails();
			return true;

		case F_MENU_RENAME:
			AlertDialog.Builder alertf = new AlertDialog.Builder(this);

			alertf.setTitle(getString(R.string.rename));

			// Set an EditText view to get user input
			final EditText inputf = new EditText(this);
			inputf.setText(mSelectedListItem);
			alertf.setView(inputf);

			alertf.setPositiveButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (inputf.getText().length() < 1)
								dialog.dismiss();

							if (mFileMag.renameTarget(mFileMag.getCurrentDir()
									+ "/" + mSelectedListItem, inputf.getText()
									.toString()) == 0) {
								Toast.makeText(Main.this,
										getString(R.string.filewasrenamed),
										Toast.LENGTH_LONG).show();
							} else
								Toast.makeText(Main.this,
										mSelectedListItem + " was not renamed",
										Toast.LENGTH_LONG).show();

							dialog.dismiss();
							String temp = mFileMag.getCurrentDir();
							mHandler.updateDirectory(mFileMag.getNextDir(temp,
									true));
						}
					});

			alertf.setNegativeButton(getString(R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Cancel
						}
					});

			alertf.show();
			return true;

		case F_MENU_ATTACH:

			File sharefile = new File(mFileMag.getCurrentDir() + "/"
					+ mSelectedListItem);

			try {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_SUBJECT, mSelectedListItem);
				i.putExtra(Intent.EXTRA_BCC, "");
				i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(sharefile));
				startActivity(Intent
						.createChooser(i, getString(R.string.share)));
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.error),
						Toast.LENGTH_SHORT).show();
			}

		case F_MENU_MOVE:
		case D_MENU_MOVE:
		case F_MENU_COPY:
		case D_MENU_COPY:
			if (item.getItemId() == F_MENU_MOVE
					|| item.getItemId() == D_MENU_MOVE)
				mHandler.setDeleteAfterCopy(true);

			mHoldingFile = true;

			mCopiedTarget = mFileMag.getCurrentDir() + "/" + mSelectedListItem;

			return true;

		case D_MENU_PASTE:
			boolean multi_select = mHandler.hasMultiSelectData();

			if (multi_select) {
				mHandler.copyFileMultiSelect(mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);

			} else if (mHoldingFile && mCopiedTarget.length() > 1) {

				mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem);
			}

			mHoldingFile = false;
			return true;

		case D_MENU_ZIP:
			String dir = mFileMag.getCurrentDir();

			mHandler.zipFile(dir + "/" + mSelectedListItem);
			return true;

		case D_MENU_UNZIP:
			if (mHoldingZip && mZippedTarget.length() > 1) {
				String current_dir = mFileMag.getCurrentDir() + "/"
						+ mSelectedListItem + "/";
				String old_dir = mZippedTarget.substring(0,
						mZippedTarget.lastIndexOf("/"));
				String name = mZippedTarget.substring(
						mZippedTarget.lastIndexOf("/") + 1,
						mZippedTarget.length());

				if (new File(mZippedTarget).canRead()
						&& new File(current_dir).canWrite()) {
					mHandler.unZipFileToDir(name, current_dir, old_dir);
					setDirectoryButtons();

				} else {
					Toast.makeText(
							this,
							getString(R.string.youdonthavepermissiontounzip)
									+ name, Toast.LENGTH_SHORT).show();
				}
			}

			mHoldingZip = false;
			mDetailLabel.setText("");
			mZippedTarget = "";
			return true;
		}

		return false;
	}

	private void filedetails() {
		// Get Infos for Dialog
		File file3 = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);

		if (file3.isFile()) {
			double size = file3.length();
			if (size > GB)
				setDisplay_size(String.format("%.2f GB", (double) size / GB));
			else if (size < GB && size > MG)
				setDisplay_size(String.format("%.2f MB", (double) size / MG));
			else if (size < MG && size > KB)
				setDisplay_size(String.format("%.2f KB", (double) size / KB));
			else
				setDisplay_size(String.format("%.2f B", (double) size));
		}

		System.out.println("Before Format : " + file3.lastModified());
		SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		System.out.println("After Format : "
				+ sdf1.format(file3.lastModified()));

		// Only Dialog
		final Dialog dialog1 = new Dialog(Main.this);
		dialog1.setContentView(R.layout.details);
		// dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		dialog1.setTitle(R.string.details);
		dialog1.setCancelable(true);

		// set up text
		TextView textv = (TextView) dialog1.findViewById(R.id.name_label);
		textv.setText(mSelectedListItem);

		TextView textv0 = (TextView) dialog1.findViewById(R.id.path_label);
		textv0.setText(mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/");

		TextView textv1 = (TextView) dialog1.findViewById(R.id.time_stamp);
		textv1.setText(sdf1.format(file3.lastModified()));

		TextView textv2 = (TextView) dialog1.findViewById(R.id.total_size);
		textv2.setText(getDisplay_size() + "");

		// Set up Button
		Button button1 = (Button) dialog1.findViewById(R.id.quit);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog1.cancel();
			}
		});

		dialog1.show();
	}

	private void folderdetails() {
		// Get Info for Dialog
		File file1 = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);

		System.out.println("Before Format : " + file1.lastModified());
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		System.out
				.println("After Format : " + sdf.format(file1.lastModified()));

		// Only Dialog
		final Dialog dialog = new Dialog(Main.this);
		dialog.setContentView(R.layout.details);
		// dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		dialog.setTitle(R.string.details);
		dialog.setCancelable(true);

		// set up text
		TextView text = (TextView) dialog.findViewById(R.id.name_label);
		text.setText(mSelectedListItem);

		TextView text1 = (TextView) dialog.findViewById(R.id.path_label);
		text1.setText(mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/");

		TextView text2 = (TextView) dialog.findViewById(R.id.time_stamp);
		text2.setText(sdf.format(file1.lastModified()));

		TextView text3 = (TextView) dialog.findViewById(R.id.total_size);
		text3.setText(" --- ");

		// Set up Button
		Button button = (Button) dialog.findViewById(R.id.quit);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});

		dialog.show();
	}

	// Menus, options menu and context menu start here
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			mHandler.stopThumbnailThread();
			finish();
			return true;

		case R.id.multis:
			mHandler.multiselect();
			startActionMode(mActionModeCallback);
			return true;

		case R.id.bookmarks:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			final Cursor bookmarksCursor = getBookmarks();

			builder.setTitle(R.string.bookmark);
			builder.setCursor(bookmarksCursor,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (bookmarksCursor.moveToPosition(item)) {
								String path = bookmarksCursor.getString(bookmarksCursor
										.getColumnIndex(Bookmarks.PATH));
								File file = new File(path);
								if (file != null) {
									if (file.isDirectory()) {
										// getFileList(path);
										mHandler.opendir(path);
										setDirectoryButtons();
									} else {
										// getOperations(path);
										Intent i1 = new Intent();
										i1.setAction(android.content.Intent.ACTION_VIEW);
										i1.setDataAndType(Uri.fromFile(file),
												"*/*");
										startActivity(i1);
									}
								}
							} else {
								Toast.makeText(Main.this, "Error",
										Toast.LENGTH_SHORT).show();
							}
						}
					}, Bookmarks.NAME);
			builder.create();
			builder.show();
			return true;

		case R.id.create:
			final CharSequence[] create = { getString(R.string.newfile),
					getString(R.string.newd), };

			AlertDialog.Builder builder3 = new AlertDialog.Builder(Main.this);
			builder3.setItems(create, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int itemz) {
					switch (itemz) {

					case 0:
						createfile();
						return;

					case 1:
						AlertDialog.Builder alert = new AlertDialog.Builder(
								Main.this);
						alert.setTitle(getString(R.string.createnewfolder));

						final EditText input = new EditText(Main.this);
						alert.setView(input);
						alert.setCancelable(true);
						alert.setPositiveButton(getString(R.string.ok),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										if (input.getText().length() > 1) {
											if (mFileMag.createDir(
													mFileMag.getCurrentDir()
															+ "/", input
															.getText()
															.toString()) == 0)
												Toast.makeText(
														Main.this,
														input.getText()
																.toString()
																+ getString(R.string.created),
														Toast.LENGTH_LONG)
														.show();
											else
												Toast.makeText(
														Main.this,
														getString(R.string.newfolderwasnotcreated),
														Toast.LENGTH_SHORT)
														.show();
										}

										dialog.dismiss();
										String temp = mFileMag.getCurrentDir();
										mHandler.updateDirectory(mFileMag
												.getNextDir(temp, true));
									}
								});

						alert.setNegativeButton(getString(R.string.cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Cancel
									}
								});

						alert.show();
						return;
					}
				}

			});
			AlertDialog alertmore = builder3.create();
			alertmore.show();
			return true;

		case R.id.finfo:
			Intent info = new Intent(Main.this, DirectoryInfo.class);
			info.putExtra("PATH_NAME", mFileMag.getCurrentDir());
			Main.this.startActivity(info);
			return true;
		case R.id.settings:
			Intent intent = new Intent(Main.this, Settings.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@SuppressWarnings("deprecation")
	private Cursor getBookmarks() {
		return managedQuery(Bookmarks.CONTENT_URI, new String[] {
				Bookmarks._ID, Bookmarks.NAME, Bookmarks.PATH, }, null, null,
				null);
	}

	public void openaction() {
		final File file = new File(mFileMag.getCurrentDir() + "/"
				+ mSelectedListItem);
		final CharSequence[] openAsOperations = { getString(R.string.text),
				getString(R.string.image), getString(R.string.video),
				getString(R.string.music), getString(R.string.file) };

		AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
		builder.setItems(openAsOperations,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dInterface, int item) {

						switch (item) {
						case 0:
							Intent txtIntent = new Intent();
							txtIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							txtIntent.setDataAndType(Uri.fromFile(file),
									"text/plain");
							startActivity(txtIntent);
							break;
						case 1:
							Intent imageIntent = new Intent();
							imageIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							imageIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(imageIntent);
							break;

						case 2:
							Intent movieIntent = new Intent();
							movieIntent
									.setAction(android.content.Intent.ACTION_VIEW);
							movieIntent.setDataAndType(Uri.fromFile(file),
									"image/*");
							startActivity(movieIntent);
							break;
						case 3:

							Intent i = new Intent();
							i.setAction(android.content.Intent.ACTION_VIEW);
							i.setDataAndType(Uri.fromFile(file), "audio/*");
							startActivity(i);
							break;

						case 4:
							Intent i1 = new Intent();
							i1.setAction(android.content.Intent.ACTION_VIEW);
							i1.setDataAndType(Uri.fromFile(file), "*/*");
							startActivity(i1);
							break;
						}
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	// On back pressed Actions
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		String current = mFileMag.getCurrentDir();

		if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !current.equals("/")) {
			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
				Toast.makeText(Main.this, R.string.multioff, Toast.LENGTH_SHORT)
						.show();

			} else {
				// stop updating thumbnail icons if its running
				mHandler.stopThumbnailThread();
				mHandler.updateDirectory(mFileMag.getPreviousDir());
				setDirectoryButtons();
				listview();
			}
			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& current.equals("/")) {
			Toast.makeText(Main.this, getString(R.string.pressbackagaintoquit),
					Toast.LENGTH_SHORT).show();

			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
				Toast.makeText(Main.this, R.string.multioff, Toast.LENGTH_SHORT)
						.show();
			}

			mUseBackKey = false;
			setDirectoryButtons();

			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& current.equals("/")) {
			finish();

			return false;
		}
		return false;
	}

	protected void createfile() {
		AlertDialog.Builder alertf = new AlertDialog.Builder(this);
		alertf.setTitle(getString(R.string.newfile));

		final EditText editt = new EditText(this);
		alertf.setView(editt);
		alertf.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String path = editt.getText().toString();

						File file = new File(mFileMag.getCurrentDir()
								+ File.separator + path + ".txt");
						try {
							file.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
						}

						String temp = mFileMag.getCurrentDir();
						mHandler.updateDirectory(mFileMag
								.getNextDir(temp, true));

					}
				});

		alertf.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alertf.show();

	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (// When the user chooses to show/hide hidden files, update the list
			// to correspond with the user's choice
		Settings.PREFS_DISPLAYHIDDENFILES.equals(key)
				|| Settings.PREFS_PREVIEW.equals(key)) {

		}
	}

	public void setDisplay_size(String display_size) {
		this.display_size = display_size;
	}

	public String getDisplay_size() {
		return display_size;
	}

	private void displayFreeSpace() {
		ProgressBar statusBar = (ProgressBar) findViewById(R.id.progressBar);

		if (statusBar == null)
			return;

		int total = ProgressbarClass.totalMemory();
		int free = ProgressbarClass.freeMemory();

		if (free / total > 0.5) {
			statusBar.setVisibility(View.GONE);
		} else {
			statusBar.setVisibility(View.VISIBLE);
			statusBar.setMax(total);
			statusBar.setProgress(free);
		}
	}

	public static class ProgressbarClass extends Application {

		public static int totalMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
			return Total;
		}

		public static int freeMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
			return Free;
		}

		public static int busyMemory() {
			StatFs statFs = new StatFs(Environment.getRootDirectory()
					.getAbsolutePath());
			int Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
			int Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
			int Busy = Total - Free;
			return Busy;
		}
	}

	public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			mode.setTitle(R.string.choosefiles);
			getMenuInflater().inflate(R.menu.actionmode, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.actiondelete:
				try {
					mHandler.multidelete();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actionshare:
				try {
					multishare();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actioncopy:
				try {
					mHandler.multicopy();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			case R.id.actionmove:
				try {
					mHandler.multimove();
					mode.finish();
				} catch (Exception e) {
					Toast.makeText(Main.this, getString(R.string.choosefiles),
							Toast.LENGTH_SHORT).show();
					mode.finish();
				}
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			mActionMode = null;
			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
			}
		}

		@Override
		public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
			return false;
		}
	};

	public void multishare() {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		int length = mHandler.mMultiSelectData.size();
		Intent mail_int = new Intent();

		mail_int.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
		mail_int.setType("text/plain");
		mail_int.putExtra(Intent.EXTRA_BCC, "");
		mail_int.putExtra(Intent.EXTRA_SUBJECT, " ");

		for (int i = 0; i < length; i++) {
			File file = new File(mHandler.mMultiSelectData.get(i));
			uris.add(Uri.fromFile(file));
		}
		mail_int.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		mHandler.mContext.startActivity(Intent.createChooser(mail_int,
				getString(R.string.share)));
		mHandler.mDelegate.killMultiSelect(true);
	}
}
