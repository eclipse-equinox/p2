/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

/**
 * An exception used by an expression parser that indicates that something went wrong when
 * parsing.
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class ExpressionParseException extends RuntimeException {
	private static final long serialVersionUID = 8432875384760577764L;

	public ExpressionParseException(String message) {
		super(message);
	}

	public ExpressionParseException(String expression, String message, int position) {
		super("Parse error in string \"" + expression + "\": " + message + " at position " + position); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
