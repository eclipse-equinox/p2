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
	public synchronized boolean isMatch(CharSequence value) {
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

	private SimplePattern(String pattern, Node node) {
		this.pattern = pattern;
		this.node = node;
	}

	private static class RubberBandNode extends Node {
		RubberBandNode(Node next) {
			super(next);
		}

		boolean match(CharSequence value, int pos) {
			if (next == null)
				return true;

			int top = value.length();
			while (pos < top) {
				if (next.match(value, pos++))
					return true;
			}
			return false;
		}
	}

	private static class AnyCharacterNode extends Node {
		AnyCharacterNode(Node next) {
			super(next);
		}

		boolean match(CharSequence value, int pos) {
			int top = value.length();
			return next == null ? pos + 1 == top : next.match(value, pos + 1);
		}
	}

	private static class ConstantNode extends Node {
		final String constant;

		ConstantNode(Node next, String constant) {
			super(next);
			this.constant = constant;
		}

		boolean match(CharSequence value, int pos) {
			int vtop = value.length();
			int ctop = constant.length();
			if (ctop + pos > vtop)
				return false;

			for (int idx = 0; idx < ctop; ++idx, ++pos)
				if (constant.charAt(idx) != value.charAt(pos))
					return false;

			return next == null ? true : next.match(value, pos);
		}
	}

	private static abstract class Node {
		final Node next;

		Node(Node next) {
			this.next = next;
		}

		abstract boolean match(CharSequence value, int pos);
	}

	public static SimplePattern compile(String pattern) {
		if (pattern == null)
			throw new IllegalArgumentException("Pattern can not be null"); //$NON-NLS-1$
		return new SimplePattern(pattern, null);
	}

	private static Node parse(String pattern, int pos) {
		int top = pattern.length();
		StringBuffer bld = null;
		Node parsedNode = null;
		while (pos < top) {
			char c = pattern.charAt(pos);
			switch (c) {
				case '*' :
					parsedNode = new RubberBandNode(parse(pattern, pos + 1));
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

		if (bld != null)
			parsedNode = new ConstantNode(parsedNode, bld.toString());
		return parsedNode;
	}
}
