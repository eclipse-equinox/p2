/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository.artifact;

import org.eclipse.equinox.p2.metadata.IArtifactKey;

import java.io.OutputStream;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * A repository containing artifacts.
 * <p>
 * This interface is not intended to be implemented by clients.  Artifact repository
 * implementations must subclass {@link AbstractArtifactRepository} rather than 
 * implementing this interface directly.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IArtifactRepository extends IRepository {

	/**
	 * See {@link IQuery#getProperty(String)}.  A query should implement "getExcludeArtifactKeys" returning
	 * Boolean.TRUE to indicate that it is not interested in IArtifactKeys
	 */
	public static final String QUERY_EXCLUDE_KEYS = "ExcludeArtifactKeys"; //$NON-NLS-1$

	/**
	 * See {@link IQuery#getProperty(String)}.  A query should implement "getExcludeArtifactDescriptors" returning
	 * Boolean.TRUE to indicate that it is not interested in IArtifactKeys
	 */
	public static final String QUERY_EXCLUDE_DESCRIPTORS = "ExcludeArtifactDescriptors"; //$NON-NLS-1$

	/** 
	 * The return code to use when a client could/should retry a failed getArtifact() operation.
	 * For example, the repository may have additional mirrors that could be consulted.
	 */
	public static int CODE_RETRY = 13;

	/**
	 * Create an instance of IArtifactDescriptor based on the given key
	 * @param key
	 * @return a new instanceof of IArtifactDescriptor
	 */
	public IArtifactDescriptor createArtifactDescriptor(IArtifactKey key);

	/**
	 * Add the given descriptor to the set of descriptors in this repository.  This is 
	 * a relatively low-level operation that should be used only when the actual related 
	 * content is in this repository and the given descriptor accurately describes 
	 * that content.
	 * @param descriptor the descriptor to add.
	 */
	public void addDescriptor(IArtifactDescriptor descriptor);

	/**
	 * Add the given artifact descriptors to this repository
	 * @param descriptors the artifact descriptors to add
	 */
	public void addDescriptors(IArtifactDescriptor[] descriptors);

	/** 
	 * Returns true if this repository contains the given descriptor.
	 * @param descriptor the descriptor to query
	 * @return true if the given descriptor is already in this repository
	 */
	public boolean contains(IArtifactDescriptor descriptor);

	/** 
	 * Returns true if this repository contains the given artifact key.
	 * @param key the key to query
	 * @return true if the given key is already in this repository
	 */
	public boolean contains(IArtifactKey key);

	/**
	 * Write to the given output stream the bytes represented by the artifact descriptor processed by the processing steps of the given descriptor.
	 */
	public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	/**
	 * Write to the given output stream the bytes represented by the artifact descriptor without processing by the steps of the given descriptor. 
	 */
	public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor);

	/**
	 * Return the set of artifact descriptors describing the ways that this repository
	 * can supply the artifact associated with the given artifact key
	 * @param key the artifact key to lookup
	 * @return the descriptors associated with the given key
	 */
	public IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key);

	/**
	 * Executes the given artifact requests on this byte server.
	 * @param requests The artifact requests
	 * @param monitor
	 * @return a status object that is <code>OK</code> if requests were
	 * processed successfully. Otherwise, a status indicating information,
	 * warnings, or errors that occurred while executing the artifact requests
	 */
	public IStatus getArtifacts(IArtifactRequest[] requests, IProgressMonitor monitor);

	/**
	 * Open an output stream to which a client can write the data for the given 
	 * artifact descriptor.
	 * @param descriptor the descriptor describing the artifact data to be written to the 
	 * resultant stream
	 * @return the stream to which the artifact content can be written. The returned output
	 *  stream may implement <code>IStateful</code>.
	 * @throws ProvisionException if the output stream could not be created.  Reasons include:
	 * <ul>
	 * <li>An I/O exception occurred (@link {@link ProvisionException#REPOSITORY_FAILED_WRITE}) .</li>
	 * <li>An artifact already exists at that location ({@link ProvisionException#ARTIFACT_EXISTS}).</li>
	 * </ul>
	 */
	public OutputStream getOutputStream(IArtifactDescriptor descriptor) throws ProvisionException;

	/**
	 * Remove the all keys, descriptors, and contents from this repository.
	 */
	public void removeAll();

	/**
	 * Remove the given descriptor and its corresponding content in this repository.  
	 * @param descriptor the descriptor to remove.
	 */
	public void removeDescriptor(IArtifactDescriptor descriptor);

	/**
	 * Remove the given key and all related content and descriptors from this repository.  
	 * @param key the key to remove.
	 */
	public void removeDescriptor(IArtifactKey key);

}
