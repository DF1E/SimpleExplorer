package com.dnielfe.manager.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compress {

	private static final int BUFFER = 2048;

	private String[] files;
	private String zipFile;

	public Compress(String[] files, String name) {
		this.files = files;
		this.zipFile = name;
	}

	public void zip(String location) {
		BufferedInputStream origin = null;
		int count;

		try {
			FileOutputStream dest = new FileOutputStream(zipFile);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
					dest));

			byte data[] = new byte[BUFFER];

			for (int i = 0; i < files.length; i++) {
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(files[i].substring(files[i]
						.lastIndexOf("/") + 1));
				out.putNextEntry(entry);
				out.setLevel(Deflater.DEFAULT_COMPRESSION);
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}