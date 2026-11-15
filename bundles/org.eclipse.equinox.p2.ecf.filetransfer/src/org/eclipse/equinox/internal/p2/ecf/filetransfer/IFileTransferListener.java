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

import org.eclipse.ecf.filetransfer.events.IFileTransferEvent;

/**
 * Listener for handling file transfer events. Instances implementing this
 * interface or sub-interfaces will have their handleTransferEvent called
 * asynchronously when a given event is received. Implementers must be prepared
 * to have this method called asynchronously by an arbitrary thread.
 * 
 * <p>
 * Note these methods will be called asynchronously when notifications of remote
 * changes are received by the provider implementation code. The provider is
 * free to call the methods below with an arbitrary thread, so the
 * implementation of these methods must be appropriately prepared.
 * <p>
 * For example, if the code implementing any of these methods must interact with
 * user interface code, then it should use code such as the following to execute
 * on the SWT UI thread:
 * 
 * <pre>
 * 	Display.getDefault().asyncExec(new Runnable() {
 * 		public void run() {
 * 		... UI code here
 * 		}
 * 	});
 * </pre>
 * 
 * Further, the code in the implementations of these methods should <b>not block</b>
 * via I/O operations or blocking UI calls.
 */
public interface IFileTransferListener {
	/**
	 * Handle file transfer events
	 * 
	 * @param event
	 *            the event to be handled. should not be <code>null</code>.
	 */
	public void handleTransferEvent(IFileTransferEvent event);
}
