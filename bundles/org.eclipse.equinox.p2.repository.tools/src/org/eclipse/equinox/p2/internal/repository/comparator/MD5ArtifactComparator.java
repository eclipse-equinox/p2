/*******************************************************************************
 *  Copyright (c) 2008, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.comparator;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.tools.comparator.IArtifactComparator;
import org.eclipse.osgi.util.NLS;

/**
 * A comparator that compares two artifacts by checking the MD5 checksum
 * recorded in the artifact descriptor. This comparator doesn't actually compute MD5
 * checksums directly.
 */
public class MD5ArtifactComparator implements IArtifactComparator {

	public static String MD5_COMPARATOR_ID = "org.eclipse.equinox.artifact.md5.comparator"; //$NON-NLS-1$

	public IStatus compare(IArtifactRepository source, IArtifactDescriptor sourceDescriptor, IArtifactRepository destination, IArtifactDescriptor destDescriptor) {
		String sourceMD5 = sourceDescriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
		String destMD5 = destDescriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);

		if (sourceMD5 == null && destMD5 == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noMD5Infomation, sourceDescriptor));

		if (sourceMD5 == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noMD5InRepository, source, sourceDescriptor));

		if (destMD5 == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noMD5InRepository, destination, destDescriptor));

		if (sourceMD5.equals(destMD5))
			return Status.OK_STATUS;

		return new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.warning_differentMD5, new Object[] {URIUtil.toUnencodedString(sourceDescriptor.getRepository().getLocation()), URIUtil.toUnencodedString(destDescriptor.getRepository().getLocation()), sourceDescriptor}));
	}
}
