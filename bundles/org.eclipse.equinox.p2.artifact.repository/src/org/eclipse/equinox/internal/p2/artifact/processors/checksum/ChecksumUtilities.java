/*******************************************************************************
 * Copyright (c) 2015, 2018 Mykola Nikishov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5Verifier;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.util.NLS;

public class ChecksumUtilities {

	private static final String ARTIFACT_CHECKSUMS_POINT = "org.eclipse.equinox.p2.artifact.repository.artifactChecksums"; //$NON-NLS-1$
	public static final String MD5 = "md5"; //$NON-NLS-1$

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
	public static Collection<ProcessingStep> getChecksumVerifiers(IArtifactDescriptor descriptor, String property, Set<String> checksumsToSkip) throws IllegalArgumentException {
		Collection<ProcessingStep> steps = new ArrayList<>();
		Map<String, String> checksums = ChecksumHelper.getChecksums(descriptor, property);

		IConfigurationElement[] checksumVerifierConfigurations = getChecksumComparatorConfigurations();

		for (Entry<String, String> checksumEntry : checksums.entrySet()) {
			if (checksumsToSkip.contains(checksumEntry.getKey()))
				continue;

			for (IConfigurationElement checksumVerifierConfiguration : checksumVerifierConfigurations) {
				String checksumId = checksumVerifierConfiguration.getAttribute("id"); //$NON-NLS-1$
				if (checksumEntry.getKey().equals(checksumId)) {
					String checksumAlgorithm = checksumVerifierConfiguration.getAttribute("algorithm"); //$NON-NLS-1$
					ChecksumVerifier checksumVerifier = new ChecksumVerifier(checksumAlgorithm, checksumId, checksumEntry.getValue());
					if (checksumVerifier.getStatus().isOK())
						steps.add(checksumVerifier);
					else
						// TODO log something?
						continue;
				}
			}
		}

		Optional<MD5Verifier> legacyMd5Verifier = getLegacyMd5Verifier(descriptor, property);
		legacyMd5Verifier.ifPresent(verifier -> steps.add(verifier));

		return steps;
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
	public static IStatus calculateChecksums(File pathOnDisk, Map<String, String> checksums, Collection<String> checksumsToSkip) {
		// TODO pathOnDisk.getAbsolutePath() || pathOnDisk.getCanonicalPath()
		MultiStatus status = new MultiStatus(Activator.ID, IStatus.OK, NLS.bind(Messages.calculateChecksum_file, pathOnDisk.getAbsolutePath()), null);
		for (IConfigurationElement checksumVerifierConfiguration : ChecksumUtilities.getChecksumComparatorConfigurations()) {
			String id = checksumVerifierConfiguration.getAttribute("id"); //$NON-NLS-1$
			if (checksumsToSkip.contains(id))
				// don't calculate checksum if algo is disabled
				continue;
			String algorithm = checksumVerifierConfiguration.getAttribute("algorithm"); //$NON-NLS-1$
			try {
				String checksum = ChecksumProducer.produce(pathOnDisk, algorithm);
				checksums.put(id, checksum);
				String message = NLS.bind(Messages.calculateChecksum_ok, new Object[] {id, algorithm, checksum});
				status.add(new Status(IStatus.OK, Activator.ID, message));
			} catch (NoSuchAlgorithmException e) {
				String message = NLS.bind(Messages.calculateChecksum_error, id, algorithm);
				status.add(new Status(IStatus.ERROR, Activator.ID, message, e));
			} catch (IOException e) {
				String message = NLS.bind(Messages.calculateChecksum_error, id, algorithm);
				status.add(new Status(IStatus.ERROR, Activator.ID, message, e));
			}
		}
		return status;
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

	private static Optional<MD5Verifier> getLegacyMd5Verifier(IArtifactDescriptor descriptor, String propertyNamespace) {
		String md5 = null;
		switch (propertyNamespace) {
			case IArtifactDescriptor.ARTIFACT_CHECKSUM :
				md5 = descriptor.getProperty(IArtifactDescriptor.ARTIFACT_MD5);
				break;
			case IArtifactDescriptor.DOWNLOAD_CHECKSUM :
				md5 = descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
				break;
			default :
				throw new IllegalArgumentException(propertyNamespace);
		}

		if (md5 != null) {
			@SuppressWarnings("resource") //It's used later so shouldn't be closed
			MD5Verifier checksumVerifier = new MD5Verifier(md5);
			if (checksumVerifier.getStatus().isOK())
				return Optional.of(checksumVerifier);
		}

		return Optional.empty();
	}

	private static void putLegacyMd5Property(String propertyNamespace, Map<String, String> checksums, HashMap<String, String> result) {
		String md5 = checksums.get(ChecksumUtilities.MD5);
		if (md5 != null) {
			if (IArtifactDescriptor.ARTIFACT_CHECKSUM.equals(propertyNamespace))
				result.put(IArtifactDescriptor.ARTIFACT_MD5, md5);
			if (IArtifactDescriptor.DOWNLOAD_CHECKSUM.equals(propertyNamespace))
				result.put(IArtifactDescriptor.DOWNLOAD_MD5, md5);
		}
	}
}
