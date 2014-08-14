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

package com.dnielfe.manager.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;

import com.dnielfe.manager.Browser;
import com.dnielfe.manager.R;
import com.dnielfe.manager.commands.RootCommands;
import com.dnielfe.manager.preview.MimeTypes;
import com.dnielfe.manager.settings.Settings;
import com.stericson.RootTools.RootTools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.widget.Toast;

public class SimpleUtils {

	private static final int BUFFER = 2048;
	private static long mDirSize = 0;
	private static int fileCount = 0;

	// scan file after move/copy
	public static void requestMediaScanner(final Context context,
			final File... files) {
		final String[] paths = new String[files.length];
		int i = 0;
		for (final File file : files) {
			paths[i] = file.getPath();
			i++;
		}
		MediaScannerConnection.scanFile(context, paths, null, null);
	}

	/*
	 * @param dir directory to search in
	 * 
	 * @param fileName filename that is being searched for
	 * 
	 * @param n ArrayList to populate results
	 */
	private static void search_file(String dir, String fileName,
			ArrayList<String> n) {
		File root_dir = new File(dir);
		String[] list = root_dir.list();

		if (list != null && root_dir.canRead()) {
			int len = list.length;

			for (int i = 0; i < len; i++) {
				File check = new File(dir + "/" + list[i]);
				String name = check.getName();

				if (check.isFile()
						&& name.toLowerCase().contains(fileName.toLowerCase())) {
					n.add(check.getPath());
				} else if (check.isDirectory()) {
					if (name.toLowerCase().contains(fileName.toLowerCase())) {
						n.add(check.getPath());

					} else if (check.canRead() && !dir.equals("/"))
						search_file(check.getAbsolutePath(), fileName, n);
				}
			}
		}
	}

	public static ArrayList<String> listFiles(String path) {
		ArrayList<String> mDirContent = new ArrayList<String>();
		boolean showhidden = Settings.showHiddenFiles();

		if (!mDirContent.isEmpty())
			mDirContent.clear();

		final File file = new File(path);

		if (file.exists() && file.canRead()) {
			String[] list = file.list();
			int len = list.length;

			// add files/folder to ArrayList depending on hidden status
			for (int i = 0; i < len; i++) {
				if (!showhidden) {
					if (list[i].toString().charAt(0) != '.')
						mDirContent.add(path + "/" + list[i]);
				} else {
					mDirContent.add(path + "/" + list[i]);
				}
			}
		} else {
			try {
				mDirContent = RootCommands.listFiles(file.getAbsolutePath(),
						showhidden);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// sort files with a comparator
		SortUtils.sortList(mDirContent, file.getPath());
		return mDirContent;
	}

	public static long getDirSize(File path) {
		getDirectorySize(path);
		return mDirSize;
	}

	/**
	 * @param path
	 *            of directory
	 */
	public static void getDirectorySize(File path) {
		File[] list = path.listFiles();
		int len;

		if (list != null) {
			len = list.length;
			for (int i = 0; i < len; i++) {
				try {
					if (list[i].isFile() && list[i].canRead()) {
						mDirSize += list[i].length();
					} else if (list[i].isDirectory() && list[i].canRead()
							&& !FileUtils.isSymlink(list[i])) {
						getDirectorySize(list[i]);
					}
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 
	 * @param old
	 *            the file to be copied/ moved
	 * @param newDir
	 *            the directory to copy/move the file to
	 */
	public static void copyToDirectory(String old, String newDir) {
		File old_file = new File(old);
		File temp_dir = new File(newDir);
		byte[] data = new byte[BUFFER];
		int read = 0;

		if (old_file.canWrite() && temp_dir.isDirectory()
				&& temp_dir.canWrite()) {
			if (old_file.isFile()) {
				String file_name = old.substring(old.lastIndexOf("/"),
						old.length());
				File cp_file = new File(newDir + file_name);

				try {
					BufferedOutputStream o_stream = new BufferedOutputStream(
							new FileOutputStream(cp_file));
					BufferedInputStream i_stream = new BufferedInputStream(
							new FileInputStream(old_file));

					while ((read = i_stream.read(data, 0, BUFFER)) != -1)
						o_stream.write(data, 0, read);

					o_stream.flush();
					i_stream.close();
					o_stream.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (old_file.isDirectory()) {
				String files[] = old_file.list();
				String dir = newDir
						+ old.substring(old.lastIndexOf("/"), old.length());
				int len = files.length;

				if (!new File(dir).mkdir())
					return;

				for (int i = 0; i < len; i++)
					copyToDirectory(old + "/" + files[i], dir);
			}
		} else {
			if (RootTools.isAccessGiven())
				RootCommands.moveCopyRoot(old, newDir);
		}

		return;
	}

	// filePath = currentDir + "/" + item
	// newName = new name
	public static boolean renameTarget(String filePath, String newName) {
		File src = new File(filePath);

		String temp = filePath.substring(0, filePath.lastIndexOf("/"));
		File dest = new File(temp + "/" + newName);

		if (src.renameTo(dest))
			return true;
		else
			return false;
	}

	// path = currentDir
	// name = new name
	public static boolean createDir(String path, String name) {
		File folder = new File(path, name);

		if (folder.exists())
			return false;

		if (folder.mkdir())
			return true;
		else {
			try {
				RootCommands.createRootdir(folder, path);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public static void deleteTarget(Activity activity, String path, String dir) {
		File target = new File(path);

		if (!target.exists()) {
			return;
		} else if (target.isFile() && target.canWrite()) {
			target.delete();
			requestMediaScanner(activity, target);
			return;
		} else if (target.isDirectory() && target.canRead()) {
			String[] file_list = target.list();

			if (file_list != null && file_list.length == 0) {
				target.delete();
				return;
			} else if (file_list != null && file_list.length > 0) {

				for (int i = 0; i < file_list.length; i++) {
					File temp_f = new File(target.getAbsolutePath() + "/"
							+ file_list[i]);

					if (temp_f.isDirectory())
						deleteTarget(activity, temp_f.getAbsolutePath(), dir);
					else if (temp_f.isFile()) {
						temp_f.delete();
						requestMediaScanner(activity, temp_f);
					}
				}
			}

			if (target.exists())
				if (target.delete())
					return;
		} else if (target.exists() && !target.delete()) {
			RootCommands.DeleteFileRoot(path, dir);
		}
		return;
	}

	/**
	 * 
	 * @param dir
	 * @param pathName
	 * @return
	 */
	public static ArrayList<String> searchInDirectory(String dir,
			String fileName) {
		ArrayList<String> names = new ArrayList<String>();
		search_file(dir, fileName, names);

		return names;
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

	public static void openFile(final Context context, final File target) {
		final String mime = MimeTypes.getMimeType(target);
		final Intent i = new Intent(Intent.ACTION_VIEW);

		if (mime != null) {
			i.setDataAndType(Uri.fromFile(target), mime);
		} else {
			i.setDataAndType(Uri.fromFile(target), "*/*");
		}

		if (context.getPackageManager().queryIntentActivities(i, 0).isEmpty()) {
			Toast.makeText(context, R.string.cantopenfile, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		try {
			context.startActivity(i);
		} catch (Exception e) {
			Toast.makeText(context,
					context.getString(R.string.cantopenfile) + e.getMessage(),
					Toast.LENGTH_SHORT).show();
		}
	}

	public static byte[] createChecksum(String filename) throws Exception {
		InputStream fis = new FileInputStream(filename);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();
	}

	// a byte array to a HEX string
	public static String getMD5Checksum(String filename) throws Exception {
		byte[] b = createChecksum(filename);
		String result = "";

		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	// save current string in ClipBoard
	public static void savetoClipBoard(final Context co, String dir1) {
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) co
				.getSystemService(Context.CLIPBOARD_SERVICE);
		android.content.ClipData clip = android.content.ClipData.newPlainText(
				"Copied Text", dir1);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(co,
				"'" + dir1 + "' " + co.getString(R.string.copiedtoclipboard),
				Toast.LENGTH_SHORT).show();
	}

	public static void createShortcut(Activity main, String path) {
		File file = new File(path);

		try {
			// Create the intent that will handle the shortcut
			Intent shortcutIntent = new Intent(main, Browser.class);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			shortcutIntent.putExtra(Browser.EXTRA_SHORTCUT, path);

			// The intent to send to broadcast for register the shortcut intent
			Intent intent = new Intent();
			intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, file.getName());
			intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(main,
							R.drawable.ic_launcher));
			intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			main.sendBroadcast(intent);

			Toast.makeText(main, main.getString(R.string.shortcutcreated),
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(main, main.getString(R.string.error),
					Toast.LENGTH_SHORT).show();
		}
	}
}