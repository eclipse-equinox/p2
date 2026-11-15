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

import java.io.File;
import java.io.OutputStream;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.filetransfer.IFileTransferInfo;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.ecf.filetransfer.IIncomingFileTransferRequestListener;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;

/**
 * Event to represent remote file transfer requests. Events implementing this
 * interface are delivered to {@link IIncomingFileTransferRequestListener}
 * 
 * @see IIncomingFileTransferRequestListener#handleFileTransferRequest(IFileTransferRequestEvent)
 */
public interface IFileTransferRequestEvent extends IFileTransferEvent {
	/**
	 * Get ID of remote requester
	 * 
	 * @return ID of remote requester. Will not be <code>null</code>.
	 */
	public ID getRequesterID();

	/**
	 * Get file transfer info associated with this file transfer request even
	 * 
	 * @return IFileTransfer info. Will not be <code>null</code>.
	 */
	public IFileTransferInfo getFileTransferInfo();

	/**
	 * Accept the file transfer request. This method should be called if the
	 * receiver of the IFileTransferRequestEvent would like to accept the file
	 * transfer request. Will not return <code>null</code>. Once called
	 * successfully, then {@link #requestAccepted()} will return true, and
	 * further calls to {@link #accept(File)} or
	 * {@link #accept(OutputStream, IFileTransferListener)} will throw
	 * IncomingFileTransferExceptions.
	 * 
	 * @param localFileToSave
	 *            the file on the local file system to receive the remote file.
	 *            Must not be <code>null</code>.
	 * @return IIncomingFileTransfer to receive file. Will not be
	 *         <code>null</code>.
	 * @throws IncomingFileTransferException
	 *             if accept message cannot be delivered back to requester
	 */
	public IIncomingFileTransfer accept(File localFileToSave) throws IncomingFileTransferException;

	/**
	 * Accept the file transfer request. This method should be called if the
	 * receiver of the IFileTransferRequestEvent would like to accept the file
	 * transfer request. Will not return <code>null</code>. Once called
	 * successfully, then {@link #requestAccepted()} will return true, and
	 * further calls to {@link #accept(File)} or
	 * {@link #accept(OutputStream, IFileTransferListener)} will throw
	 * IncomingFileTransferExceptions.
	 * 
	 * @param outputStream
	 *            the output stream to receive the accepted file contents. Must
	 *            not be <code>null</code>.
	 * @param listener
	 *            for file transfer events during file reception. May be
	 *            <code>null</code>.
	 * @return IIncomingFileTransfer to receive file. Will not be
	 *         <code>null</code>.
	 * @throws IncomingFileTransferException
	 *             if accept message cannot be delivered back to requester
	 */
	public IIncomingFileTransfer accept(OutputStream outputStream, IFileTransferListener listener) throws IncomingFileTransferException;

	/**
	 * Reject the file transfer request. This method should be called if the
	 * receiver of the IFileTransferRequestEvent would like to reject the file
	 * transfer request
	 * 
	 */
	public void reject();

	/**
	 * If request was accepted from remote target (via successful call to
	 * {@link #accept(File)}this method will return true, if rejected or failed
	 * returns false.
	 * 
	 * @return true if request was accepted, false if rejected or failed
	 */
	public boolean requestAccepted();
}
