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
package org.eclipse.equinox.p2.core;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.Activator;

/**
 * A checked exception indicating a recoverable error occurred while provisioning.
 * The status provides a further description of the problem.
 * <p>
 * This exception class is not intended to be subclassed by clients.
 * </p>
 */
public class ProvisionException extends CoreException {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new exception with the given status object.  The message
	 * of the given status is used as the exception message.
	 *
	 * @param status the status object to be associated with this exception
	 */
	public ProvisionException(IStatus status) {
		super(status);
	}

	/**
	 * Creates a new exception with the given message and a severity of 
	 * {@link IStatus#ERROR}.
	 *
	 * @param message The human-readable problem message
	 */
	public ProvisionException(String message) {
		super(new Status(IStatus.ERROR, Activator.ID, message));
	}

	/**
	 * Creates a new exception with the given message and cause, and
	 * a severity of {@link IStatus#ERROR}.
	 *
	 * @param message The human-readable problem message
	 * @param cause The underlying cause of the exception
	 */
	public ProvisionException(String message, Throwable cause) {
		super(new Status(IStatus.ERROR, Activator.ID, message, cause));
	}
}
