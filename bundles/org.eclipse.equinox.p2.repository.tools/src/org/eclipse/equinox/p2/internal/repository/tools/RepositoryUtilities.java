/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RepositoryUtilities {

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
			while ((read = fis.read()) != -1) {
				md5Checker.update((byte) read);
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
