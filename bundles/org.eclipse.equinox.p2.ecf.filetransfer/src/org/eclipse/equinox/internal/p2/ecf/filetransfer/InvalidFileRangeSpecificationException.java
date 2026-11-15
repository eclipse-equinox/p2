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

/**
 *
 */
public class InvalidFileRangeSpecificationException extends IncomingFileTransferException {

	private static final long serialVersionUID = 532923607480972210L;

	private IFileRangeSpecification rangeSpec = null;

	/**
	 * @param rangeSpec rangeSpec
	 */
	public InvalidFileRangeSpecificationException(IFileRangeSpecification rangeSpec) {
		super();
		this.rangeSpec = rangeSpec;
	}

	/**
	 * @param status status
	 * @param rangeSpec range spec
	 */
	public InvalidFileRangeSpecificationException(IStatus status, IFileRangeSpecification rangeSpec) {
		super(status);
		this.rangeSpec = rangeSpec;
	}

	/**
	 * @param message message
	 * @param cause cause
	 * @param rangeSpec range spec
	 */
	public InvalidFileRangeSpecificationException(String message, Throwable cause, IFileRangeSpecification rangeSpec) {
		super(message, cause);
		this.rangeSpec = rangeSpec;
	}

	/**
	 * @param message message
	 * @param rangeSpec range spec
	 */
	public InvalidFileRangeSpecificationException(String message, IFileRangeSpecification rangeSpec) {
		super(message);
		this.rangeSpec = rangeSpec;
	}

	/**
	 * @param cause cause
	 * @param rangeSpec range spec
	 */
	public InvalidFileRangeSpecificationException(Throwable cause, IFileRangeSpecification rangeSpec) {
		super(cause);
		this.rangeSpec = rangeSpec;
	}

	public IFileRangeSpecification getFileRangeSpecification() {
		return this.rangeSpec;
	}

}
