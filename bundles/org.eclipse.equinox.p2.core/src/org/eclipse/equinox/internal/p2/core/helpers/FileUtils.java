/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.*;
import java.net.URL;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

public class FileUtils {

	/**
	 * Unzip from a File to an output directory.
	 */
	public static void unzipFile(File zipFile, File outputDir) throws IOException {
		InputStream in = new FileInputStream(zipFile);
		try {
			unzipStream(in, zipFile.length(), outputDir, null, null);
		} catch (IOException e) {
			// add the file name to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from a File to an output directory, with progress indication.
	 * monitor may be null.
	 */
	public static void unzipFile(File zipFile, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		InputStream in = new FileInputStream(zipFile);
		try {
			unzipStream(in, zipFile.length(), outputDir, taskName, monitor);
		} catch (IOException e) {
			// add the file name to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipFile, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from a URL to an output directory, with progress indication.
	 * monitor may be null.
	 */
	public static void unzipURL(URL zipURL, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		int size = zipURL.openConnection().getContentLength();
		InputStream in = zipURL.openStream();
		try {
			unzipStream(in, size, outputDir, taskName, monitor);
		} catch (IOException e) {
			// add the URL to the message
			throw new IOException(NLS.bind(Messages.Util_Error_Unzipping, zipURL, e.getMessage()));
		} finally {
			in.close();
		}
	}

	/**
	 * Unzip from an InputStream to an output directory.
	 */
	public static void unzipStream(InputStream stream, long size, File outputDir, String taskName, IProgressMonitor monitor) throws IOException {
		InputStream is = monitor == null ? stream : stream; // new ProgressMonitorInputStream(stream, size, size, taskName, monitor); TODO Commented code
		ZipInputStream in = new ZipInputStream(new BufferedInputStream(is));
		ZipEntry ze = in.getNextEntry();
		if (ze == null) {
			// There must be at least one entry in a zip file.
			// When there isn't getNextEntry returns null.
			in.close();
			throw new IOException(Messages.Util_Invalid_Zip_File_Format);
		}
		do {
			File outFile = new File(outputDir, ze.getName());
			if (ze.isDirectory()) {
				outFile.mkdirs();
			} else {
				if (outFile.exists()) {
					outFile.delete();
				} else {
					outFile.getParentFile().mkdirs();
				}
				try {
					copyStream(in, false, new FileOutputStream(outFile), true);
				} catch (FileNotFoundException e) {
					// TEMP: ignore this for now in case we're trying to replace
					// a running eclipse.exe
				}
				outFile.setLastModified(ze.getTime());
			}
			in.closeEntry();
		} while ((ze = in.getNextEntry()) != null);
		in.close();
	}

	// Delete empty directories under dir, including dir itself.
	public static void deleteEmptyDirs(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i += 1) {
				deleteEmptyDirs(files[i]);
			}
			dir.getCanonicalFile().delete();
		}
	}

	// Delete the given file whether it is a file or a directory
	public static void deleteAll(File file) {
		if (!file.exists())
			return;
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++)
					deleteAll(files[i]);
		}
		file.delete();
	}

	/**
	 * Copy an input stream to an output stream.
	 * Optionally close the streams when done.
	 * Return the number of bytes written.
	 */
	public static int copyStream(InputStream in, boolean closeIn, OutputStream out, boolean closeOut) throws IOException {
		try {
			int written = 0;
			byte[] buffer = new byte[16 * 1024];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
				written += len;
			}
			return written;
		} finally {
			try {
				if (closeIn) {
					in.close();
				}
			} finally {
				if (closeOut) {
					out.close();
				}
			}
		}
	}

	public static void zip(File[] sourceFiles, File destinationArchive) throws IOException {
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destinationArchive));
		try {
			for (int i = 0; i < sourceFiles.length; i++)
				if (sourceFiles[i].isDirectory())
					zipDir(output, sourceFiles[i], new Path(sourceFiles[i].getName()));
				else
					zipFile(output, sourceFiles[i], new Path(""));//$NON-NLS-1$
		} finally {
			try {
				// Note! This call will fail miserably if no entries were added to the zip!
				// The file is left open after an exception is thrown.
				output.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/*
	 * Zip the contents of the given directory into the zip file represented by
	 * the given zip stream. Prepend the given prefix to the file paths.
	 */
	private static void zipDir(ZipOutputStream output, File source, IPath prefix) {
		File[] files = source.listFiles();
		for (int i = 0; i < files.length; i++) {
			try {
				if (files[i].isFile())
					zipFile(output, files[i], prefix);
				else
					zipDir(output, files[i], prefix.append(files[i].getName()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Add the given file to the zip file represented by the specified stream.
	 * Prepend the given prefix to the path of the file.
	 */
	private static void zipFile(ZipOutputStream output, File sourceFile, IPath prefix) throws IOException {
		InputStream input = new FileInputStream(sourceFile);
		try {
			ZipEntry zipEntry = new ZipEntry(prefix.append(sourceFile.getName()).toString());
			zipEntry.setTime(sourceFile.lastModified());
			output.putNextEntry(zipEntry);
			copyStream(input, true, output, false);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				output.closeEntry();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
