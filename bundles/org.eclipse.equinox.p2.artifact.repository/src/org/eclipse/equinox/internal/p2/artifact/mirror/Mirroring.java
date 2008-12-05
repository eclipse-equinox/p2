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
	private IArtifactRepository baseline;
	private boolean raw;
	private boolean compare = false;
	private IArtifactComparator comparator;
	private String comparatorID;

	private IArtifactComparator getComparator() {
		if (comparator == null)
			comparator = ArtifactComparatorFactory.getArtifactComparator(comparatorID);
		return comparator;
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

	public void setBaseline(IArtifactRepository baseline) {
		this.baseline = baseline;
	}

	public MultiStatus run(boolean failOnError, boolean verbose) {
		if (!destination.isModifiable())
			throw new IllegalStateException(NLS.bind(Messages.exception_destinationNotModifiable, destination.getLocation()));
		if (compare)
			getComparator(); //initialize the comparator. Only needed if we're comparing. Used to force error if comparatorID is invalid.
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
			OutputStream output = null;
			try {
				if (verbose)
					System.out.println("Mirroring: " + descriptor.getArtifactKey() + " (Descriptor: " + descriptor + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (compare && baseline != null)
					if (baseline.contains(descriptor)) {
						// we have to create an output stream based on the descriptor found in the baseline otherwise all
						// the properties will be copied over from the wrong descriptor and our repository will be inconsistent.
						IArtifactDescriptor baselineDescriptor = null;
						IArtifactDescriptor[] baselineDescriptors = baseline.getArtifactDescriptors(descriptor.getArtifactKey());
						for (int i = 0; baselineDescriptor == null && i < baselineDescriptors.length; i++) {
							if (baselineDescriptors[i].equals(descriptor))
								baselineDescriptor = baselineDescriptors[i];
						}

						// if we found a descriptor in the baseline then we'll use it to copy the artifact
						if (baselineDescriptor != null) {
							MultiStatus status = new MultiStatus(Activator.ID, IStatus.OK, "Compare and download of " + descriptor + " from baseline", null);
							//Compare source against baseline
							status.add(getComparator().compare(baseline, baselineDescriptor, source, descriptor));
							try {
								output = destination.getOutputStream(baselineDescriptor);
								//download artifact from baseline
								status.add(baseline.getRawArtifact(baselineDescriptor, output, new NullProgressMonitor()));
								return status;
							} catch (ProvisionException e) {
								if (e.getStatus().getCode() == ProvisionException.ARTIFACT_EXISTS)
									return compareToDestination(baselineDescriptor, e);
								//if exception is unexpected, propagate it.
								throw e;
							}
						}
					}

				output = destination.getOutputStream(newDescriptor);
				return source.getRawArtifact(descriptor, output, new NullProgressMonitor());
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		} catch (ProvisionException e) {
			//This code means the artifact already exists in the target. This is expected.
			if (e.getStatus().getCode() == ProvisionException.ARTIFACT_EXISTS) {
				if (compare)
					return compareToDestination(descriptor, e);
				String message = NLS.bind(Messages.mirror_alreadyExists, descriptor, destination);
				return new Status(IStatus.INFO, Activator.ID, ProvisionException.ARTIFACT_EXISTS, message, e);
			}
			return e.getStatus();
		}
	}

	/**
	 * Takes an IArtifactDescriptor descriptor and the ProvisionException that was thrown when destination.getOutputStream(descriptor)
	 * and compares descriptor to the duplicate descriptor in the destination.
	 * 
	 * Callers should verify the ProvisionException was thrown due to the artifact existing in the destination before invoking this method.
	 * @param descriptor
	 * @param e
	 * @return the status of the compare
	 */
	private IStatus compareToDestination(IArtifactDescriptor descriptor, ProvisionException e) {
		IArtifactDescriptor[] destDescriptors = destination.getArtifactDescriptors(descriptor.getArtifactKey());
		IArtifactDescriptor destDescriptor = null;
		for (int i = 0; destDescriptor == null && i < destDescriptors.length; i++) {
			if (destDescriptors[i].equals(descriptor))
				destDescriptor = destDescriptors[i];
		}
		if (destDescriptor == null)
			return new Status(IStatus.INFO, Activator.ID, ProvisionException.ARTIFACT_EXISTS, Messages.Mirroring_NO_MATCHING_DESCRIPTOR, e);
		return getComparator().compare(source, descriptor, destination, destDescriptor);
	}
}
