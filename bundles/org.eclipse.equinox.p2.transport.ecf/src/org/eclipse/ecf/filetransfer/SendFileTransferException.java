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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.ECFException;

/**
 * Outgoing file transfer exception
 * 
 */
public class SendFileTransferException extends ECFException {

	private static final long serialVersionUID = -3752377147967128446L;

	private int errorCode = -1;

	public SendFileTransferException(IStatus status) {
		super(status);
	}

	public SendFileTransferException() {
		// null constructor
	}

	public SendFileTransferException(int errorCode) {
		this();
		this.errorCode = errorCode;
	}

	public SendFileTransferException(String message) {
		super(message);
	}

	public SendFileTransferException(String message, int errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public SendFileTransferException(Throwable cause) {
		super(cause);
	}

	public SendFileTransferException(Throwable cause, int errorCode) {
		super(cause);
		this.errorCode = errorCode;
	}

	public SendFileTransferException(String message, Throwable cause) {
		super(message, cause);
	}

	public SendFileTransferException(String message, Throwable cause, int errorCode) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
