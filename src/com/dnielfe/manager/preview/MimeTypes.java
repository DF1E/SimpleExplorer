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

package com.dnielfe.manager.preview;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import android.webkit.MimeTypeMap;

import com.dnielfe.manager.R;

public final class MimeTypes {

	private MimeTypes() {

	}

	private static final HashMap<String, Integer> EXT_ICONS = new HashMap<String, Integer>();

	private static final HashMap<String, String> MIME_TYPES = new HashMap<String, String>();

	static {
		// BINARY
		EXT_ICONS.put("a", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("bin", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("class", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("com", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("dex", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("dump", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("exe", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("dat", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("dll", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("lib", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("o", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("obj", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("pyc", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("pyo", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("ser", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("swf", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("so", Integer.valueOf(R.drawable.blanc));

		// Shell
		EXT_ICONS.put("bar", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("csh", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("ksh", Integer.valueOf(R.drawable.blanc));
		EXT_ICONS.put("sh", Integer.valueOf(R.drawable.blanc));

		// TEXT
		EXT_ICONS.put("csv", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("diff", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("in", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("list", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("log", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("rc", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("text", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("txt", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("tsv", Integer.valueOf(R.drawable.text1));

		// Properties
		EXT_ICONS.put("properties", Integer.valueOf(R.drawable.config));
		EXT_ICONS.put("conf", Integer.valueOf(R.drawable.config));
		EXT_ICONS.put("prop", Integer.valueOf(R.drawable.config));

		// HTML
		EXT_ICONS.put("htm", Integer.valueOf(R.drawable.html));
		EXT_ICONS.put("html", Integer.valueOf(R.drawable.html));
		EXT_ICONS.put("mhtml", Integer.valueOf(R.drawable.html));
		EXT_ICONS.put("xhtml", Integer.valueOf(R.drawable.html));

		// XML
		EXT_ICONS.put("xml", Integer.valueOf(R.drawable.xml32));
		EXT_ICONS.put("mxml", Integer.valueOf(R.drawable.xml32));

		// DOCUMENT
		EXT_ICONS.put("doc", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("docx", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("odp", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("odt", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("rtf", Integer.valueOf(R.drawable.text1));

		EXT_ICONS.put("ods", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("xls", Integer.valueOf(R.drawable.text1));
		EXT_ICONS.put("xlsx", Integer.valueOf(R.drawable.text1));

		// Presentation
		EXT_ICONS.put("ppt", Integer.valueOf(R.drawable.ppt));
		EXT_ICONS.put("pptx", Integer.valueOf(R.drawable.ppt));

		// PDF
		EXT_ICONS.put("pdf", Integer.valueOf(R.drawable.pdf));
		EXT_ICONS.put("fdf", Integer.valueOf(R.drawable.pdf));

		// Compress
		EXT_ICONS.put("ace", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("bz", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("bz2", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("cab", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("cpio", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("gz", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("lha", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("lrf", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("lzma", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("rar", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("tar", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("tgz", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("xz", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("zip", Integer.valueOf(R.drawable.zip));
		EXT_ICONS.put("Z", Integer.valueOf(R.drawable.zip));

		// JAR
		EXT_ICONS.put("jar", Integer.valueOf(R.drawable.jar32));

		// RAR
		EXT_ICONS.put("rar", Integer.valueOf(R.drawable.rar));

		// TAR
		EXT_ICONS.put("tar", Integer.valueOf(R.drawable.tar));

		// Image
		EXT_ICONS.put("bmp", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("cgm", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("g3", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("gif", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("ief", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("jpe", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("jpeg", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("jpg", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("png", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("btif", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("svg", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("svgz", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("tif", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("tiff", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("psd", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("dwg", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("dxf", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("fbs", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("fpx", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("fst", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("mmr", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("rlc", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("mdi", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("npx", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("wbmp", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("xif", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("ras", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("ico", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("pcx", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("pct", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("pic", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("xbm", Integer.valueOf(R.drawable.image));
		EXT_ICONS.put("xwd", Integer.valueOf(R.drawable.image));

		// Audio
		EXT_ICONS.put("aac", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("adp", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("aif", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("aifc", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("aiff", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("amr", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("ape", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("au", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("dts", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("eol", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("flac", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("kar", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("lvp", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("m2a", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("m3a", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("m3u", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("m4a", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mid", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mid", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mka", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mp2", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mp3", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("mpga", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("oga", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("ogg", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("pya", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("ram", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("rmi", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("snd", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("spx", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("wav", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("wax", Integer.valueOf(R.drawable.music));
		EXT_ICONS.put("wma", Integer.valueOf(R.drawable.music));

		// Video
		EXT_ICONS.put("3gp", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("3gpp", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("3g2", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("3gpp2", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("h261", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("h263", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("h264", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("jpgv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("jpgm", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("jpm", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mj2", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mp4", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mp4v", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mpg4", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("m1v", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("m2v", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mpa", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mpe", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mpg", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mpeg", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("ogv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mov", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("qt", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("fvt", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("m4u", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("pyv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("viv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("f4v", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("fli", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("flv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("m4v", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("asf", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("asx", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("avi", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("wmv", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("wmx", Integer.valueOf(R.drawable.movies));
		EXT_ICONS.put("mkv", Integer.valueOf(R.drawable.movies));

		// Application
		EXT_ICONS.put("apk", Integer.valueOf(R.drawable.appicon));

		/*
		 * ================= MIME TYPES ====================
		 */
		MIME_TYPES.put("asm", "text/x-asm");
		MIME_TYPES.put("def", "text/plain");
		MIME_TYPES.put("in", "text/plain");
		MIME_TYPES.put("rc", "text/plain");
		MIME_TYPES.put("list", "text/plain");
		MIME_TYPES.put("log", "text/plain");
		MIME_TYPES.put("pl", "text/plain");
		MIME_TYPES.put("prop", "text/plain");
		MIME_TYPES.put("properties", "text/plain");
		MIME_TYPES.put("rc", "text/plain");

		MIME_TYPES.put("epub", "application/epub+zip");
		MIME_TYPES.put("ibooks", "application/x-ibooks+zip");

		MIME_TYPES.put("ifb", "text/calendar");
		MIME_TYPES.put("eml", "message/rfc822");
		MIME_TYPES.put("msg", "application/vnd.ms-outlook");

		MIME_TYPES.put("ace", "application/x-ace-compressed");
		MIME_TYPES.put("bz", "application/x-bzip");
		MIME_TYPES.put("bz2", "application/x-bzip2");
		MIME_TYPES.put("cab", "application/vnd.ms-cab-compressed");
		MIME_TYPES.put("gz", "application/x-gzip");
		MIME_TYPES.put("lrf", "application/octet-stream");
		MIME_TYPES.put("jar", "application/java-archive");
		MIME_TYPES.put("xz", "application/x-xz");
		MIME_TYPES.put("Z", "application/x-compress");

		MIME_TYPES.put("bat", "application/x-msdownload");
		MIME_TYPES.put("ksh", "text/plain");
		MIME_TYPES.put("sh", "application/x-sh");

		MIME_TYPES.put("db", "application/octet-stream");
		MIME_TYPES.put("db3", "application/octet-stream");

		MIME_TYPES.put("otf", "x-font-otf");
		MIME_TYPES.put("ttf", "x-font-ttf");
		MIME_TYPES.put("psf", "x-font-linux-psf");

		MIME_TYPES.put("cgm", "image/cgm");
		MIME_TYPES.put("btif", "image/prs.btif");
		MIME_TYPES.put("dwg", "image/vnd.dwg");
		MIME_TYPES.put("dxf", "image/vnd.dxf");
		MIME_TYPES.put("fbs", "image/vnd.fastbidsheet");
		MIME_TYPES.put("fpx", "image/vnd.fpx");
		MIME_TYPES.put("fst", "image/vnd.fst");
		MIME_TYPES.put("mdi", "image/vnd.ms-mdi");
		MIME_TYPES.put("npx", "image/vnd.net-fpx");
		MIME_TYPES.put("xif", "image/vnd.xiff");
		MIME_TYPES.put("pct", "image/x-pict");
		MIME_TYPES.put("pic", "image/x-pict");

		MIME_TYPES.put("adp", "audio/adpcm");
		MIME_TYPES.put("au", "audio/basic");
		MIME_TYPES.put("snd", "audio/basic");
		MIME_TYPES.put("m2a", "audio/mpeg");
		MIME_TYPES.put("m3a", "audio/mpeg");
		MIME_TYPES.put("oga", "audio/ogg");
		MIME_TYPES.put("spx", "audio/ogg");
		MIME_TYPES.put("aac", "audio/x-aac");
		MIME_TYPES.put("mka", "audio/x-matroska");

		MIME_TYPES.put("jpgv", "video/jpeg");
		MIME_TYPES.put("jpgm", "video/jpm");
		MIME_TYPES.put("jpm", "video/jpm");
		MIME_TYPES.put("mj2", "video/mj2");
		MIME_TYPES.put("mjp2", "video/mj2");
		MIME_TYPES.put("mpa", "video/mpeg");
		MIME_TYPES.put("ogv", "video/ogg");
		MIME_TYPES.put("flv", "video/x-flv");
		MIME_TYPES.put("mkv", "video/x-matroska");

	}

	public static int getIconForExt(String ext) {
		final Integer res = EXT_ICONS.get(ext);
		return res == null ? 0 : res.intValue();
	}

	public static int getTypeIcon(File file) {
		final String extension = FilenameUtils.getExtension(file.getName());
		if (extension != null && !extension.isEmpty()) {
			return getIconForExt(extension);
		}
		return 0;
	}

	@Nullable
	public static String getMimeType(File file) {
		if (file.isDirectory()) {
			return null;
		}
		
		String type = null;
		final String extension = FilenameUtils.getExtension(file.getName());
		
		if (extension != null && !extension.isEmpty()) {
			final String extensionLowerCase = extension.toLowerCase(Locale.getDefault());
			final MimeTypeMap mime = MimeTypeMap.getSingleton();
			type = mime.getMimeTypeFromExtension(extensionLowerCase);
			if (type == null) {
				type = MIME_TYPES.get(extensionLowerCase);
			}
		}
		return type;
	}

	public static boolean mimeTypeMatch(String mime, String input) {
		return Pattern.matches(mime.replace("*", ".*"), input);
	}

	public static boolean isPicture(File f) {
		final String mime = getMimeType(f);
		if (mime != null) {
			return mimeTypeMatch("image/*", mime);
		}
		return false;
	}

	public static boolean isVideo(File f) {
		final String mime = getMimeType(f);
		if (mime != null) {
			return mimeTypeMatch("video/*", mime);
		}
		return false;
	}
}
