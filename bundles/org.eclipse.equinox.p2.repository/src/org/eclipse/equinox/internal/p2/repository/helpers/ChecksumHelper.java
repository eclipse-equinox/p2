/*******************************************************************************
 *  Copyright (c) 2015, 2018 Mykola Nikishov
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.repository.helpers;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class ChecksumHelper {

	public static final String MD5 = "md5"; //$NON-NLS-1$

	/**
	 * @param property either {@link IArtifactDescriptor#ARTIFACT_CHECKSUM} or {@link IArtifactDescriptor#DOWNLOAD_CHECKSUM}
	 * @return (mutable) map of <algorithm,checksum>
	 * @throws IllegalArgumentException if checksum property neither {@link IArtifactDescriptor#ARTIFACT_CHECKSUM} nor {@link IArtifactDescriptor#DOWNLOAD_CHECKSUM}
	 */
	static public Map<String, String> getChecksums(IArtifactDescriptor descriptor, String property) throws IllegalArgumentException {
		if (!IArtifactDescriptor.ARTIFACT_CHECKSUM.equals(property) && !IArtifactDescriptor.DOWNLOAD_CHECKSUM.equals(property))
			// TODO provide more details
			throw new IllegalArgumentException();

		Map<String, String> checksumsByAlgo = new HashMap<>();

		String md5Checksum = getLegacyMd5Checksum(descriptor, property);
		if (md5Checksum != null) {
			checksumsByAlgo.put(MD5, md5Checksum);
		}

		// get checksum properties
		for (Entry<String, String> p : descriptor.getProperties().entrySet()) {
			String key = p.getKey();
			if (key.startsWith(property)) {
				String checksumAlgorithmId = key // "artifact.checksum.sha3"
						.substring(property.length()) // ".sha3"
						.substring(1); // "sha3"
				String checksumValue = Objects.requireNonNull(p.getValue());
				String duplicatedChecksum = checksumsByAlgo.put(checksumAlgorithmId, checksumValue);
				if (duplicatedChecksum != null)
					// should never happen - duplicated checksum
					;
			}
		}

		return checksumsByAlgo;
	}

	/**
	 * @return MD5 checksum from legacy property, either {@link IArtifactDescriptor#DOWNLOAD_MD5} or {@link IArtifactDescriptor#ARTIFACT_MD5}
	 */
	private static String getLegacyMd5Checksum(IArtifactDescriptor descriptor, String property) {
		switch (property) {
			case IArtifactDescriptor.ARTIFACT_CHECKSUM :
				return descriptor.getProperty(IArtifactDescriptor.ARTIFACT_MD5);
			case IArtifactDescriptor.DOWNLOAD_CHECKSUM :
				return descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
			default :
				return null;
		}
	}

	public static String toHexString(byte[] digest) {
		StringBuilder buf = new StringBuilder();
		for (byte element : digest) {
			if ((element & 0xFF) < 0x10)
				buf.append('0');
			buf.append(Integer.toHexString(element & 0xFF));
		}
		return buf.toString();
	}
}
