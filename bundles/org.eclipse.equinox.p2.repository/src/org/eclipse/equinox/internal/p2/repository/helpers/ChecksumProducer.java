/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - do not read data multiple times
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.repository.helpers;

import java.io.*;
import java.security.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.osgi.util.NLS;

/**
 * Calculates a checksum using {@link java.security.MessageDigest}
 */
public class ChecksumProducer {

	private static final int BUFFER_SIZE = 4 * 1024;

	private final String id;
	private final String algorithm;
	private final String providerName;

	private MessageDigest messageDigest;

	public ChecksumProducer(String id, String algorithm, String providerName) {
		this.id = id;
		this.algorithm = algorithm;
		this.providerName = providerName;
	}

	public MessageDigest getMessageDigest() throws GeneralSecurityException {
		if (messageDigest == null) {
			messageDigest = getMessageDigest(algorithm, providerName);
		}
		return messageDigest;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public String getProviderName() {
		return providerName;
	}

	public String getId() {
		return id;
	}

	/**
	 * @param file should not be <code>null</code>
	 * @return MD5 checksum of the file or <code>null</code> in case of NoSuchAlgorithmException
	 * @throws IOException
	 */
	@Deprecated
	// bug #509401 - still here to not break x-friends like in bug #507193
	public static String computeMD5(File file) throws IOException {
		try {
			return produce(file, "MD5", null); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			return null;
		}
	}

	/**
	 * @param file should not be <code>null</code>
	 * @param algorithm {@link java.security.MessageDigest#getInstance(String)}
	 * @param providerName {@link Provider#getName()}
	 * @return checksum of the file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @see {@link java.security.MessageDigest#getInstance(String, Provider)}
	 */
	public static String produce(File file, String algorithm, String providerName) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		MessageDigest messageDigest = getMessageDigest(algorithm, providerName);
		try (InputStream fis = new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), messageDigest)) {
			byte[] buffer = new byte[BUFFER_SIZE];
			while (fis.read(buffer) != -1) {
				// consume stream to update digest
			}
			byte[] digest = messageDigest.digest();
			return ChecksumHelper.toHexString(digest);
		}
	}

	public static MessageDigest getMessageDigest(String algorithm, String providerName) throws NoSuchAlgorithmException, NoSuchProviderException {
		if (providerName == null)
			return MessageDigest.getInstance(algorithm);

		Provider provider = ServiceHelper.getService(Activator.getContext(), Provider.class, "(providerName=" + providerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (provider == null)
			throw new NoSuchProviderException(NLS.bind(Messages.noSuchProvider, providerName));

		return MessageDigest.getInstance(algorithm, provider);
	}

}
