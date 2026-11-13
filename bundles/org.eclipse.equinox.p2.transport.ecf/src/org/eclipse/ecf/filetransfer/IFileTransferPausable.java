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

/**
 * Adapter interface for pausing and resuming IFileTransfer instances that
 * expose this adapter interface via
 * {@link IFileTransfer#getAdapter(Class adapter)}. To use this interface,
 * clients should do the following:
 * 
 * <pre>
 *   IFileTransfer fileTransfer;
 *   IFileTransferPausable pausable = (IFileTransferPausable) fileTransfer.getAdapter(IFileTransferPausable.class);
 *   if (pausable !=null) {
 *      ... use it
 *   } else {
 *      ... does not support pausing
 *   }
 * </pre>
 * 
 */
public interface IFileTransferPausable {

	/**
	 * Pause file transfer. Returns true if the associated IFileTransfer is
	 * successfully paused. Returns false if the implementing file transfer
	 * cannot be paused, or transfer has already completed.
	 * 
	 * @return boolean true if file transfer successfully paused. False if cannot be
	 *         paused, or the transfer has already completed
	 */
	public boolean pause();

	/**
	 * 
	 * @return boolean true if file transfer paused, false if not paused
	 */
	public boolean isPaused();

	/**
	 * Resume file transfer after having been paused. If successfully resumed,
	 * then returns true. If the associated IFileShare is not already paused, or
	 * has already completed then this method returns false.
	 * 
	 * @return boolean true if transfer is successfully resumed, false otherwise
	 */
	public boolean resume();
}
