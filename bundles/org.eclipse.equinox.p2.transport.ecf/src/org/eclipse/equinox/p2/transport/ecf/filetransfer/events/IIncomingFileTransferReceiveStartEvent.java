/****************************************************************************
 * Copyright (c) 2004, 2026 Composent, Inc. and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *               Cloudsmith, Inc. - additional API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.equinox.p2.transport.ecf.filetransfer.events;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IIncomingFileTransfer;

/**
 * Event sent to {@link IFileTransferListener} associated with
 * {@link IIncomingFileTransfer} instances
 *
 */
public interface IIncomingFileTransferReceiveStartEvent extends IIncomingFileTransferEvent {

	/**
	 * Get incoming file transfer by specifying an OutputStream instance to save
	 * the received contents to. NOTE: the caller is responsible for calling
	 * {@link OutputStream#close()} on the OutputStream provided. If the stream
	 * provided is buffered, then {@link java.io.BufferedOutputStream#flush()}
	 * should be called to guaranteed that the data received is actually written
	 * to the given OutputStream.
	 *
	 * @param streamToStore the output stream to store the incoming file. Must not
	 *                      be <code>null</code>.
	 * @return IIncomingFileTransfer the incoming file transfer object. Will not
	 *         be <code>null</code>.
	 * @throws IOException if streamToStore cannot be opened for writing
	 */
	public IIncomingFileTransfer receive(OutputStream streamToStore) throws IOException;

	/**
	 * Cancel incoming file transfer
	 */
	public void cancel();
}
