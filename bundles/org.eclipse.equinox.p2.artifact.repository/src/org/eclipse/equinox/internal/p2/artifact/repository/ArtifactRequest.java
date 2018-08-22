/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - transport split
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

/**
 * Base class for all requests on an {@link IArtifactRepository}.
 */
public abstract class ArtifactRequest implements IArtifactRequest {
	private static final Status DEFAULT_STATUS = new Status(IStatus.ERROR, Activator.ID, "default"); //$NON-NLS-1$
	protected IArtifactKey artifact;
	protected String resolvedKey;
	protected IArtifactRepository source;
	protected IStatus result = DEFAULT_STATUS;
	protected Transport transport = null;

	public ArtifactRequest(IArtifactKey key, Transport transport) {
		artifact = key;
		this.transport = transport;
	}

	@Override
	public IArtifactKey getArtifactKey() {
		return artifact;
	}

	/**
	 * Returns the result of the previous call to {@link #perform(IArtifactRepository, IProgressMonitor)},
	 * or <code>null</code> if perform has never been called.
	 * 
	 * @return The result of the previous perform call.
	 */
	@Override
	public IStatus getResult() {
		if (result == DEFAULT_STATUS)
			return new Status(IStatus.ERROR, Activator.ID, "No repository found containing: " + getArtifactKey().toString());

		return result;
	}

	protected IArtifactRepository getSourceRepository() {
		return source;
	}

	/**
	 * Performs the artifact request, and sets the result status.
	 * 
	 * @param sourceRepository the repository to download the artifact from 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	@Override
	abstract public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor);

	/**
	 * Sets the result of an invocation of {@link #perform(IArtifactRepository, IProgressMonitor)}.
	 * This method is called by subclasses to set the result status for
	 * this request.
	 * 
	 * @param value The result status
	 */
	protected void setResult(IStatus value) {
		result = value;
	}

	protected void setSourceRepository(IArtifactRepository value) {
		source = value;
	}
}
