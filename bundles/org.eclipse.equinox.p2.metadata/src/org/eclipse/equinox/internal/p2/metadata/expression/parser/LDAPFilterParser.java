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
package org.eclipse.equinox.internal.p2.metadata.expression.parser;

import java.util.*;
import org.eclipse.equinox.internal.p2.metadata.Messages;
import org.eclipse.equinox.internal.p2.metadata.expression.IExpressionConstants;
import org.eclipse.equinox.internal.p2.metadata.expression.LDAPApproximation;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.osgi.util.NLS;

/**
 * Parser class for OSGi filter strings. This class parses the complete filter string and builds a tree of Filter
 * objects rooted at the parent.
 */
public class LDAPFilterParser {
	@SuppressWarnings("serial")
	private static final Map<String, IFilterExpression> filterCache = Collections.<String, IFilterExpression> synchronizedMap(new LinkedHashMap<String, IFilterExpression>() {
		public boolean removeEldestEntry(Map.Entry<String, IFilterExpression> expr) {
			return size() > 64;
		}
	});

	private final IExpressionFactory factory;

	private final IExpression self;

	private final StringBuffer sb = new StringBuffer();

	private String filterString;

	private int position;

	public LDAPFilterParser(IExpressionFactory factory) {
		this.factory = factory;
		self = factory.variable(IExpressionConstants.VARIABLE_THIS);
		position = 0;
	}

	public IFilterExpression parse(String filterStr) {
		IFilterExpression filter = filterCache.get(filterStr);
		if (filter != null)
			return filter;

		synchronized (this) {
			filterString = filterStr;
			position = 0;
			try {
				IExpression expr = parseFilter();
				if (position != filterString.length())
					throw syntaxException(Messages.filter_trailing_characters);
				filter = factory.filterExpression(expr);
				filterCache.put(filterStr, filter);
				return filter;
			} catch (StringIndexOutOfBoundsException e) {
				throw syntaxException(Messages.filter_premature_end);
			}
		}
	}

	private IExpression parseAnd() {
		skipWhiteSpace();
		char c = filterString.charAt(position);
		if (c != '(')
			throw syntaxException(Messages.filter_missing_leftparen);

		ArrayList<IExpression> operands = new ArrayList<IExpression>();
		while (c == '(') {
			IExpression child = parseFilter();
			if (!operands.contains(child))
				operands.add(child);
			c = filterString.charAt(position);
		}
		// int sz = operands.size();
		// return sz == 1 ? operands.get(0) : factory.and(operands.toArray(new IExpression[sz]));
		return factory.normalize(operands, IExpression.TYPE_AND);
	}

	private IExpression parseAttr() {
		skipWhiteSpace();

		int begin = position;
		int end = position;

		char c = filterString.charAt(begin);
		while (!(c == '~' || c == '<' || c == '>' || c == '=' || c == '(' || c == ')')) {
			position++;
			if (!Character.isWhitespace(c))
				end = position;
			c = filterString.charAt(position);
		}
		if (end == begin)
			throw syntaxException(Messages.filter_missing_attr);
		return factory.member(self, filterString.substring(begin, end));
	}

	private IExpression parseFilter() {
		IExpression filter;
		skipWhiteSpace();

		if (filterString.charAt(position) != '(')
			throw syntaxException(Messages.filter_missing_leftparen);

		position++;
		filter = parseFiltercomp();

		skipWhiteSpace();

		if (filterString.charAt(position) != ')')
			throw syntaxException(Messages.filter_missing_rightparen);

		position++;
		skipWhiteSpace();

		return filter;
	}

	private IExpression parseFiltercomp() {
		skipWhiteSpace();

		char c = filterString.charAt(position);

		switch (c) {
			case '&' : {
				position++;
				return parseAnd();
			}
			case '|' : {
				position++;
				return parseOr();
			}
			case '!' : {
				position++;
				return parseNot();
			}
		}
		return parseItem();
	}

	private IExpression parseItem() {
		IExpression attr = parseAttr();

		skipWhiteSpace();
		String value;

		boolean[] hasWild = {false};
		char c = filterString.charAt(position);
		switch (c) {
			case '~' :
			case '>' :
			case '<' :
				if (filterString.charAt(position + 1) != '=')
					throw syntaxException(Messages.filter_invalid_operator);
				position += 2;
				int savePos = position;
				value = parseValue(hasWild);
				if (hasWild[0]) {
					// Unescaped wildcard found. This is not legal for the given operator
					position = savePos;
					throw syntaxException(Messages.filter_invalid_value);
				}
				switch (c) {
					case '>' :
						return factory.greaterEqual(attr, factory.constant(value));
					case '<' :
						return factory.lessEqual(attr, factory.constant(value));
				}
				return factory.matches(attr, factory.constant(new LDAPApproximation(value)));
			case '=' :
				position++;
				value = parseValue(hasWild);
				return hasWild[0] ? factory.matches(attr, factory.constant(SimplePattern.compile(value))) : factory.equals(attr, factory.constant(value));
		}
		throw syntaxException(Messages.filter_invalid_operator);
	}

	private IExpression parseNot() {
		skipWhiteSpace();

		if (filterString.charAt(position) != '(')
			throw syntaxException(Messages.filter_missing_leftparen);
		return factory.not(parseFilter());
	}

	private IExpression parseOr() {
		skipWhiteSpace();
		char c = filterString.charAt(position);
		if (c != '(')
			throw syntaxException(Messages.filter_missing_leftparen);

		ArrayList<IExpression> operands = new ArrayList<IExpression>();
		while (c == '(') {
			IExpression child = parseFilter();
			operands.add(child);
			c = filterString.charAt(position);
		}
		// int sz = operands.size();
		// return sz == 1 ? operands.get(0) : factory.or(operands.toArray(new IExpression[sz]));
		return factory.normalize(operands, IExpression.TYPE_OR);
	}

	private static int hexValue(char c) {
		int v;
		if (c <= '9')
			v = c - '0';
		else if (c <= 'F')
			v = (c - 'A') + 10;
		else
			v = (c - 'a') + 10;
		return v;
	}

	private String parseValue(boolean[] hasWildBin) {
		sb.setLength(0);
		int savePos = position;
		boolean hasEscapedWild = false;
		parseloop: while (true) {
			char c = filterString.charAt(position);
			switch (c) {
				case '*' :
					if (hasEscapedWild && !hasWildBin[0]) {
						// We must redo the parse.
						position = savePos;
						hasWildBin[0] = true;
						return parseValue(hasWildBin);
					}
					hasWildBin[0] = true;
					sb.append(c);
					position++;
					break;

				case ')' :
					break parseloop;

				case '(' :
					throw syntaxException(Messages.filter_invalid_value);

				case '\\' :
					c = filterString.charAt(++position);
					if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f' && position + 1 < filterString.length()) {
						char nc = filterString.charAt(position + 1);
						if (nc >= '0' && nc <= '9' || nc >= 'A' && nc <= 'F' || nc >= 'a' && nc <= 'f') {
							// Assume proper \xx escape where xx are hex digits
							++position;
							c = (char) (((hexValue(c) << 4) & 0xf0) | (hexValue(nc) & 0x0f));
							if (c == '*' && hasWildBin != null) {
								hasEscapedWild = true;
								if (hasWildBin[0])
									sb.append('\\');
							}
						}
					}
					/* fall through into default */

				default :
					sb.append(c);
					position++;
					break;
			}
		}
		if (sb.length() == 0)
			throw syntaxException(Messages.filter_missing_value);
		return sb.toString();
	}

	private void skipWhiteSpace() {
		for (int top = filterString.length(); position < top; ++position)
			if (!Character.isWhitespace(filterString.charAt(position)))
				break;
	}

	protected ExpressionParseException syntaxException(String message) {
		return new ExpressionParseException(NLS.bind(message, filterString, Integer.toString(position)));
	}
}
