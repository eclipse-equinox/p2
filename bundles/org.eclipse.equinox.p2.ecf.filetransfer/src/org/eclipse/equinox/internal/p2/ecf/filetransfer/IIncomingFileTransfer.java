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

import java.util.Date;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;

/**
 * Incoming file transfer request. Instance implementing this interface are
 * provided via calling the
 * {@link IIncomingFileTransferReceiveStartEvent#receive(java.io.File)} method.
 * 
 */
public interface IIncomingFileTransfer extends IFileTransfer {
	/**
	 * Get number of bytes received so far. If provider does not support
	 * reporting the number of bytes received, will return -1.
	 * 
	 * @return long number of bytes received. Returns -1 if provider does not
	 *         support reporting of number of bytes received during transfer
	 */
	public long getBytesReceived();

	/**
	 * Get listener assigned to this incoming file transfer.  May be <code>null</code> if no listener 
	 * has been provided.
	 * 
	 * @return listener the IFileTransferListener provided for this incoming file transfer.
	 */
	public IFileTransferListener getListener();

	/**
	 * Get file range specification for this incoming file transfer instance.  Will return
	 * <code>null</code> if the retrieval is of the entire file.
	 * 
	 * @return file range specification for this incoming file transfer instance.  Returns
	 * <code>null</code> if the retrieval is of the entire file.
	 */
	public IFileRangeSpecification getFileRangeSpecification();

	/**
	 * Obtains the name of the remote file if possible. The name will typically but not
	 * necessarily be the same as the leaf part of the path to the remote file.
	 *
	 * @return The name of the remote file or <code>null</code> if no such name can be determined.
	 * 
	 * @since 2.0
	 */
	public String getRemoteFileName();

	/**
	 * Obtains the timestamp that reflects the time when the remote file was last
	 * modified if possible. 
	 * @return The time the remote file was last modified or <code>null</code> if that
	 * information was not available.
	 * @since 2.0
	 */
	public Date getRemoteLastModified();
}
