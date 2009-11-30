/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

import java.util.Map;

public interface IArtifactDescriptor {

	public static final String DOWNLOAD_SIZE = "download.size"; //$NON-NLS-1$
	public static final String ARTIFACT_SIZE = "artifact.size"; //$NON-NLS-1$
	public static final String DOWNLOAD_MD5 = "download.md5"; //$NON-NLS-1$
	public static final String DOWNLOAD_CONTENTTYPE = "download.contentType"; //$NON-NLS-1$
	public static final String TYPE_ZIP = "application/zip"; //$NON-NLS-1$
	public static final String ARTIFACT_MD5 = "artifact.md5"; //$NON-NLS-1$
	public static final String FORMAT = "format"; //$NON-NLS-1$

	/**
	 * Return the key for the artifact described by this descriptor.
	 * @return the key associated with this descriptor
	 */
	public abstract IArtifactKey getArtifactKey();

	/**
	 * Return the value of the given property in this descriptor  <code>null</code> 
	 * is returned if no such property exists
	 * @param key the property key to look for
	 * @return the value of the given property or <code>null</code>
	 */
	public abstract String getProperty(String key);

	/**
	 * Returns a read-only collection of the properties of the artifact descriptor.
	 * @return the properties of this artifact descriptor.
	 */
	public Map getProperties();

	/** 
	 * Return the list of processing steps associated with this descriptor.
	 * An empty set of steps implies that this descriptor describes a complete
	 * copy of the artifact in its native form.
	 * @return the list of processing steps for this descriptor
	 */
	public abstract ProcessingStepDescriptor[] getProcessingSteps();

	/**
	 * Return the artifact repository that holds the artifact described by this descriptor.
	 * <code>null</code> is returned if this descriptor is not held in a repository.
	 * 
	 * @return the repository holding this artifact or <code>null</code> if none.
	 */
	public abstract IArtifactRepository getRepository();
}
