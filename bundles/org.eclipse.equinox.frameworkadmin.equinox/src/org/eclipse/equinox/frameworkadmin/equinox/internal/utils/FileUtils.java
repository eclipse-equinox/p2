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
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils;

import java.io.*;

public class FileUtils {

	public static String getRealLocation(final String location) {
		String ret = location;
		if (location.startsWith("reference:"))
			ret = location.substring("reference:".length());
		if (location.startsWith("initial@"))
			ret = location.substring("initial@".length());
		if (ret == location)
			return ret;
		return getRealLocation(ret);
	}

	public static boolean copy(File source, File target) throws IOException {
		//try {
			target.getParentFile().mkdirs();
			target.createNewFile();
			transferStreams(new FileInputStream(source), new FileOutputStream(target));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			return false;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
		return true;
	}

	/**
	 * Transfers all available bytes from the given input stream to the given
	 * output stream. Regardless of failure, this method closes both streams.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				e.printStackTrace();
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				e.printStackTrace();// ignore
			}
		}
	}
}
