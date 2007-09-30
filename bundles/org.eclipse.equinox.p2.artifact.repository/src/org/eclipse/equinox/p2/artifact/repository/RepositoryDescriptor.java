/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.artifact.repository;

import java.net.URI;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.osgi.framework.Version;

public abstract class RepositoryDescriptor {
	/**
	 * Return the type of repository described.
	 * @return the type of repository
	 */
	public abstract String getType();

	/**
	 * Return the version of the repository
	 * @return the version of the repository
	 */
	public abstract Version getVersion();

	/**
	 * The set of transports that can be used to download artifacts from this repository
	 * @return an array of 
	 */
	public abstract String[] getTransports();

	public abstract IArtifactDescriptor getArtifact(IArtifactKey key);

	public abstract URI getBaseURL(); //the url to be used as a base for download
}
