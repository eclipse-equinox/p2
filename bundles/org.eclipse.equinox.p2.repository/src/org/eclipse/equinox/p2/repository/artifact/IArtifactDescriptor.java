/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import java.util.Map;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 * An artifact descriptor describes an artifact stored in some artifact repository. The
 * descriptor defines the artifact it contains, as well as any processing steps that
 * must be performed when the artifact is transferred out of the repository (such
 * as decompression, error checking, etc).
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients. Instead subclass the {@link ArtifactDescriptor} class
 * @since 2.0
 */
public interface IArtifactDescriptor {

	/**
	 * An artifact descriptor property (value "download.size") indicating the number
	 * of bytes that will be transferred when this artifact is transferred out of the repository.
	 */
	public static final String DOWNLOAD_SIZE = "download.size"; //$NON-NLS-1$
	/**
	 * An artifact descriptor property (value "artifact.size") indicating the size in
	 * bytes of the artifact in its native format (after processing steps have been applied).
	 */
	public static final String ARTIFACT_SIZE = "artifact.size"; //$NON-NLS-1$
	/**
	 * An artifact descriptor property (value "download.md5") indicating the MD5
	 * checksum of the artifact bytes that are transferred.
	 */
	public static final String DOWNLOAD_MD5 = "download.md5"; //$NON-NLS-1$
	/**
	 * An artifact descriptor property (value "download.contentType") indicating the 
	 * content type of the artifact bytes that are transferred.
	 */
	public static final String DOWNLOAD_CONTENTTYPE = "download.contentType"; //$NON-NLS-1$
	/**
	 * An content type (value "application/zip") indicating the content is a zip file.
	 */
	public static final String TYPE_ZIP = "application/zip"; //$NON-NLS-1$
	/**
	 * An artifact descriptor property (value "artifact.md5") indicating the MD5
	 * checksum of the artifact bytes in its native format (after processing steps have
	 * been applied).
	 */
	public static final String ARTIFACT_MD5 = "artifact.md5"; //$NON-NLS-1$

	/**
	 * An artifact descriptor property (value "format") indicating the storage format
	 * of the artifact in the repository.
	 * @see #FORMAT_PACKED
	 */
	public static final String FORMAT = "format"; //$NON-NLS-1$

	/**
	 * A property value for the {@link #FORMAT} artifact descriptor property (value "packed")
	 * indicating the storage format is using pack200 compression.
	 * @see #FORMAT
	 */
	public static final String FORMAT_PACKED = "packed"; //$NON-NLS-1$

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
	public Map<String, String> getProperties();

	/** 
	 * Return the list of processing steps associated with this descriptor.
	 * An empty set of steps implies that this descriptor describes a complete
	 * copy of the artifact in its native form. If one or more steps are present,
	 * they may be performed when the artifact is transferred from the repository
	 * that contains it.
	 * 
	 * @return the list of processing steps for this descriptor
	 */
	public abstract IProcessingStepDescriptor[] getProcessingSteps();

	/**
	 * Return the artifact repository that holds the artifact described by this descriptor.
	 * <code>null</code> is returned if this descriptor is not held in a repository.
	 * 
	 * @return the repository holding this artifact or <code>null</code> if none.
	 */
	public abstract IArtifactRepository getRepository();
}
