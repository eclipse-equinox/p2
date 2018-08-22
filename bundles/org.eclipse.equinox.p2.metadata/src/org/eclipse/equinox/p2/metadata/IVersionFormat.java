/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

/**
 * <p>The IVersionFormat represents the Omni Version Format in compiled form. It
 * is also a parser for versions of that format.</p>
 * <p>An instance of IVersionFormat is immutable and thus thread safe. The parser
 * does not maintain any state.</p>
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IVersionFormat {

	/**
	 * The string that by default will be interpreted as the logical max string when parsing
	 * optional elements of type string and a default that is the empty string (i.e. OSGi)
	 */
	static final String DEFAULT_MAX_STRING_TRANSLATION = "zzz"; //$NON-NLS-1$

	/**
	 * The string that by default will be interpreted as the logical min string when parsing
	 * optional elements of type string and a default that is the max string (i.e. Maven triplets)
	 */
	static final String DEFAULT_MIN_STRING_TRANSLATION = "-"; //$NON-NLS-1$

	/**
	 * Appends the string representation of this compiled format to
	 * the given StringBuffer.
	 * @param sb The buffer that will receive the string representation
	 */
	void toString(StringBuffer sb);

	/**
	 * Parse the given version string.
	 * @param version The version string to parse.
	 * @return A created version.
	 * @throws IllegalArgumentException If the version string could not be parsed.
	 */
	Version parse(String version);
}
