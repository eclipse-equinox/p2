/****************************************************************************
 * Copyright (c) 2008 Versant Corp.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Markus Kuppe (mkuppe <at> versant <dot> com) - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.core.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.internal.core.identity.Activator;

/**
 * @since 3.0
 */
public class ECFRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 456677468837349011L;
	/** Status object. */
	private IStatus status;

	public ECFRuntimeException() {
		super();
	}

	/**
	 * @param message
	 *            message associated with exception
	 */
	public ECFRuntimeException(String message) {
		this(message, null);
	}

	/**
	 * @param cause
	 *            the cause of the new exception
	 */
	public ECFRuntimeException(Throwable cause) {
		this(cause.getMessage(), cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ECFRuntimeException(String message, Throwable cause) {
		this(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
				((message == null) ? "" : message), cause)); //$NON-NLS-1$
	}

	/**
	 * Creates a new exception with the given status object. The message of the
	 * given status is used as the exception message.
	 *
	 * @param status
	 *            the status object to be associated with this exception
	 */
	public ECFRuntimeException(IStatus status) {
		super(status.getMessage());
		initCause(status.getException());
		this.status = status;
	}

	/**
	 * Returns the status object for this exception.
	 * <p>
	 * <b>IMPORTANT:</b><br>
	 * The result must NOT be used to log a <code>CoreException</code> (e.g.,
	 * using <code>yourPlugin.getLog().log(status);</code>), since that code
	 * pattern hides the original stacktrace. Instead, create a new
	 * {@link Status} with your plug-in ID and this <code>CoreException</code>,
	 * and log that new status.
	 * </p>
	 *
	 * @return a status object
	 */
	public IStatus getStatus() {
		return status;
	}
}
