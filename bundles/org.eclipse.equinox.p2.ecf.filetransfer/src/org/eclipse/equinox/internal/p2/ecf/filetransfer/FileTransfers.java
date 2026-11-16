/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ecf.filetransfer;

import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.IFileTransferProtocolToFactoryMapper;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.browse.MultiProtocolFileSystemBrowserAdapter;
import org.eclipse.equinox.internal.p2.ecf.filetransfer.provider.retrieve.MultiProtocolRetrieveAdapter;

/**
 * Entry point for retrieving and browsing remote files. Every call returns a
 * new adapter which selects the file transfer provider to use based on the
 * protocol of the requested file, so adapters must not be shared between
 * concurrent requests with different credentials.
 */
public final class FileTransfers {

	private FileTransfers() {
	}

	/**
	 * @return a new adapter for retrieving remote files
	 */
	public static IRetrieveFileTransferContainerAdapter newRetrieveAdapter() {
		return new MultiProtocolRetrieveAdapter();
	}

	/**
	 * @return a new adapter for browsing remote file systems, e.g. for querying
	 *         the last modified time of a remote file
	 */
	public static IRemoteFileSystemBrowserContainerAdapter newBrowseAdapter() {
		return new MultiProtocolFileSystemBrowserAdapter();
	}

	/**
	 * @return the mapper which allows to query and change the providers used for
	 *         the different protocols
	 */
	public static IFileTransferProtocolToFactoryMapper getProtocolToFactoryMapper() {
		return org.eclipse.equinox.internal.p2.ecf.filetransfer.internal.Activator.getDefault();
	}
}
