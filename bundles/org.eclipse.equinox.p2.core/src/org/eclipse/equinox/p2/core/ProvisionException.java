/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core;

public class ProvisionException extends Exception {
	private static final long serialVersionUID = 1L;

	public ProvisionException(String message) {
		super(message);
	}

	public ProvisionException(Throwable e) {
		super(e);
	}

	public ProvisionException(String message, Throwable e) {
		super(message, e);
	}
}
