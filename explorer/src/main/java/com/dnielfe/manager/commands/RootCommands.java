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

package com.dnielfe.manager.commands;

import android.util.Log;

import com.dnielfe.manager.settings.Settings;
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
        ArrayList<String> mDirContent = new ArrayList<String>();
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
        ArrayList<String> mDirContent = new ArrayList<String>();
        BufferedReader in;

        try {
            in = execute(buildfindCommand(path, query));

            String line;
            while ((line = in.readLine()) != null) {
                mDirContent.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    private static String buildfindCommand(String startDirectory, String what) {
        final StringBuilder command = new StringBuilder();
        command.append("find ");
        command.append(getCommandLineString(startDirectory));
        command.append(" -iname ");
        command.append('*');
        command.append(getCommandLineString(what));
        command.append('*');
        command.append(" -exec ls -a {} \\;");
        return command.toString();
    }

    // Create Directory with root
    public static boolean createRootdir(File dir, String path) {
        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            execute("mkdir " + getCommandLineString(dir.getAbsolutePath()));
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

            execute("cp -fr " + getCommandLineString(old) + " "
                    + getCommandLineString(newDir));
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

            execute("mv " + file.getAbsolutePath() + " "
                    + newf.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Delete file with root
    public static void DeleteFileRoot(String path) {

        try {
            if (!readReadWriteFile())
                RootTools.remount(path, "rw");

            if (new File(path).isDirectory()) {
                execute("rm -f -r " + getCommandLineString(path));
            } else {
                execute("rm -r " + getCommandLineString(path));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create file with root
    public static boolean createRootFile(String cdir, String name) {
        File dir = new File(cdir + "/" + name);

        if (dir.exists())
            return false;

        try {
            if (!readReadWriteFile())
                RootTools.remount(cdir, "rw");

            execute("touch " + getCommandLineString(dir.getAbsolutePath()));
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
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        dis));
                String data;
                while ((data = br.readLine()) != null) {
                    procData.append(data).append("\n");
                }

                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            if (procData.toString() != null) {
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

    public static BufferedReader execute(String cmd) {
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(
                    process.getOutputStream());
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

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean changeGroupOwner(File file, String owner, String group) {
        try {
            if (!readReadWriteFile())
                RootTools.remount(file.getAbsolutePath(), "rw");

            execute("chown " + owner + "." + group + " "
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

            execute("chmod " + toOctalPermission(permissions) + " "
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
            in = execute("ls -l "
                    + getCommandLineString(file.getAbsolutePath()));

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

    /**
     * Returns octal-formatted permission
     *
     * @param p Permissions to generate octal format for
     * @return octal-formatted permission representation
     */

    private static String toOctalPermission(final Permissions p) {
        byte user = 00;
        byte group = 00;
        byte other = 00;

        if (p.ur) {
            user += 04;
        }
        if (p.uw) {
            user += 02;
        }
        if (p.ux) {
            user += 01;
        }

        if (p.gr) {
            group += 04;
        }
        if (p.gw) {
            group += 02;
        }
        if (p.gx) {
            group += 01;
        }

        if (p.or) {
            other += 04;
        }
        if (p.ow) {
            other += 02;
        }
        if (p.ox) {
            other += 01;
        }

        return String.valueOf(user) + group + other;
    }
}
