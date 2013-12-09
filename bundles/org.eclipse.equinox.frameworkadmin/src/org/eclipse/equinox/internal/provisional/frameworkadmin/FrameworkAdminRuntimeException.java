/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
