/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.sar;

import java.io.*;
import java.util.zip.*;

/**
 * Helper class for converting Zips/Jars to Sars and vice versa.
 */
public class SarUtil {

	private static final int BUFFER_SIZE = 8 * 1024;
	private static final boolean DEBUG = SarConstants.DEBUG;

	/**
	 * 
	 */
	private SarUtil() {
		// utility class
	}

	/**
	 * Normalize the given zip/jar.
	 * 
	 * @param zipSource
	 * @param zipTarget
	 * @throws IOException
	 */
	public static void normalize(File zipSource, File zipTarget) throws IOException {
		File tempSar = File.createTempFile("temp", ".sar");
		try {
			zipToSar(zipSource, tempSar);
			sarToZip(tempSar, zipTarget);
		} finally {
			tempSar.delete();
		}
	}

	/**
	 * Normalize the given zip/jar.
	 * 
	 * @param zipSource
	 * @param zipTarget
	 * @throws IOException
	 */
	public static void normalize(InputStream zipSource, OutputStream zipTarget) throws IOException {
		DirectByteArrayOutputStream tempSar = new DirectByteArrayOutputStream();
		zipToSar(zipSource, tempSar);
		sarToZip(tempSar.getInputStream(), zipTarget);
	}

	/**
	 * @param zipFile
	 * @throws IOException
	 */
	public static void zipToSar(File zipFile, File sarFile) throws IOException {
		InputStream zipInputStream = new BufferedInputStream(new FileInputStream(zipFile));
		OutputStream sarOutputStream = new BufferedOutputStream(new FileOutputStream(sarFile));
		SarUtil.zipToSar(zipInputStream, sarOutputStream);
	}

	/**
	 * @param zippedInputStream
	 * @param saredOutputStream
	 * @throws IOException
	 */
	public static void zipToSar(InputStream zippedInputStream, OutputStream saredOutputStream) throws IOException {
		zipToSar(zippedInputStream, true, saredOutputStream, true);
	}

	/**
	 * @param zippedInputStream
	 * @param closeIn
	 * @param saredOutputStream
	 * @param closeOut
	 * @throws IOException
	 */
	public static void zipToSar(InputStream zippedInputStream, boolean closeIn, OutputStream saredOutputStream, boolean closeOut) throws IOException {
		zipToSarNoClose(zippedInputStream, saredOutputStream);

		if (closeIn)
			zippedInputStream.close();
		if (closeOut)
			saredOutputStream.close();
	}

	/**
	 * @param sarFile
	 * @param zipFile
	 * @throws IOException
	 */
	public static void sarToZip(File sarFile, File zipFile) throws IOException {
		InputStream saredInputStream = new BufferedInputStream(new FileInputStream(sarFile));
		OutputStream zippedOutputStream = new BufferedOutputStream(new FileOutputStream(zipFile));

		sarToZip(saredInputStream, zippedOutputStream);
	}

	/**
	 * @param saredInputStream
	 * @param zippedOutputStream
	 * @throws IOException
	 */
	public static void sarToZip(InputStream saredInputStream, OutputStream zippedOutputStream) throws IOException {
		sarToZip(saredInputStream, true, zippedOutputStream, true);
	}

	/**
	 * @param saredInputStream
	 * @param closeIn
	 * @param zippedOutputStream
	 * @param closeOut
	 * @throws IOException
	 */
	public static void sarToZip(InputStream saredInputStream, boolean closeIn, OutputStream zippedOutputStream, boolean closeOut) throws IOException {
		sarToZipNoClose(saredInputStream, zippedOutputStream);

		if (closeIn)
			saredInputStream.close();
		if (closeOut)
			zippedOutputStream.close();
	}

	/**
	 * @param zippedInputStream
	 * @param saredOutputStream
	 * @throws IOException
	 */
	private static void zipToSarNoClose(InputStream zippedInputStream, OutputStream saredOutputStream) throws IOException {

		ZipInputStream zipInputStream = new ZipInputStream(zippedInputStream);
		SarOutputStream sarOutputStream = new SarOutputStream(saredOutputStream);

		ZipEntry zipEntry;
		byte[] buf = new byte[BUFFER_SIZE];
		while ((zipEntry = zipInputStream.getNextEntry()) != null) {
			boolean isZip = isZip(zipEntry);
			SarEntry sarEntry = new SarEntry(zipEntry, isZip);
			sarOutputStream.putNextEntry(sarEntry);
			if (isZip) {
				zipToSarNoClose(zipInputStream, sarOutputStream);
			} else {
				int read;
				while ((read = zipInputStream.read(buf)) != -1) {
					if (DEBUG) {
						System.out.println("Content: " + new String(buf, 0, read));
					}
					sarOutputStream.write(buf, 0, read);
				}
			}
			zipInputStream.closeEntry();
			sarOutputStream.closeEntry();
		}
		sarOutputStream.finish();
	}

	/**
	 * @param saredInputStream
	 * @param zippedOutputStream
	 * @throws IOException
	 */
	private static void sarToZipNoClose(InputStream saredInputStream, OutputStream zippedOutputStream) throws IOException {

		SarInputStream sarInputStream = new SarInputStream(saredInputStream);
		ZipOutputStream zipOutputStream = new ZipOutputStream(zippedOutputStream);

		SarEntry sarEntry;
		byte[] buf = new byte[BUFFER_SIZE];
		while ((sarEntry = sarInputStream.getNextEntry()) != null) {
			ZipEntry zipEntry = new ZipEntry(sarEntry);
			zipOutputStream.putNextEntry(zipEntry);
			if (sarEntry.isZip()) {
				sarToZipNoClose(sarInputStream, zipOutputStream);
			} else {
				int read;
				while ((read = sarInputStream.read(buf)) != -1) {
					if (DEBUG) {
						System.out.println("Content: " + new String(buf, 0, read));
					}
					zipOutputStream.write(buf, 0, read);
				}
			}
			sarInputStream.closeEntry();
			zipOutputStream.closeEntry();
		}

		zipOutputStream.finish();
	}

	private static boolean isZip(ZipEntry zipEntry) {
		String name = zipEntry.getName().toLowerCase();
		return name.endsWith(".zip") || name.endsWith(".jar");
	}
}
