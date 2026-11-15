/****************************************************************************
 * Copyright (c) 2004 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.filetransfer;

import org.eclipse.ecf.filetransfer.events.IFileTransferRequestEvent;

/**
 * Listener for incoming file transfer requests. Instances implementing this
 * interface are registered via the
 * {@link ISendFileTransferContainerAdapter#addListener(IIncomingFileTransferRequestListener)}
 * 
 * @see ISendFileTransferContainerAdapter#addListener(IIncomingFileTransferRequestListener)
 */
public interface IIncomingFileTransferRequestListener {
	/**
	 * Handle file transfer requests when received asynchronously from remotes.
	 * 
	 * @param event
	 *            the {@link IFileTransferRequestEvent} that represents the file
	 *            transfer request. Will not be should not be <code>null</code>.
	 */
	public void handleFileTransferRequest(IFileTransferRequestEvent event);
}
