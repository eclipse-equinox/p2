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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ecf.core.identity.IIdentifiable;
import org.eclipse.ecf.filetransfer.events.IFileTransferEvent;

/**
 * File transfer super interface. This interface provides the abstract file
 * transfer semantics for both {@link IOutgoingFileTransfer} and
 * {@link IIncomingFileTransfer} transfer sub interfaces.
 */
public interface IFileTransfer extends IAdaptable, IIdentifiable {
	/**
	 * Cancel this file transfer. If file transfer has already been completed,
	 * then this method has no effect. If the file transfer has not been
	 * completed then calling this method will result in an
	 * {@link IFileTransferEvent} being delivered to the
	 * {@link IFileTransferListener} indicating that transfer is done ({@link #isDone()}
	 * returns true), and some exception will be made available
	 * 
	 */
	public void cancel();

	/**
	 * Get the percent complete for this file transfer. The returned value will
	 * be either -1.0, meaning that the underlying provider does not support
	 * reporting percent complete for this file transfer, or a value between 0
	 * and 1 reflecting the percent complete for this file transfer. If 0.0 no
	 * data has been sent, if 1.0, the file transfer is 100 percent complete.
	 * 
	 * The value returned from this method should <b>not</b> be used to
	 * determine whether the transfer has completed, as it may not show
	 * completion in the event of an transfer failure. Note that the
	 * {@link #isDone()} method should be consulted to determine if the file
	 * transfer has completed (with or without error).
	 * 
	 * @return double percent complete. Returns -1.0 if the underlying provider
	 *         does not support reporting percentage complete, or between 0 and
	 *         1 to indicate actual percent complete for this file transfer
	 */
	public double getPercentComplete();

	/**
	 * Get any exception associated with this file transfer. The value returned
	 * from this method is valid only if {@link #isDone()} method returns true.
	 * If the file transfer completed successfully, {@link #isDone()} will
	 * return true, and this method will return null. If the file transfer
	 * completed unsuccessfully (some exception occurred), then
	 * {@link #isDone()} will return true, and this method will return a
	 * non-null Exception instance that occurred.
	 * <p>
	 * If the the file transfer was canceled by the user, then the exception 
	 * returned will be an instance of {@link UserCancelledException}.  
	 * 
	 * @return Exception associated with this file transfer. <code>null</code>
	 *         if transfer completed successfully, non-null if transfer
	 *         completed with some exception. Only valid <b>after</b>
	 *         {@link #isDone()} returns true.
	 */
	public Exception getException();

	/**
	 * Return true if this file transfer is done, false if not yet completed.
	 * The file transfer can be completed successfully, or an exception can
	 * occur and the file transfer will have failed. In either case of
	 * successful or unsuccessful transfer, this method will return true when
	 * the file transfer is complete. To determine whether the transfer
	 * completed <b>successfully</b>, it is necessary to also consult the
	 * {@link #getException()} method.
	 * 
	 * @return boolean true if file transfer is done, false if file transfer is
	 *         still in progress.
	 */
	public boolean isDone();

	/**
	 * Return resulting file length (in bytes) for this file transfer instance.  If the length is not known,
	 * -1 will be returned.  Note that if a {@link IFileRangeSpecification} is provided that the returned
	 * file length is the expected file length of just the range retrieved (and not the entire file).
	 *
	 *@return long file length
	 * @since 2.0
	 */
	public long getFileLength();

}
