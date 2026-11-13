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

package org.eclipse.equinox.internal.p2.transport.ecf.provider.filetransfer.retrieve;

import java.net.URI;
import java.util.Map;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.equinox.internal.p2.transport.ecf.core.security.IConnectContext;
import org.eclipse.equinox.internal.p2.transport.ecf.core.util.Proxy;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileRangeSpecification;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.equinox.internal.p2.transport.ecf.internal.provider.filetransfer.Activator;

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
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRetrieveFileTransfer#setConnectContextForAuthentication(org.eclipse.equinox.internal.p2.transport.ecf.core.security.IConnectContext)
	 */
	public void setConnectContextForAuthentication(IConnectContext connectContext) {
		this.connectContext = connectContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRetrieveFileTransfer#setProxy(org.eclipse.equinox.internal.p2.transport.ecf.core.util.Proxy)
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRetrieveFileTransfer#sendRetrieveRequest(java.net.URI,
	 *      org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(URI remoteFileID, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException {

		Assert.isNotNull(remoteFileID);
		Assert.isNotNull(transferListener);

		String protocol = remoteFileID.getScheme();

		IRetrieveFileTransfer fileTransfer = Activator.getDefault().getFileTransfer(protocol);

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
	 * @see org.eclipse.equinox.p2.transport.ecf.filetransfer.service.IRetrieveFileTransfer#sendRetrieveRequest(java.net.URI,
	 *      org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileRangeSpecification,
	 *      org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener, java.util.Map)
	 */
	public void sendRetrieveRequest(URI remoteFileID, IFileRangeSpecification rangeSpecification, IFileTransferListener transferListener, Map options) throws IncomingFileTransferException {
		Assert.isNotNull(remoteFileID);
		Assert.isNotNull(transferListener);

		String protocol = remoteFileID.getScheme();

		IRetrieveFileTransfer fileTransfer = Activator.getDefault().getFileTransfer(protocol);

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
