/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.publisher.eclipse.bundledescription;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple tokenizer class. Used to parse data.
 */
class Tokenizer {
	protected char value[];
	protected int max;
	protected int cursor;

	public Tokenizer(String value) {
		this.value = value.toCharArray();
		max = this.value.length;
		cursor = 0;
	}

	private void skipWhiteSpace() {
		char[] val = value;
		int cur = cursor;

		for (; cur < max; cur++) {
			char c = val[cur];
			if ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r')) {
				continue;
			}
			break;
		}
		cursor = cur;
	}

	public String getToken(String terminals) {
		skipWhiteSpace();
		char[] val = value;
		int cur = cursor;

		int begin = cur;
		for (; cur < max; cur++) {
			char c = val[cur];
			if ((terminals.indexOf(c) != -1)) {
				break;
			}
		}
		cursor = cur;
		int count = cur - begin;
		if (count > 0) {
			skipWhiteSpace();
			while (count > 0 && (val[begin + count - 1] == ' ' || val[begin + count - 1] == '\t'))
				count--;
			return (new String(val, begin, count));
		}
		return (null);
	}

	public String getEscapedToken(String terminals) {
		char[] val = value;
		int cur = cursor;
		if (cur >= max)
			return null;
		StringBuilder sb = new StringBuilder();
		char c;
		for (; cur < max; cur++) {
			c = val[cur];
			// this is an escaped char
			if (c == '\\') {
				cur++; // skip the escape char
				if (cur == max)
					break;
				c = val[cur]; // include the escaped char
			} else if (terminals.indexOf(c) != -1) {
				break;
			}
			sb.append(c);
		}

		cursor = cur;
		return sb.toString();
	}

	public List<String> getEscapedTokens(String terminals) {
		List<String> result = new ArrayList<>();
		for (String token = getEscapedToken(terminals); token != null; token = getEscapedToken(terminals)) {
			result.add(token);
			getChar(); // consume terminal
		}
		return result;
	}

	public char getChar() {
		int cur = cursor;
		if (cur < max) {
			cursor = cur + 1;
			return (value[cur]);
		}
		return ('\0'); /* end of value */
	}

}
