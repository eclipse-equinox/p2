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

import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.IIncomingFileTransfer;

/**
 * Event sent to {@link IFileTransferListener} associated with
 * {@link IIncomingFileTransfer} instances data are received
 * 
 */
public interface IIncomingFileTransferReceiveDataEvent extends IIncomingFileTransferEvent {
	// no methods for interface
}
