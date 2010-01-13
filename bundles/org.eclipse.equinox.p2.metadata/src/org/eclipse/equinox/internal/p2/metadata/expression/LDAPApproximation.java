/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.io.Serializable;

/**
 * Map a string for an LDAP APPROX (~=) comparison.
 * This implementation removes white spaces and transforms everything to lower case.
 */
public final class LDAPApproximation implements Serializable, Comparable<LDAPApproximation> {
	private static final long serialVersionUID = 4782295637798543587L;
	private final String pattern;
	private transient String approxPattern;

	public LDAPApproximation(String pattern) {
		this.pattern = pattern;
	}

	public int compareTo(LDAPApproximation o) {
		return pattern.compareTo(o.pattern);
	}

	public boolean equals(Object o) {
		return o == this || (o instanceof LDAPApproximation && ((LDAPApproximation) o).pattern.equals(pattern));
	}

	public int hashCode() {
		return 3 * pattern.hashCode();
	}

	/**
	 * Matches the <code>value</code> with the compiled expression. The value
	 * is considered matching if all characters are matched by the expression. A
	 * partial match is not enough.
	 * @param value The value to match
	 * @return <code>true</code> if the value was a match.
	 */
	public boolean isMatch(CharSequence value) {
		if (value == null)
			return false;
		if (approxPattern == null)
			approxPattern = approxString(pattern);
		return approxString(value).equals(approxPattern);
	}

	public String toString() {
		return pattern;
	}

	private static String approxString(CharSequence input) {
		boolean changed = false;
		char[] output = new char[input.length()];
		int cursor = 0;
		for (int i = 0, length = output.length; i < length; i++) {
			char c = input.charAt(i);
			if (Character.isWhitespace(c)) {
				changed = true;
				continue;
			}
			if (Character.isUpperCase(c)) {
				changed = true;
				c = Character.toLowerCase(c);
			}
			output[cursor++] = c;
		}
		return changed ? new String(output, 0, cursor) : input.toString();
	}
}
