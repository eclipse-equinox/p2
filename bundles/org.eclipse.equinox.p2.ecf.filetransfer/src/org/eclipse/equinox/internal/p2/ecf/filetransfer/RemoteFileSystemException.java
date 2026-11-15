/****************************************************************************
 * Copyright (c) 2007 Composent, Inc. and others.
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

package org.eclipse.ecf.filetransfer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.ECFException;

/**
 *
 */
public class RemoteFileSystemException extends ECFException {

	private static final long serialVersionUID = -2199951600347999396L;

	/**
	 * 
	 */
	public RemoteFileSystemException() {
		super();
	}

	/**
	 * @param status status
	 */
	public RemoteFileSystemException(IStatus status) {
		super(status);
	}

	/**
	 * @param message message
	 * @param cause cause
	 */
	public RemoteFileSystemException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message message
	 */
	public RemoteFileSystemException(String message) {
		super(message);
	}

	/**
	 * @param cause cause
	 */
	public RemoteFileSystemException(Throwable cause) {
		super(cause);
	}

}
