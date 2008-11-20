/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - various ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.osgi.util.NLS;

/**
 * A utility class that performs mirroring of artifacts between repositories.
 */
public class Mirroring {
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private boolean raw;
	private boolean compare = false;
	private IArtifactComparator comparator;
	private String comparatorID = null;

	private static final String comparatorPoint = "org.eclipse.equinox.p2.artifact.repository.comparators"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	private IArtifactComparator getComparator() {
		if (comparator == null)
			comparator = computeComparators();
		return comparator;
	}

	private IArtifactComparator computeComparators() {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(comparatorPoint);

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

	public Mirroring(IArtifactRepository source, IArtifactRepository destination, boolean raw) {
		this.source = source;
		this.destination = destination;
		this.raw = raw;
	}

	public void setCompare(boolean compare) {
		this.compare = compare;
	}

	public void setComparatorId(String id) {
		this.comparatorID = id;
	}

	public MultiStatus run(boolean failOnError, boolean verbose) {
		if (!destination.isModifiable())
			throw new IllegalStateException(NLS.bind(Messages.exception_destinationNotModifiable, destination.getLocation()));
		if (compare)
			getComparator(); //initialize the comparator. Only needed if we're comparing.
		IArtifactKey[] keys = source.getArtifactKeys();
		MultiStatus multiStatus = new MultiStatus(Activator.ID, IStatus.OK, Messages.message_mirroringStatus, null);
		for (int i = 0; i < keys.length; i++) {
			IArtifactKey key = keys[i];
			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(key);
			for (int j = 0; j < descriptors.length; j++) {
				multiStatus.add(mirror(descriptors[j], verbose));
				//stop mirroring as soon as we have an error
				if (failOnError && multiStatus.getSeverity() == IStatus.ERROR)
					return multiStatus;
			}
		}
		return multiStatus;
	}

	private IStatus mirror(IArtifactDescriptor descriptor, boolean verbose) {
		IArtifactDescriptor newDescriptor = raw ? descriptor : new ArtifactDescriptor(descriptor);
		try {
			OutputStream repositoryStream = null;
			try {
				if (verbose)
					System.out.println("Mirroring: " + descriptor.getArtifactKey() + " (Descriptor: " + descriptor + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				repositoryStream = destination.getOutputStream(newDescriptor);
				return source.getRawArtifact(descriptor, repositoryStream, new NullProgressMonitor());
			} finally {
				if (repositoryStream != null) {
					try {
						repositoryStream.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		} catch (ProvisionException e) {
			//This code means the artifact already exists in the target. This is expected.
			if (e.getStatus().getCode() == ProvisionException.ARTIFACT_EXISTS) {
				String message = NLS.bind(Messages.mirror_alreadyExists, descriptor, destination);
				if (!compare)
					return new Status(IStatus.INFO, Activator.ID, ProvisionException.ARTIFACT_EXISTS, message, e);
				return getComparator().compare(source, descriptor, destination, newDescriptor);

			}
			return e.getStatus();
		}
	}
}
