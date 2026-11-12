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
package org.eclipse.ecf.core.security;

public class UnsupportedCallbackException extends Exception {

	private static final long serialVersionUID = -1821878324884011143L;

	private Callback callback;

	/**
	 * Constructs a <code>UnsupportedCallbackException</code> with no detail
	 * message.
	 * 
	 * <p>
	 * 
	 * @param callback
	 *            the unrecognized <code>Callback</code>.
	 */
	public UnsupportedCallbackException(Callback callback) {
		super();
		this.callback = callback;
	}

	/**
	 * Constructs a UnsupportedCallbackException with the specified detail
	 * message. A detail message is a String that describes this particular
	 * exception.
	 * 
	 * <p>
	 * 
	 * @param callback
	 *            the unrecognized <code>Callback</code>.
	 *            <p>
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public UnsupportedCallbackException(Callback callback, String msg) {
		super(msg);
		this.callback = callback;
	}

	/**
	 * Get the unrecognized <code>Callback</code>.
	 * 
	 * <p>
	 * 
	 * @return the unrecognized <code>Callback</code>.
	 */
	public Callback getCallback() {
		return callback;
	}

}
