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
package org.eclipse.equinox.internal.p2.metadata;

import org.eclipse.equinox.internal.provisional.p2.metadata.Version;

/**
 * The abstract BasicVersion class adds the methods necessary to to compare and serialize
 * versions in version ranges. The class is not intended as public API.
 */
public abstract class BasicVersion extends Version {
	private static final long serialVersionUID = -2983093417537485027L;

	/**
	 * Appends the original for this version onto the <code>sb</code> StringBuffer
	 * if present.
	 * @param sb The buffer that will receive the raw string format
	 * @param rangeSafe Set to <code>true</code> if range delimiters should be escaped
	 */
	public abstract void originalToString(StringBuffer sb, boolean rangeSafe);

	/**
	 * Appends the raw format for this version onto the <code>sb</code> StringBuffer.
	 * @param sb The buffer that will receive the raw string format
	 * @param rangeSafe Set to <code>true</code> if range delimiters should be escaped
	 */
	public abstract void rawToString(StringBuffer sb, boolean rangeSafe);

	/**
	 * This method is package protected since it violates the immutable
	 * contract.
	 * @return The raw vector. Must be treated as read-only
	 */
	abstract Comparable[] getVector();
}
