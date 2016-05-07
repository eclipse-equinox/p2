/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *		Compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.tools.comparator;

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

	public static IArtifactComparator getArtifactComparator(String comparatorID) {
		IConfigurationElement[] extensions = RegistryFactory.getRegistry().getConfigurationElementsFor(COMPARATOR_POINT);

		IConfigurationElement element = null;
		if (comparatorID == null && extensions.length > 0) {
			element = extensions[0]; //just take the first one
		} else {
			for (int i = 0; i < extensions.length; i++) {
				if (extensions[i].getAttribute(ATTR_ID).equals(comparatorID)) {
					element = extensions[i];
					break;
				}
			}
		}
		if (element != null) {
			try {
				Object execExt = element.createExecutableExtension(ATTR_CLASS);
				if (execExt instanceof IArtifactComparator)
					return (IArtifactComparator) execExt;
			} catch (Exception e) {
				//fall through
			}
		}

		if (comparatorID != null)
			throw new IllegalArgumentException(NLS.bind(Messages.exception_comparatorNotFound, comparatorID));
		throw new IllegalArgumentException(Messages.exception_noComparators);
	}
}
