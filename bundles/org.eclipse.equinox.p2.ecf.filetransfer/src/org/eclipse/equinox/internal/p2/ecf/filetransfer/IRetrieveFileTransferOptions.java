/****************************************************************************
 * Copyright (c) 2009 Composent, Inc. and others.
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
package org.eclipse.ecf.filetransfer;

import java.util.Map;

/**
 * @since 3.1
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IRetrieveFileTransferOptions {

	/**
	 * This constant defines a key in options Map passed to {@link IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileTransferListener, java.util.Map)}.
	 * Supporting providers will use this key to look for a value of type Map, and if found the String key/value pairs in the 
	 * Map will be used as request headers.  The expected type of the value associated with this key is of type {@link Map}.
	 * 
	 */
	public static final String REQUEST_HEADERS = IRetrieveFileTransferOptions.class.getName() + ".requestHeaders"; //$NON-NLS-1$

	/**
	 * This constant defines a key in options Map passed to {@link IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileTransferListener, java.util.Map)}.
	 * Supporting providers will use this key to look for a value of type Integer or String, and if found this value
	 * will be used to determine the socket connection timeout for this request.  The expected type of the value
	 * associated with this key is of type Integer, or String value of an Integer.
	 */
	public static final String CONNECT_TIMEOUT = IRetrieveFileTransferOptions.class.getName() + ".connectTimeout"; //$NON-NLS-1$

	/**
	 * This constant defines a key in options Map passed to {@link IRetrieveFileTransferContainerAdapter#sendRetrieveRequest(org.eclipse.ecf.filetransfer.identity.IFileID, IFileTransferListener, java.util.Map)}.
	 * Supporting providers will use this key to look for a value of type Integer or String, and if found this value
	 * will be used to determine the socket read timeout for this request.  The expected type of the value
	 * associated with this key is of type Integer, or String value of an Integer.
	 */
	public static final String READ_TIMEOUT = IRetrieveFileTransferOptions.class.getName() + ".readTimeout"; //$NON-NLS-1$
}
