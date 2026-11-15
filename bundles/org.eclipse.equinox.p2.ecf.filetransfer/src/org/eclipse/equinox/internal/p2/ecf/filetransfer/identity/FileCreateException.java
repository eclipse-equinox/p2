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
package org.eclipse.ecf.filetransfer.identity;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.ECFException;

/**
 * Exception class for creation of {@link IFileID} instances via
 * {@link FileIDFactory}
 * 
 */
public class FileCreateException extends ECFException {

	private static final long serialVersionUID = -4242692047102300537L;

	public FileCreateException(IStatus status) {
		super(status);
	}

	public FileCreateException() {
		super();
	}

	public FileCreateException(String message) {
		super(message);
	}

	public FileCreateException(Throwable cause) {
		super(cause);
	}

	public FileCreateException(String message, Throwable cause) {
		super(message, cause);
	}

}
