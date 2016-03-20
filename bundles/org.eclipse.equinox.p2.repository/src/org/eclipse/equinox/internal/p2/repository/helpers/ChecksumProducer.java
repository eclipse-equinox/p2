/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mykola Nikishov - extract MD5 checksum calculation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumProducer {

	public static String computeMD5(File file) {
		if (file == null || file.isDirectory() || !file.exists())
			return null;
		MessageDigest md5Checker;
		try {
			md5Checker = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		InputStream fis = null;
		try {
			fis = new BufferedInputStream(new FileInputStream(file));
			int read = -1;
			final int bufferSize = 4 * 1024;
			byte[] buffer = new byte[bufferSize];
			while ((read = fis.read(buffer, 0, bufferSize)) != -1) {
				md5Checker.update(buffer, 0, read);
			}
			byte[] digest = md5Checker.digest();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				if ((digest[i] & 0xFF) < 0x10)
					buf.append('0');
				buf.append(Integer.toHexString(digest[i] & 0xFF));
			}
			return buf.toString();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

}
