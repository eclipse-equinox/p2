/****************************************************************************
 * Copyright (c) 2009, 2026 IBM, and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.equinox.p2.transport.ecf.filetransfer.events;

import org.eclipse.equinox.p2.transport.ecf.filetransfer.IFileTransferListener;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IIncomingFileTransfer;
import org.eclipse.equinox.p2.transport.ecf.filetransfer.IRemoteFileSystemRequest;

/**
 * Event sent to {@link IFileTransferListener} associated with
 * {@link IIncomingFileTransfer} or {@link IRemoteFileSystemRequest} instances.
 * <p>
 * The event is send before the first request is send to the server. It allows
 * the caller to get a handle to the transfer so that it can be canceled. A
 * transfer may have to send several requests to one (or more servers) until the
 * retrieved data itself is received. The entire phase before this is referred
 * here as connect phase.
 * </p>
 * <p>
 * The caller may cancel the request from another thread. The expectation is
 * that the cancellation will react with little delay, typically in less than 1
 * second.
 * </p>
 * <p>
 * Not all providers support this event.
 * </p>
 */
public interface IFileTransferConnectStartEvent extends IFileTransferEvent {

	/**
	 * Cancel file transfer.
	 */
	void cancel();
}
