/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.identity;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.ECFRuntimeException;

public class IDCreateException extends ECFRuntimeException {
	private static final long serialVersionUID = 3258416140119323960L;

	public IDCreateException() {
		super();
	}

	public IDCreateException(IStatus status) {
		super(status);
	}

	public IDCreateException(String message) {
		super(message);
	}

	public IDCreateException(Throwable cause) {
		super(cause);
	}

	public IDCreateException(String message, Throwable cause) {
		super(message, cause);
	}
}