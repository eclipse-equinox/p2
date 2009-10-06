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
package org.eclipse.equinox.internal.provisional.p2.core;


/**
 * Exception thrown by the {@link VersionFormatParser}
 *
 */
public class FormatException extends Exception {

	private static final long serialVersionUID = -867104101610941043L;

	public FormatException(String message) {
		super(message);
	}
}
