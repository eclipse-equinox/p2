/****************************************************************************
 * Copyright (c) 2004, 2007 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.retrieve;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IFileRangeSpecification;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.internal.provider.filetransfer.Activator;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.ecf.provider.filetransfer.identity.FileTransferNamespace;

/**
 * Multi protocol handler for retrieve file transfer. Multiplexes between Apache
 * httpclient 3.0.1-based file retriever and the URLConnection-based file
 * retriever.
 */
public class MultiProtocolRetrieveAdapter implements IRetrieveFileTransfer {

	IConnectContext connectContext = null;
	Proxy proxy = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#getRetrieveNamespace()
	 */
	public Namespace getRetrieveNamespace() {
		return IDFactory.getDefault().getNamespaceByName(FileTransferNamespace.PROTOCOL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setConnectContextForAuthentication(org.eclipse.ecf.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#setProxy(org.eclipse.ecf.core.util.Proxy)
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID,
	 *      org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(IFileID remoteFileID, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException {

		Assert.isNotNull(remoteFileID);
		Assert.isNotNull(transferListener);

		String protocol = null;
		try {
			protocol = remoteFileID.getURI().getScheme();
		} catch (URISyntaxException e) {
			try {
				protocol = remoteFileID.getURL().getProtocol();
			} catch (final MalformedURLException e1) {
				throw new IncomingFileTransferException(Messages.AbstractRetrieveFileTransfer_MalformedURLException);
			}
		}

		IRetrieveFileTransferContainerAdapter fileTransfer = Activator.getDefault().getFileTransfer(protocol);

		// We will default to JRE-provided file transfer if nothing else
		// available
		// for given protocol
		if (fileTransfer == null)
			fileTransfer = new UrlConnectionRetrieveFileTransfer();

		// Set connect context
		fileTransfer.setConnectContextForAuthentication(connectContext);
		// Set Proxy
		fileTransfer.setProxy(proxy);

		// send request using given file transfer protocol
		fileTransfer.sendRetrieveRequest(remoteFileID, transferListener, options);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.filetransfer.IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID,
	 *      org.eclipse.ecf.filetransfer.IFileRangeSpecification,
	 *      org.eclipse.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(IFileID remoteFileID, IFileRangeSpecification rangeSpecification, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException {
		Assert.isNotNull(remoteFileID);
		Assert.isNotNull(transferListener);

		String protocol = null;
		try {
			protocol = remoteFileID.getURI().getScheme();
		} catch (URISyntaxException e) {
			try {
				protocol = remoteFileID.getURL().getProtocol();
			} catch (final MalformedURLException e1) {
				throw new IncomingFileTransferException(Messages.AbstractRetrieveFileTransfer_MalformedURLException);
			}
		}

		IRetrieveFileTransferContainerAdapter fileTransfer = Activator.getDefault().getFileTransfer(protocol);

		// We will default to JRE-provided file transfer if nothing else
		// available
		// for given protocol
		if (fileTransfer == null)
			fileTransfer = new UrlConnectionRetrieveFileTransfer();

		// Set connect context
		fileTransfer.setConnectContextForAuthentication(connectContext);
		// Set Proxy
		fileTransfer.setProxy(proxy);

		// send request using given file transfer protocol
		fileTransfer.sendRetrieveRequest(remoteFileID, rangeSpecification, transferListener, options);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == null)
			return null;
		final IAdapterManager adapterManager = Activator.getDefault().getAdapterManager();
		if (adapterManager == null)
			return null;
		return (T) adapterManager.loadAdapter(this, adapter.getName());
	}

}
