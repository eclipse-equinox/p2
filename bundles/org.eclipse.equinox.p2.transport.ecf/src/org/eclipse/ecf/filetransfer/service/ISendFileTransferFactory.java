/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.filetransfer.service;

/**
 * Send file transfer factory. This service interface is used by clients to
 * create a new ISendFileTransfer instance, used to send file to remote
 * clients.
 */
public interface ISendFileTransferFactory {

	/**
	 * Get new instance of ISendFileTransfer.
	 * 
	 * @return ISendFileTransfer for initiating send of a local file.
	 */
	public ISendFileTransfer newInstance();

}
