/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
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

package org.eclipse.ecf.provider.filetransfer.browse;

import java.io.File;
import java.net.MalformedURLException;
import org.eclipse.ecf.core.util.Proxy;
import org.eclipse.ecf.filetransfer.IRemoteFileSystemListener;
import org.eclipse.ecf.filetransfer.RemoteFileSystemException;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.internal.provider.filetransfer.Messages;
import org.eclipse.osgi.util.NLS;

/**
 * A class for asynchronously browsing a {@link java.io.File}-based filesystem.
 */
public class LocalFileSystemBrowser extends AbstractFileSystemBrowser {

	protected File local;

	/**
	 * @param listener listener
	 * @param directoryID2 remote directory
	 * @throws RemoteFileSystemException if some problem 
	 */
	public LocalFileSystemBrowser(IFileID directoryID2, IRemoteFileSystemListener listener) throws RemoteFileSystemException {
		super(directoryID2, listener, null, null, null);
		try {
			local = new File(directoryID2.getURL().getPath());
		} catch (MalformedURLException e) {
			throw new RemoteFileSystemException(e);
		}
		if (!local.exists())
			throw new RemoteFileSystemException(NLS.bind(Messages.FileSystemBrowser_EXCEPTION_DIRECTORY_DOES_NOT_EXIST, local));
	}

	protected void runRequest() throws Exception {
		if (local.isDirectory()) {
			File[] files = local.listFiles();
			remoteFiles = new LocalRemoteFile[files.length];
			for (int i = 0; i < files.length; i++) {
				remoteFiles[i] = new LocalRemoteFile(files[i]);
			}
		} else {
			remoteFiles = new LocalRemoteFile[1];
			remoteFiles[0] = new LocalRemoteFile(local);
		}
	}

	protected void setupProxy(Proxy proxy) {
		// nothing
	}

}
