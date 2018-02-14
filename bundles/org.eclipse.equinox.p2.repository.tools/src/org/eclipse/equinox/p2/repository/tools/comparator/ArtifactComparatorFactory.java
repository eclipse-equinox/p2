/*******************************************************************************
 *  Copyright (c) 2008, 2018 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Compeople AG (Stefan Liebig) - various ongoing maintenance
 *     Mykola Nikishov - maintenance and documentation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.tools.comparator;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.osgi.util.NLS;

/**
 * @since 2.0
 */
public class ArtifactComparatorFactory {
	private static final String COMPARATOR_POINT = "org.eclipse.equinox.p2.artifact.repository.artifactComparators"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Find artifact comparator by comparator's extension id.
	 *
	 * @param comparatorID id of the extension contributed to <code>org.eclipse.equinox.p2.artifact.repository.artifactComparators</code> extension point
	 * @return if found, instance of artifact comparator
	 * @throws {@link IllegalArgumentException} otherwise
	 */
	public static IArtifactComparator getArtifactComparator(String comparatorID) {
		List<IConfigurationElement> extensions = Arrays.asList(RegistryFactory.getRegistry().getConfigurationElementsFor(COMPARATOR_POINT));

		Optional<IArtifactComparator> artifactComparator = findComparatorConfiguration(comparatorID, extensions).map(ArtifactComparatorFactory::createArtifactComparator);
		if (artifactComparator.isPresent())
			return artifactComparator.get();

		if (comparatorID != null) {
			String comparators = extensions.stream().map(extension -> extension.getAttribute(ATTR_ID)).collect(Collectors.joining(", ")); //$NON-NLS-1$
			throw new IllegalArgumentException(NLS.bind(Messages.exception_comparatorNotFound, comparatorID, comparators));
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
}
