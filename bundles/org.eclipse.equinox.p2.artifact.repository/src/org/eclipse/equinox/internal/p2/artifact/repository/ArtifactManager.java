/*******************************************************************************
 *  Copyright (c) 2025 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.OutputStream;
import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The artifact manager is responsible for managing artifacts on a per agent
 * basis. It possibly caches the data from previous requests to improve
 * performance or knows alternative locations to fetch the artifacts.
 */
@ProviderType
public interface ArtifactManager {

	/**
	 * Service name for the artifact manager service.
	 */
	String SERVICE_NAME = "org.eclipse.equinox.internal.p2.repository.ArtifactManager"; //$NON-NLS-1$

	/**
	 * Acquire the artifact described by the given artifact descriptor and writing
	 * it into the target output stream. Progress is reported on the monitor. If the
	 * <code>target</code> is an instance of {@link IStateful} the resulting status
	 * is also reported on the target.
	 *
	 * @return IStatus that is a {@link DownloadStatus} if the artifact was
	 *         downloaded from a remote server, or a plain status in other cases
	 *         (including errors).
	 * @param source     An URI of file to download from a remote, this might be a
	 *                   mirror of the actual artifact repository
	 * @param target     the {@link OutputStream} where result is written
	 * @param descriptor the descriptor of the artifact that is about to be
	 *                   downloaded
	 * @param monitor    where progress should be reported, might be
	 *                   <code>null</code> if no progress reporting is desired
	 */
	IStatus getArtifact(URI source, OutputStream target, IArtifactDescriptor descriptor, IProgressMonitor monitor);
}
