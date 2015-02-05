package com.dnielfe.manager.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final int BUFFER = 8192;

    public static void createZip(String[] files, String zipFile) {
        try {
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));

            for (String s : files) {
                File file = new File(s);

                if (file.isDirectory()) {
                    zipSubFolder(out, file, file.getParent().length());
                } else {
                    zipFile(out, file);
                }
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unpackZip(File zipFile, File location) {
        try {
            // Extract entries while creating required sub-directories
            ZipFile zf = new ZipFile(zipFile);
            Enumeration<?> e = zf.entries();

            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                File destinationFilePath = new File(location, entry.getName());

                // create directories if required.
                destinationFilePath.getParentFile().mkdirs();

                // if the entry is directory, leave it. Otherwise extract it.
                if (!entry.isDirectory()) {
                    // Get the InputStream for current entry of the zip file
                    // using InputStream getInputStream(Entry entry) method.
                    BufferedInputStream bis = new BufferedInputStream(zf.getInputStream(entry));

                    int b;
                    byte buffer[] = new byte[BUFFER];

                    // read the current entry from the zip file, extract it and
                    // write the extracted file.
                    FileOutputStream fos = new FileOutputStream(destinationFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER);

                    while ((b = bis.read(buffer, 0, 1024)) != -1) {
                        bos.write(buffer, 0, b);
                    }

                    bos.flush();
                    bos.close();
                    bis.close();
                }
            }

            zf.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void zipSubFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {
        File[] fileList = folder.listFiles();

        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                BufferedInputStream origin;
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);

                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }

    private static void zipFile(ZipOutputStream out, File file)
            throws IOException {
        BufferedInputStream origin;
        byte data[] = new byte[BUFFER];
        String str = file.getPath();

        FileInputStream fi = new FileInputStream(str);
        origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(str.substring(str.lastIndexOf("/") + 1));
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
    }
}
