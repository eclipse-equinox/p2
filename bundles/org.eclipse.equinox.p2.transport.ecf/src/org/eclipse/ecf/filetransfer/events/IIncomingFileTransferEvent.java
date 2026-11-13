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

import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;

/**
 * Super interface for incoming file transfer events
 * 
 */
public interface IIncomingFileTransferEvent extends IFileTransferEvent {
	/**
	 * Get {@link IIncomingFileTransfer} associated with this event
	 * 
	 * @return IIncomingFileTransfer that is source of this event. Will not be
	 *         <code>null</code>.
	 */
	public IIncomingFileTransfer getSource();
}
