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

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.ECFException;

/**
 * Exception thrown upon incoming file transfer problem
 * 
 */
public class IncomingFileTransferException extends ECFException {

	private static final long serialVersionUID = 2438441801862623371L;

	private int errorCode = -1;
	private Map responseHeaders;

	public IncomingFileTransferException(IStatus status) {
		super(status);
	}

	public IncomingFileTransferException() {
		// null constructor
	}

	public IncomingFileTransferException(int errorCode) {
		this();
		this.errorCode = errorCode;
	}

	public IncomingFileTransferException(String message) {
		super(message);
	}

	public IncomingFileTransferException(String message, int errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public IncomingFileTransferException(Throwable cause) {
		super(cause);
	}

	public IncomingFileTransferException(Throwable cause, int errorCode) {
		super(cause);
		this.errorCode = errorCode;
	}

	public IncomingFileTransferException(String message, Throwable cause) {
		super(message, cause);
	}

	public IncomingFileTransferException(String message, Throwable cause, int errorCode) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	/**
	 * @param message message
	 * @param cause cause
	 * @param errorCode errorCode
	 * @param responseHeaders responseHeaders
	 * @since 4.0
	 */
	public IncomingFileTransferException(String message, Throwable cause, int errorCode, Map responseHeaders) {
		super(message, cause);
		this.errorCode = errorCode;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * @param message message
	 * @param errorCode errorCode
	 * @param responseHeaders responseHeaders
	 * @since 4.0
	 */
	public IncomingFileTransferException(String message, int errorCode, Map responseHeaders) {
		super(message);
		this.errorCode = errorCode;
		this.responseHeaders = responseHeaders;
	}

	/**
	 * @param cause cause
	 * @param errorCode errorCode
	 * @param responseHeaders responseHeaders
	 * @since 4.0
	 */
	public IncomingFileTransferException(Throwable cause, int errorCode, Map responseHeaders) {
		super(cause);
		this.errorCode = errorCode;
		this.responseHeaders = responseHeaders;
	}

	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * @since 4.0
	 * @return Map response headers
	 */
	public Map getResponseHeaders() {
		return responseHeaders;
	}
}
