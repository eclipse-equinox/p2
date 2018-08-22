/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.frameworkadmin;

public class FrameworkAdminRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -2292498677000772317L;
	public static final String FRAMEWORKADMIN_UNAVAILABLE = "FrameworkAdmin service created this object is not available any more"; //$NON-NLS-1$
	public static final String UNSUPPORTED_OPERATION = "This implementation doesn't support this method."; //$NON-NLS-1$

	private final String reason;
	private Throwable cause;

	/**
	 * @param message
	 */
	public FrameworkAdminRuntimeException(String message, String reason) {
		super(message);
		this.reason = reason;
		this.cause = null;
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FrameworkAdminRuntimeException(String message, Throwable cause, String reason) {
		super(message);
		this.reason = reason;
		this.cause = cause;
	}

	/**
	 * @param cause
	 */
	public FrameworkAdminRuntimeException(Throwable cause, String reason) {
		super(cause.getLocalizedMessage());
		this.reason = reason;
		this.cause = cause;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public Throwable getCause() {
		return cause;
	}
}
