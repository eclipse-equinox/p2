/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Compeople AG (Stefan Liebig) - various ongoing maintenance
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.tools.comparator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.p2.internal.repository.comparator.*;
import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 */
public class ArtifactComparatorFactory {
	private static final String COMPARATOR_POINT = "org.eclipse.equinox.p2.artifact.repository.artifactComparators"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Find artifact comparator by comparator's extension id, for instance:
	 * <ul>
	 * <li><code>org.eclipse.equinox.p2.repository.tools.jar.comparator</code> for {@link JarComparator}</li>
	 * <li><code>org.eclipse.equinox.artifact.md5.comparator</code> for {@link MD5ArtifactComparator}</li>
	 * </ul>
	 *
	 * For {@link ArtifactChecksumComparator}, there is no static id that could be used directly. Instead, its extension point id <code>org.eclipse.equinox.artifact.comparator.checksum</code> should be concatenated with id of desired checksum algorithm:
	 * <ul>
	 * <li><code>org.eclipse.equinox.artifact.comparator.checksum.sha-256</code> to use artifact checksum comparator with SHA-256 message digest</li>
	 * <li><code>org.eclipse.equinox.artifact.comparator.checksum.tiger</code> to use Tiger message digest</li>
	 * </ul>
	 *
	 * @param comparatorID id of the extension contributed to <code>org.eclipse.equinox.p2.artifact.repository.artifactComparators</code> extension point
	 * @return if found, instance of artifact comparator
	 * @throws {@link IllegalArgumentException} otherwise
	 */
	public static IArtifactComparator getArtifactComparator(String comparatorID) {
		List<IConfigurationElement> extensions = Stream.of(RegistryFactory.getRegistry().getConfigurationElementsFor(COMPARATOR_POINT))
			.collect(Collectors.toList());
		// exclude artifact checksum comparator, needs special care
		extensions.removeIf(extension -> extension.getAttribute(ATTR_ID).equals(ArtifactChecksumComparator.COMPARATOR_ID));

		// handle generic artifact comparators
		Optional<IArtifactComparator> artifactComparator = findComparatorConfiguration(comparatorID, extensions)
			.map(ArtifactComparatorFactory::createArtifactComparator);
		if (artifactComparator.isPresent())
			return artifactComparator.get();

		// handle artifact checksum comparator
		if (comparatorID != null && comparatorID.startsWith(ArtifactChecksumComparator.COMPARATOR_ID)) {
			Optional<ArtifactChecksumComparator> checksumComparator = getChecksumComparator(comparatorID);
			if (checksumComparator.isPresent())
				return checksumComparator.get();
		}

		if (comparatorID != null) {
			Stream<String> checksumComparators = Stream.of(ChecksumUtilities.getChecksumComparatorConfigurations())
				.map(element -> element.getAttribute(CHECKSUM_ID))
				.map(checksumId -> ArtifactChecksumComparator.COMPARATOR_ID.concat(".").concat(checksumId)); //$NON-NLS-1$
			Stream<String> comparators = extensions.stream()
				.map(extension -> extension.getAttribute(ATTR_ID));
			String availableComparators = Stream.concat(comparators, checksumComparators).collect(Collectors.joining(", ")); //$NON-NLS-1$
			throw new IllegalArgumentException(NLS.bind(Messages.exception_comparatorNotFound, comparatorID, availableComparators));
		}
		throw new IllegalArgumentException(Messages.exception_noComparators);
	}

	private static Optional<IConfigurationElement> findComparatorConfiguration(String comparatorID, List<IConfigurationElement> extensions) {
		if (comparatorID == null && extensions.size() > 0) {
			return Optional.ofNullable(extensions.get(0)); //just take the first one
		}

		return extensions.stream().filter(extension -> extension.getAttribute(ATTR_ID).equals(comparatorID)).findAny();
	}

	private static IArtifactComparator createArtifactComparator(IConfigurationElement element) {
		try {
			Object execExt = element.createExecutableExtension(ATTR_CLASS);
			if (execExt instanceof IArtifactComparator)
				return (IArtifactComparator) execExt;
		} catch (Exception e) {
			//fall through
		}
		return null;
	}

	private static final String CHECKSUM_ID = "id"; //$NON-NLS-1$
	private static final String CHECKSUM_ALGORITHM = "algorithm"; //$NON-NLS-1$

	private static Optional<ArtifactChecksumComparator> getChecksumComparator(String comparatorId) {
		boolean hasNoChecksumId = !comparatorId.startsWith(ArtifactChecksumComparator.COMPARATOR_ID.concat(".")); //$NON-NLS-1$
		if (hasNoChecksumId)
			return Optional.empty();

		String comparatorChecksum = Optional.ofNullable(comparatorId.substring(ArtifactChecksumComparator.COMPARATOR_ID.length() + 1))
			.orElse(""); //$NON-NLS-1$
		if (comparatorChecksum.isEmpty())
			return Optional.empty();

		IConfigurationElement[] comparatorConfigurations = ChecksumUtilities.getChecksumComparatorConfigurations();
		Optional<IConfigurationElement> comparator = Stream.of(comparatorConfigurations)
			.filter(config -> config.getAttribute(CHECKSUM_ID).equals(comparatorChecksum))
			.findAny();

		return comparator.map(c -> {
			String checksumName = c.getAttribute(CHECKSUM_ALGORITHM);
			String checksumId = c.getAttribute(CHECKSUM_ID);
			return Optional.of(new ArtifactChecksumComparator(checksumId, checksumName));
		}).orElse(Optional.empty());
	}
}
