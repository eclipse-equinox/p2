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
package org.eclipse.ecf.filetransfer.events;

import org.eclipse.ecf.filetransfer.UserCancelledException;

/**
 * Event sent to IFileTransferListeners when an incoming file transfer is
 * completed.
 * 
 */
public interface IIncomingFileTransferReceiveDoneEvent extends IIncomingFileTransferEvent {

	/**
	 * Get any exception associated with this file transfer. If the file
	 * transfer completed successfully, this method will return
	 * <code>null</code>. If the file transfer completed unsuccessfully (some
	 * exception occurred), then this method will return a non-<code>null</code>
	 * Exception instance that occurred.
	 * <p>
	 * If the the file transfer was canceled by the user, then the exception 
	 * returned will be an instance of {@link UserCancelledException}.  
	 * 
	 * @return Exception associated with this file transfer. <code>null</code>
	 *         if transfer completed successfully, non-null if transfer
	 *         completed with some exception.
	 */
	public Exception getException();

}
