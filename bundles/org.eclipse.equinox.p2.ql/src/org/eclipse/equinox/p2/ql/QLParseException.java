/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ql;

/**
 * An exception used by a query parser that indicates that something went wrong when
 * parsing.
 */
public class QLParseException extends RuntimeException {
	private static final long serialVersionUID = -4834995551702455870L;

	public QLParseException(String expression, String message, int position) {
		super("Parse error in string \"" + expression + "\": " + message + " at position " + position); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
