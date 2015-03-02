package com.dnielfe.manager.utils;

import android.util.Log;

import com.dnielfe.manager.settings.Settings;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootCommands {

    private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";

    private static String getCommandLineString(String input) {
        return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
    }

    public static ArrayList<String> listFiles(String path, boolean showhidden) {
        ArrayList<String> mDirContent = new ArrayList<>();
        BufferedReader in;

        try {
            in = execute("ls -a " + getCommandLineString(path));

            String line;
            while ((line = in.readLine()) != null) {
                if (!showhidden) {
                    if (line.charAt(0) != '.')
                        mDirContent.add(path + "/" + line);
                } else {
                    mDirContent.add(path + "/" + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    public static ArrayList<String> findFiles(String path, String query) {
        ArrayList<String> mDirContent = new ArrayList<>();
        String cmd = "find " + getCommandLineString(path) + " -type f -iname " + '*' + getCommandLineString(query) + '*' + " -exec ls -a {} \\;";
        BufferedReader in;

        try {
            in = execute(cmd);

            String line;
            while ((line = in.readLine()) != null) {
                mDirContent.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    // Create Directory with root
    public static boolean createRootdir(File dir, String path) {
        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            runAndWait("mkdir " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Move or Copy with Root Access using RootTools library
    public static void moveCopyRoot(String old, String newDir) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(newDir, "rw");

            runAndWait("cp -fr " + getCommandLineString(old) + " " + getCommandLineString(newDir));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // path = currentDir
    // oldName = currentDir + "/" + selected Item
    // name = new name
    public static void renameRootTarget(String path, String oldname, String name) {
        File file = new File(path + "/" + oldname);
        File newf = new File(path + "/" + name);

        if (name.length() < 1)
            return;

        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            runAndWait("mv " + getCommandLineString(file.getAbsolutePath()) + " "
                    + getCommandLineString(newf.getAbsolutePath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Delete file with root using RootTools library
    public static void deleteRootFileOrDir(File f) {
        RootTools.deleteFileOrDirectory(getCommandLineString(f.getPath()), true);
    }

    // Create file with root
    public static boolean createRootFile(String cdir, String name) {
        File dir = new File(cdir + "/" + name);

        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(cdir, "rw");

            runAndWait("touch " + getCommandLineString(dir.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // Check if system is mounted
    private static boolean readReadWriteFile() {
        File mountFile = new File("/proc/mounts");
        StringBuilder procData = new StringBuilder();
        if (mountFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(mountFile.toString());
                DataInputStream dis = new DataInputStream(fis);
                BufferedReader br = new BufferedReader(new InputStreamReader(dis));
                String data;
                while ((data = br.readLine()) != null) {
                    procData.append(data).append("\n");
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            String[] tmp = procData.toString().split("\n");
            for (String aTmp : tmp) {
                // Kept simple here on purpose different devices have
                // different blocks
                if (aTmp.contains("/dev/block")
                        && aTmp.contains("/system")) {
                    if (aTmp.contains("rw")) {
                        // system is rw
                        return true;
                    } else if (aTmp.contains("ro")) {
                        // system is ro
                        return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsIllegals(String toExamine) {
        // checks for "+" sign so the program doesn't throw an error when its
        // not erroring.
        Pattern pattern = Pattern.compile("[+]");
        Matcher matcher = pattern.matcher(toExamine);
        return matcher.find();
    }

    private static BufferedReader execute(String cmd) {
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String err = (new BufferedReader(new InputStreamReader(
                    process.getErrorStream()))).readLine();
            os.flush();

            if (process.waitFor() != 0 || (!"".equals(err) && null != err)
                    && !containsIllegals(err)) {
                Log.e("Root Error, cmd: " + cmd, err);
                return null;
            }
            return reader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean changeGroupOwner(File file, String owner, String group) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(file.getAbsolutePath(), "rw");

            runAndWait("chown " + owner + "." + group + " "
                    + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean applyPermissions(File file, Permissions permissions) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(file.getAbsolutePath(), "rw");

            runAndWait("chmod " + Permissions.toOctalPermission(permissions) + " "
                    + getCommandLineString(file.getAbsolutePath()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String[] getFileProperties(File file) {
        BufferedReader in;
        String[] info = null;
        String line;

        if (!Settings.rootAccess())
            return null;

        try {
            in = execute("ls -l " + getCommandLineString(file.getAbsolutePath()));

            while ((line = in.readLine()) != null) {
                info = getAttrs(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    private static String[] getAttrs(String string) {
        if (string.length() < 44) {
            throw new IllegalArgumentException("Bad ls -l output: " + string);
        }
        final char[] chars = string.toCharArray();

        final String[] results = new String[11];
        int ind = 0;
        final StringBuilder current = new StringBuilder();

        Loop:
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                case '\t':
                    if (current.length() != 0) {
                        results[ind] = current.toString();
                        ind++;
                        current.setLength(0);
                        if (ind == 10) {
                            results[ind] = string.substring(i).trim();
                            break Loop;
                        }
                    }
                    break;

                default:
                    current.append(chars[i]);
                    break;
            }
        }

        return results;
    }

    private static String runAndWait(String cmd) {
        Command c = new Command(0, cmd);

        try {
            RootTools.getShell(true).add(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (!waitForCommand(c)) {
            return null;
        }

        return c.toString();
    }

    private static boolean waitForCommand(Command cmd) {
        while (!cmd.isFinished()) {
            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                return false;
            }
        }

        return true;
    }
}
