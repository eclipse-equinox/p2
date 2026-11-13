/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
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

package org.eclipse.ecf.filetransfer.service;

/**
 * Remote file browser factory. This service interface is used by clients to
 * create a new IRemoteFileSystemBrowser instance.
 */
public interface IRemoteFileSystemBrowserFactory {

	/**
	 * Get new instance of IRemoteFileSystemBrowser.
	 * 
	 * @return IRemoteFileSystemBrowser for initiating a retrieval of a remote file.
	 */
	public IRemoteFileSystemBrowser newInstance();

}
