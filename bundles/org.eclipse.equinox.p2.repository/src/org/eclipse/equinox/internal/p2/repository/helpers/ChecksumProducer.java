/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.*;
import java.security.*;

/**
 * Calculates a checksum using {@link java.security.MessageDigest}
 */
public class ChecksumProducer {

	private static final int BUFFER_SIZE = 4 * 1024;

	/**
	 * @param file should not be <code>null</code>
	 * @return MD5 checksum of the file or <code>null</code> in case of NoSuchAlgorithmException
	 * @throws IOException
	 */
	@Deprecated
	// bug #509401 - still here to not break x-friends like in bug #507193
	public static String computeMD5(File file) throws IOException {
		try {
			return produce(file, "MD5"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	/**
	 * @param file should not be <code>null</code>
	 * @param algorithm {@link java.security.MessageDigest#getInstance(String)}
	 * @return checksum of the file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String produce(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
		try (InputStream fis = new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), messageDigest)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			while (fis.read(buffer) != -1) {
				// consume stream to update digest
			}
		}
		byte[] digest = messageDigest.digest();
		return ChecksumHelper.toHexString(digest);
	}

}
