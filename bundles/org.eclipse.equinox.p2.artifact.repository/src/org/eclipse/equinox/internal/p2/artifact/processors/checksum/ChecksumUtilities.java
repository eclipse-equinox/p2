/*******************************************************************************
 * Copyright (c) 2015, 2022 Mykola Nikishov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mykola Nikishov - initial API and implementation
 *     Christoph Läubrich - do not read data multiple times
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

public class ChecksumUtilities {

	private static final String ARTIFACT_CHECKSUMS_POINT = "org.eclipse.equinox.p2.artifact.repository.artifactChecksums"; //$NON-NLS-1$

	/**
	 * Instances of checksum verifiers applicable for the artifact descriptor
	 *
	 * @param descriptor
	 * @param property either {@link IArtifactDescriptor#ARTIFACT_CHECKSUM} or {@link IArtifactDescriptor#DOWNLOAD_CHECKSUM}
	 * @param checksumsToSkip
	 * @return list of checksum verifiers
	 * @throws IllegalArgumentException if property neither {@link IArtifactDescriptor#ARTIFACT_CHECKSUM} nor {@link IArtifactDescriptor#DOWNLOAD_CHECKSUM}
	 * @see ChecksumHelper#getChecksums(IArtifactDescriptor, String)
	 */
	public static Collection<ChecksumVerifier> getChecksumVerifiers(IArtifactDescriptor descriptor,
			String property, Set<String> checksumsToSkip) throws IllegalArgumentException {
		Collection<ChecksumVerifier> steps = new ArrayList<>();
		Map<String, String> checksums = ChecksumHelper.getChecksums(descriptor, property);

		IConfigurationElement[] checksumVerifierConfigurations = getChecksumComparatorConfigurations();

		for (Entry<String, String> checksumEntry : checksums.entrySet()) {
			if (checksumsToSkip.contains(checksumEntry.getKey()))
				continue;

			for (IConfigurationElement checksumVerifierConfiguration : checksumVerifierConfigurations) {
				String checksumId = checksumVerifierConfiguration.getAttribute("id"); //$NON-NLS-1$
				if (checksumEntry.getKey().equals(checksumId)) {
					String checksumAlgorithm = checksumVerifierConfiguration.getAttribute("algorithm"); //$NON-NLS-1$
					String providerName = checksumVerifierConfiguration.getAttribute("providerName"); //$NON-NLS-1$
					boolean insecure = Boolean.parseBoolean(checksumVerifierConfiguration.getAttribute("warnInsecure")); //$NON-NLS-1$
					int priority = parsePriority(checksumVerifierConfiguration.getAttribute("priority")); //$NON-NLS-1$
					ChecksumVerifier checksumVerifier = new ChecksumVerifier(checksumAlgorithm, providerName,
							checksumId, insecure, priority);
					checksumVerifier.initialize(null, new ProcessingStepDescriptor(null, checksumEntry.getValue(), true), descriptor);
					if (checksumVerifier.getStatus().isOK()) {
						steps.add(checksumVerifier);
					} else {
						LogHelper.log(checksumVerifier.getStatus());
					}
				}
			}
		}

		if (!steps.isEmpty() && steps.stream().allMatch(ChecksumVerifier::isInsecureAlgorithm)) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
					NLS.bind(Messages.onlyInsecureDigestAlgorithmUsed,
							steps.stream().map(ChecksumVerifier::getAlgorithmId).collect(Collectors.joining(",")), //$NON-NLS-1$
							descriptor.getArtifactKey())));
		}

		return steps.stream().max(Comparator.comparing(ChecksumVerifier::getPriority)).stream()
				.collect(Collectors.toList());
	}

	private static int parsePriority(String attribute) {
		if (attribute != null && !attribute.isBlank()) {
			try {
				return Integer.parseInt(attribute);
			} catch (RuntimeException e) {
				// can't use it then... fallback to default
			}
		}
		return 0;
	}

	public static IConfigurationElement[] getChecksumComparatorConfigurations() {
		return RegistryFactory.getRegistry().getConfigurationElementsFor(ARTIFACT_CHECKSUMS_POINT);
	}

	/**
	 * Caller is responsible for checking the returned status and decide if problems are fatal or not.
	 *
	 * @param pathOnDisk file to calculate checksums for
	 * @param checksums calculated checksums
	 * @param checksumsToSkip
	 * @return status
	 */
	public static IStatus calculateChecksums(File pathOnDisk, Map<String, String> checksums,
			Collection<String> checksumsToSkip) {
		MultiStatus status = new MultiStatus(Activator.ID, IStatus.OK,
				NLS.bind(Messages.calculateChecksum_file, pathOnDisk.getAbsolutePath()), null);
		Map<ChecksumProducer, MessageDigest> digestMap = new HashMap<>();
		for (IConfigurationElement checksumVerifierConfiguration : ChecksumUtilities
				.getChecksumComparatorConfigurations()) {
			String id = checksumVerifierConfiguration.getAttribute("id"); //$NON-NLS-1$
			if (checksumsToSkip.contains(id) || !shouldPublish(checksumVerifierConfiguration))
				// don't calculate checksum if algo is disabled
				continue;
			String algorithm = checksumVerifierConfiguration.getAttribute("algorithm"); //$NON-NLS-1$
			String providerName = checksumVerifierConfiguration.getAttribute("providerName"); //$NON-NLS-1$
			try {
				ChecksumProducer producer = new ChecksumProducer(id, algorithm, providerName);
				digestMap.put(producer, producer.getMessageDigest());
			} catch (GeneralSecurityException e) {
				String message = NLS.bind(Messages.calculateChecksum_providerError,
						new Object[] { id, algorithm, providerName });
				status.add(new Status(IStatus.ERROR, Activator.ID, message, e));
			}
		}
		if (digestMap.isEmpty()) {
			return status;
		}
		try {
			// chain all streams together
			InputStream stream = new FileInputStream(pathOnDisk);
			try {
				for (MessageDigest md : digestMap.values()) {
					stream = new DigestInputStream(stream, md);
				}
				// read all bytes and discard them
				stream.transferTo(OutputStream.nullOutputStream());
			} finally {
				stream.close();
			}
			// now all digest contains the required data
			for (var entry : digestMap.entrySet()) {
				ChecksumProducer producer = entry.getKey();
				String checksum = ChecksumHelper.toHexString(entry.getValue().digest());
				String id = producer.getId();
				String message = NLS.bind(Messages.calculateChecksum_ok,
						new Object[] { id, producer.getAlgorithm(), producer.getProviderName(), checksum });
				status.add(new Status(IStatus.OK, Activator.ID, message));
				checksums.put(id, checksum);
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.calculateChecksum_file, pathOnDisk.getAbsolutePath());
			status.add(new Status(IStatus.ERROR, Activator.ID, message, e));
		}
		return status;
	}

	private static boolean shouldPublish(IConfigurationElement checksumVerifierConfiguration) {
		String attribute = checksumVerifierConfiguration.getAttribute("publish"); //$NON-NLS-1$
		if (attribute == null || attribute.isBlank()) {
			return true;
		}
		return Boolean.parseBoolean(attribute);
	}

	/**
	 * @param property either {@link IArtifactDescriptor#ARTIFACT_CHECKSUM} or {@link IArtifactDescriptor#DOWNLOAD_CHECKSUM}
	 * @param checksums
	 */
	public static Map<String, String> checksumsToProperties(String property, Map<String, String> checksums) {
		HashMap<String, String> properties = new HashMap<>();
		for (Entry<String, String> checksum : checksums.entrySet()) {
			properties.put(String.join(".", property, checksum.getKey()), checksum.getValue()); //$NON-NLS-1$
		}

		putLegacyMd5Property(property, checksums, properties);

		return properties;
	}

	private static void putLegacyMd5Property(String propertyNamespace, Map<String, String> checksums, HashMap<String, String> result) {
		String md5 = checksums.get(ChecksumHelper.MD5);
		if (md5 != null) {
			if (IArtifactDescriptor.ARTIFACT_CHECKSUM.equals(propertyNamespace))
				result.put(IArtifactDescriptor.ARTIFACT_MD5, md5);
			if (IArtifactDescriptor.DOWNLOAD_CHECKSUM.equals(propertyNamespace))
				result.put(IArtifactDescriptor.DOWNLOAD_MD5, md5);
		}
	}
}
