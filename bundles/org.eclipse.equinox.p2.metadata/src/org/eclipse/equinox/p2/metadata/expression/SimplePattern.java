/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

import java.io.Serializable;

/**
 * A simple compiled pattern. It supports two kinds of wildcards. The '*' (any character zero to many times)
 * and the '?' (any character exactly one time).
 * @since 2.0
 */
public class SimplePattern implements Serializable, Comparable<SimplePattern> {
	private static final long serialVersionUID = -2477990705739062410L;

	/**
	 * Matches the <code>value</code> with the compiled expression. The value
	 * is considered matching if all characters are matched by the expression. A
	 * partial match is not enough.
	 * @param value The value to match
	 * @return <code>true</code> if the value was a match.
	 */
	public boolean isMatch(CharSequence value) {
		if (node == null)
			node = parse(pattern, 0);
		return node.match(value, 0);
	}

	public String toString() {
		return pattern;
	}

	public int compareTo(SimplePattern o) {
		return pattern.compareTo(o.pattern);
	}

	public boolean equals(Object o) {
		return o == this || (o instanceof SimplePattern && ((SimplePattern) o).pattern.equals(pattern));
	}

	public int hashCode() {
		return 3 * pattern.hashCode();
	}

	private final String pattern;
	private transient Node node;

	private SimplePattern(String pattern) {
		this.pattern = pattern;
	}

	static class AllNode extends Node {
		boolean match(CharSequence value, int pos) {
			return true;
		}
	}

	static class RubberBandNode extends Node {
		final Node next;

		RubberBandNode(Node next) {
			this.next = next;
		}

		boolean match(CharSequence value, int pos) {
			int top = value.length();
			String ending = next.getEndingConstant();
			if (ending != null) {
				// value must end with this constant. It will be faster
				// to scan backwards from the end.
				int clen = ending.length();
				if (clen > top - pos)
					return false;
				while (clen > 0)
					if (ending.charAt(--clen) != value.charAt(--top))
						return false;
				return true;
			}

			while (pos < top) {
				if (next.match(value, pos++))
					return true;
			}
			return false;
		}
	}

	static class AnyCharacterNode extends Node {
		final Node next;

		AnyCharacterNode(Node next) {
			this.next = next;
		}

		boolean match(CharSequence value, int pos) {
			int top = value.length();
			return next == null ? pos + 1 == top : next.match(value, pos + 1);
		}
	}

	static class EndConstantNode extends Node {
		final String constant;

		EndConstantNode(String constant) {
			this.constant = constant;
		}

		boolean match(CharSequence value, int pos) {
			int max = constant.length() + pos;
			int top = value.length();
			if (top != max)
				return false;

			int idx = 0;
			while (pos < max)
				if (value.charAt(pos++) != constant.charAt(idx++))
					return false;
			return true;
		}

		String getEndingConstant() {
			return constant;
		}
	}

	static class ConstantNode extends Node {
		final Node next;
		final String constant;

		ConstantNode(Node next, String constant) {
			this.next = next;
			this.constant = constant;
		}

		boolean match(CharSequence value, int pos) {
			int max = constant.length() + pos;
			int top = value.length();
			if (top < max)
				return false;

			int idx = 0;
			while (pos < max)
				if (value.charAt(pos++) != constant.charAt(idx++))
					return false;
			return next == null ? (pos == top) : next.match(value, pos);
		}
	}

	static abstract class Node {
		abstract boolean match(CharSequence value, int pos);

		String getEndingConstant() {
			return null;
		}
	}

	public static SimplePattern compile(String pattern) {
		if (pattern == null)
			throw new IllegalArgumentException("Pattern can not be null"); //$NON-NLS-1$
		return new SimplePattern(pattern);
	}

	private static Node parse(String pattern, int pos) {
		int top = pattern.length();
		StringBuffer bld = null;
		Node parsedNode = null;
		while (pos < top) {
			char c = pattern.charAt(pos);
			switch (c) {
				case '*' :
					++pos;
					parsedNode = pos == top ? new AllNode() : new RubberBandNode(parse(pattern, pos));
					break;
				case '?' :
					parsedNode = new AnyCharacterNode(parse(pattern, pos + 1));
					break;
				case '\\' :
					if (++pos == top)
						throw new IllegalArgumentException("Pattern ends with escape"); //$NON-NLS-1$
					c = pattern.charAt(pos);
					// fall through
				default :
					if (bld == null)
						bld = new StringBuffer();
					bld.append(c);
					++pos;
					continue;
			}
			break;
		}

		if (bld != null) {
			String constant = bld.toString();
			parsedNode = parsedNode == null ? new EndConstantNode(constant) : new ConstantNode(parsedNode, constant);
		}
		return parsedNode;
	}
}
