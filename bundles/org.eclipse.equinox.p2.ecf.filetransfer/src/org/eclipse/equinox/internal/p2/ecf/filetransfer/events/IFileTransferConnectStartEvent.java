/****************************************************************************
 * Copyright (c) 2009 IBM, and others.
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

package org.eclipse.ecf.filetransfer.events;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.identity.IFileID;

/**
 * Event sent to {@link IFileTransferListener} associated with
 * {@link IIncomingFileTransfer} or 
 * {@link IOutgoingFileTransferEvent} or 
 * {@link IRemoteFileSystemRequest} instances.
 * <p>
 * The event is send before the first request is send to the server. 
 * It allows the caller to get a handle to the transfer so that it can be 
 * canceled.
 * A transfer may have to send several requests to one (or more
 * servers) until the retrieved or send data itself is send or received.
 * The entire phase before this is referred here as connect phase.
 * </p> 
 * <p>
 * If {@link #connectUsingJob(FileTransferJob)} is called then the connect
 * phase is performed in a job. If not the caller may implement their own thread 
 * in which it can cancel the request. The expectation is that the 
 * cancellation will react with little delay, typically in less than 1 second.    
 * </p>
 * <p>
 * Not all providers support this event. 
 * </p>
 * @since 3.0
 */
public interface IFileTransferConnectStartEvent extends IAdaptable, IFileTransferEvent {

	IFileID getFileID();

	/**
	 * Cancel file transfer.
	 */
	void cancel();

	// IFileTransfer getFileTransfer();

	/**
	 * Prepare custom connect job or get default connect job.
	 * <p>
	 * As a result the connect job will be tied to the transfer. Only if the 
	 * returned job is passed into 
	 * {@link #connectUsingJob(FileTransferJob)} will it actually be scheduled to
	 * run.
	 * </p>
	 * 
	 * @param connectJob
	 *            A subclass of {@link FileTransferJob} to use to run the
	 *            connection process. If <code>null</code>, the provider will 
	 *            use create and prepare a default connect job.
	 *            NOTE: the given job should *not* be
	 *            scheduled/started prior to being provided as a parameter to
	 *            this method.
	 * @return passed in job or default connect job if parameter connectJob was 
	 *   null.           
	 */
	FileTransferJob prepareConnectJob(FileTransferJob connectJob);

	/**
	 * Connect using a job.
	 * <p>
	 * The passed in connectJob must have been prepared using 
	 * {@link #prepareConnectJob(FileTransferJob)}.
	 * The job may be scheduled after the caller returns from handling of
	 * the {@link IFileTransferConnectStartEvent}. As this is provider 
	 * implementation specific it must not relied on. 
	 * </p>
	 * 
	 * @param connectJob
	 *            A subclass of {@link FileTransferJob} to use to run the
	 *            connection process. Must not be <code>null</code>.
	 *            NOTE: the given job should *not* be
	 *            scheduled/started prior to being provided as a parameter to
	 *            this method.
	 * 
	 */
	void connectUsingJob(FileTransferJob connectJob);
}
