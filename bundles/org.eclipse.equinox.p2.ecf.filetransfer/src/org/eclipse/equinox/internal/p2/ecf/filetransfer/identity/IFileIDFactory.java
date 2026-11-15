/****************************************************************************
 * Copyright (c) 2010 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.filetransfer.identity;

import java.net.URI;
import java.net.URL;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter;

/**
 * @since 5.0
 */
public interface IFileIDFactory {

	/**
	 * Create an IFileID from a Namespace and a String.
	 * 
	 * @param namespace
	 *            the namespace to use to create the IFileID. Can use
	 *            {@link IRetrieveFileTransferContainerAdapter#getRetrieveNamespace()}.
	 *            Must not be <code>null</code>.
	 * @param remoteFile
	 *            the remote filename to use. Must not be <code>null</code>.
	 * @return IFileID instance. Will not return <code>null</code>.
	 * @throws FileCreateException
	 *             thrown if some problem creating IFileID from given namespace
	 *             and filename
	 */
	public IFileID createFileID(Namespace namespace, URL remoteFile) throws FileCreateException;

	/**
	 * Create an IFileID from a Namespace and a String.
	 * 
	 * @param namespace
	 *            the namespace to use to create the IFileID. Can use
	 *            {@link IRetrieveFileTransferContainerAdapter#getRetrieveNamespace()}.
	 *            Must not be null.
	 * @param remoteFile
	 *            the remote filename to use. Must not be <code>null</code>.
	 * @return IFileID instance. Will not return <code>null</code>.
	 * @throws FileCreateException
	 *             thrown if some problem creating IFileID from given namespace
	 *             and filename
	 */
	public IFileID createFileID(Namespace namespace, String remoteFile) throws FileCreateException;

	/**
	 * Create an IFileID from a Namespace and a String.
	 * 
	 * @param namespace
	 *            the namespace to use to create the IFileID. Can use
	 *            {@link IRetrieveFileTransferContainerAdapter#getRetrieveNamespace()}.
	 *            Must not be null.
	 * @param remoteFile
	 *            the remote resource identifier to use. Must not be <code>null</code>.
	 * @return IFileID instance. Will not return <code>null</code>.
	 * @throws FileCreateException
	 *             thrown if some problem creating IFileID from given namespace
	 *             and filename
	 * @since 5.0
	 */
	public IFileID createFileID(Namespace namespace, URI remoteFile) throws FileCreateException;

	/**
	 * Create an IFileID from a Namespace and a String.
	 * 
	 * @param namespace
	 *            the namespace to use to create the IFileID. Can use
	 *            {@link IRetrieveFileTransferContainerAdapter#getRetrieveNamespace()}.
	 *            Must not be <code>null</code>.
	 * @param arguments
	 *            Object [] of arguments to use to create file ID. These
	 *            arguments will be passed to the
	 *            {@link Namespace#createInstance(Object[])} method of the
	 *            appropriate Namespace setup by the provider
	 * @return IFileID instance. Will not return <code>null</code>.
	 * @throws FileCreateException
	 *             thrown if some problem creating IFileID from given namespace
	 *             and filename
	 */
	public IFileID createFileID(Namespace namespace, Object[] arguments) throws FileCreateException;
}
