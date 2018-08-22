/*******************************************************************************
 *  Copyright (c) 2015, 2018 Mykola Nikishov.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.tools.comparator.IArtifactComparator;
import org.eclipse.osgi.util.NLS;

/**
 * A comparator that compares two artifacts by checking the checksum
 * recorded in the artifact descriptor. This comparator doesn't actually compute
 * checksums directly.
 */
final public class ArtifactChecksumComparator implements IArtifactComparator {
	final public static String COMPARATOR_ID = "org.eclipse.equinox.artifact.comparator.checksum"; //$NON-NLS-1$

	final private String name;
	final private String id;

	/**
	 * @param checksumId
	 * @param checksumName human-readable name of the checksum algorithm
	 */
	public ArtifactChecksumComparator(String checksumId, String checksumName) {
		this.name = checksumName;
		this.id = checksumId;
	}

	@Override
	final public IStatus compare(IArtifactRepository source, IArtifactDescriptor sourceDescriptor, IArtifactRepository destination, IArtifactDescriptor destDescriptor) {
		String sourceChecksum = ChecksumHelper.getChecksums(sourceDescriptor, IArtifactDescriptor.DOWNLOAD_CHECKSUM).get(id);
		String destChecksum = ChecksumHelper.getChecksums(destDescriptor, IArtifactDescriptor.DOWNLOAD_CHECKSUM).get(id);

		if (sourceChecksum == null && destChecksum == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noChecksumInfomation, name, sourceDescriptor));

		if (sourceChecksum == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noChecksumInRepository, new Object[] {source, name, sourceDescriptor}));

		if (destChecksum == null)
			return new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.info_noChecksumInRepository, new Object[] {destination, name, destDescriptor}));

		if (sourceChecksum.equals(destChecksum))
			return Status.OK_STATUS;

		return new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.warning_different_checksum, new Object[] {URIUtil.toUnencodedString(sourceDescriptor.getRepository().getLocation()), URIUtil.toUnencodedString(destDescriptor.getRepository().getLocation()), name, sourceDescriptor}));
	}

}
