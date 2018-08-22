/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.compatibility.util;

import java.io.IOException;

/**
 * An IO Exception that allows for {@link #getCause() a cause}.
 * 
 * @author David Green
 */
public class IOWithCauseException extends IOException {

	private static final long serialVersionUID = 1L;

	private final Throwable cause;

	public IOWithCauseException(String message, Throwable cause) {
		super(message);
		this.cause = cause;
	}

	public IOWithCauseException(Throwable cause) {
		this.cause = cause;
	}

	@Override
	public Throwable getCause() {
		return cause;
	}

}
