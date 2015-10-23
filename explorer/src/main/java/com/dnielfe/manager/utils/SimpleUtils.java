package com.dnielfe.manager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.widget.Toast;

import com.dnielfe.manager.BrowserActivity;
import com.dnielfe.manager.R;
import com.dnielfe.manager.preview.IconPreview;
import com.dnielfe.manager.preview.MimeTypes;
import com.dnielfe.manager.settings.Settings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;

public class SimpleUtils {

    private static final int BUFFER = 8192;
    private static final long ONE_KB = 1024;
    private static final BigInteger KB_BI = BigInteger.valueOf(ONE_KB);
    private static final BigInteger MB_BI = KB_BI.multiply(KB_BI);
    private static final BigInteger GB_BI = KB_BI.multiply(MB_BI);
    private static final BigInteger TB_BI = KB_BI.multiply(GB_BI);

    // scan file after move/copy
    public static void requestMediaScanner(final Context context, final File... files) {
        final String[] paths = new String[files.length];
        int i = 0;
        for (final File file : files) {
            paths[i] = file.getPath();
            i++;
        }
        MediaScannerConnection.scanFile(context, paths, null, null);
    }

    // TODO: fix search with root
    private static void search_file(String dir, String fileName, ArrayList<String> n) {
        File root_dir = new File(dir);
        String[] list = root_dir.list();
        boolean root = Settings.rootAccess();

        if (list != null && root_dir.canRead()) {
            for (String aList : list) {
                File check = new File(dir + "/" + aList);
                String name = check.getName();

                if (check.isFile() && name.toLowerCase().contains(fileName.toLowerCase())) {
                    n.add(check.getPath());
                } else if (check.isDirectory()) {
                    if (name.toLowerCase().contains(fileName.toLowerCase())) {
                        n.add(check.getPath());

                        // change this!
                    } else if (check.canRead() && !dir.equals("/")) {
                        search_file(check.getAbsolutePath(), fileName, n);
                    } else if (!check.canRead() & root) {
                        ArrayList<String> al = RootCommands.findFiles(check.getAbsolutePath(), fileName);

                        for (String items : al) {
                            n.add(items);
                        }
                    }
                }
            }
        } else {
            if (root)
                n.addAll(RootCommands.findFiles(dir, fileName));
        }
    }

    public static ArrayList<String> listFiles(String path, Context c) {
        ArrayList<String> mDirContent = new ArrayList<>();
        boolean showhidden = Settings.showHiddenFiles();

        if (!mDirContent.isEmpty())
            mDirContent.clear();

        final File file = new File(path);

        if (file.exists() && file.canRead()) {
            String[] list = file.list();

            // add files/folder to ArrayList depending on hidden status
            for (String aList : list) {
                if (!showhidden) {
                    if (aList.charAt(0) != '.')
                        mDirContent.add(path + "/" + aList);
                } else {
                    mDirContent.add(path + "/" + aList);
                }
            }
        } else if (Settings.rootAccess()) {
            mDirContent = RootCommands.listFiles(file.getAbsolutePath(), showhidden);
        } else {
            Toast.makeText(c, c.getString(R.string.cantreadfolder), Toast.LENGTH_SHORT).show();
        }
        return mDirContent;
    }

    public static void moveToDirectory(String old, String newDir) {
        String file_name = old.substring(old.lastIndexOf("/"), old.length());
        File old_file = new File(old);
        File cp_file = new File(newDir + file_name);

        if (!old_file.renameTo(cp_file)) {
            copyToDirectory(old, newDir);
            deleteTarget(old);
        }
    }

    public static void copyToDirectory(String old, String newDir) {
        File old_file = new File(old);
        File temp_dir = new File(newDir);
        byte[] data = new byte[BUFFER];
        int read;

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (old_file.isDirectory()) {
                String files[] = old_file.list();
                String dir = newDir
                        + old.substring(old.lastIndexOf("/"), old.length());

                if (!new File(dir).mkdir())
                    return;

                for (String file : files) copyToDirectory(old + "/" + file, dir);
            }
        } else {
            if (Settings.rootAccess())
                RootCommands.moveCopyRoot(old, newDir);
        }
    }

    // filePath = currentDir + "/" + item
    // newName = new name
    public static boolean renameTarget(String filePath, String newName) {
        File src = new File(filePath);

        String temp = filePath.substring(0, filePath.lastIndexOf("/"));
        File dest = new File(temp + "/" + newName);

        return src.renameTo(dest);
    }

    // path = currentDir
    // name = new name
    public static boolean createDir(String path, String name) {
        File folder = new File(path, name);
        boolean success = false;

        if (folder.exists())
            success = false;

        if (folder.mkdir())
            success = true;
        else if (Settings.rootAccess()) {
            success = RootCommands.createRootdir(folder, path);
        }

        return success;
    }

    public static void deleteTarget(String path) {
        File target = new File(path);

        if (target.isFile() && target.canWrite()) {
            target.delete();
        } else if (target.isDirectory() && target.canRead() && target.canWrite()) {
            String[] file_list = target.list();

            if (file_list != null && file_list.length == 0) {
                target.delete();
                return;
            } else if (file_list != null && file_list.length > 0) {
                for (String aFile_list : file_list) {
                    File temp_f = new File(target.getAbsolutePath() + "/"
                            + aFile_list);

                    if (temp_f.isDirectory())
                        deleteTarget(temp_f.getAbsolutePath());
                    else if (temp_f.isFile()) {
                        temp_f.delete();
                    }
                }
            }

            if (target.exists())
                target.delete();
        } else if (!target.delete() && Settings.rootAccess()) {
            RootCommands.deleteRootFileOrDir(target);
        }
    }

    public static ArrayList<String> searchInDirectory(String dir, String fileName) {
        ArrayList<String> names = new ArrayList<>();
        search_file(dir, fileName, names);
        return names;
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
            Toast.makeText(context, R.string.cantopenfile, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            context.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.cantopenfile) + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // get MD5 or SHA1 checksum from a file
    public static String getChecksum(File file, String algorithm) {
        try {
            InputStream fis = new FileInputStream(file);
            MessageDigest digester = MessageDigest.getInstance(algorithm);
            byte[] bytes = new byte[2 * BUFFER];
            int byteCount;
            String result = "";

            while ((byteCount = fis.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
            }

            for (byte aB : digester.digest()) {
                result += Integer.toString((aB & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        Intent shortcutIntent;

        try {
            // Create the intent that will handle the shortcut
            if (file.isFile()) {
                shortcutIntent = new Intent(Intent.ACTION_VIEW);
                shortcutIntent.setDataAndType(Uri.fromFile(file), MimeTypes.getMimeType(file));
                shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                shortcutIntent = new Intent(main, BrowserActivity.class);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                shortcutIntent.putExtra(BrowserActivity.EXTRA_SHORTCUT, path);
            }

            // The intent to send to broadcast for register the shortcut intent
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, file.getName());

            if (file.isFile()) {
                BitmapDrawable bd = (BitmapDrawable) IconPreview.getBitmapDrawableFromFile(file);

                if (bd != null) {
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bd.getBitmap());
                } else {
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext(main, R.drawable.type_unknown));
                }
            } else {
                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(main, R.drawable.ic_launcher));
            }

            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            main.sendBroadcast(intent);

            Toast.makeText(main, main.getString(R.string.shortcutcreated),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(main, main.getString(R.string.error), Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatCalculatedSize(long ls) {
        BigInteger size = BigInteger.valueOf(ls);
        String displaySize;

        if (size.divide(TB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(TB_BI)) + " TB";
        } else if (size.divide(GB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(GB_BI)) + " GB";
        } else if (size.divide(MB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(MB_BI)) + " MB";
        } else if (size.divide(KB_BI).compareTo(BigInteger.ZERO) > 0) {
            displaySize = String.valueOf(size.divide(KB_BI)) + " KB";
        } else {
            displaySize = String.valueOf(size) + " bytes";
        }
        return displaySize;
    }

    public static long getDirectorySize(File directory) {
        final File[] files = directory.listFiles();
        long size = 0;

        if (files == null) {
            return 0L;
        }

        for (final File file : files) {
            try {
                if (!isSymlink(file)) {
                    size += sizeOf(file);
                    if (size < 0) {
                        break;
                    }
                }
            } catch (IOException ioe) {
                // ignore exception when asking for symlink
            }
        }

        return size;
    }

    private static boolean isSymlink(File file) throws IOException {
        File fileInCanonicalDir;

        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    private static long sizeOf(File file) {
        if (file.isDirectory()) {
            return getDirectorySize(file);
        } else {
            return file.length();
        }
    }

    public static String getExtension(String name) {
        String ext;

        if (name.lastIndexOf(".") == -1) {
            ext = "";

        } else {
            int index = name.lastIndexOf(".");
            ext = name.substring(index + 1, name.length());
        }
        return ext;
    }

    public static boolean isSupportedArchive(File file) {
        String ext = getExtension(file.getName());
        return ext.equalsIgnoreCase("zip");
    }
}