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
package org.eclipse.equinox.internal.p2.ql.parser;

import org.eclipse.equinox.p2.ql.IExpression;

import java.util.*;
import org.eclipse.equinox.p2.ql.*;

public class ExpressionParser extends Stack implements IParserConstants, IExpressionParser {
	private static final long serialVersionUID = 882034383978853143L;

	private static final int TOKEN_OR = 1;
	private static final int TOKEN_AND = 2;

	private static final int TOKEN_EQUAL = 10;
	private static final int TOKEN_NOT_EQUAL = 11;
	private static final int TOKEN_LESS = 12;
	private static final int TOKEN_LESS_EQUAL = 13;
	private static final int TOKEN_GREATER = 14;
	private static final int TOKEN_GREATER_EQUAL = 15;
	private static final int TOKEN_MATCHES = 16;

	private static final int TOKEN_NOT = 20;
	private static final int TOKEN_DOT = 21;
	private static final int TOKEN_COMMA = 22;
	private static final int TOKEN_PIPE = 23;
	private static final int TOKEN_DOLLAR = 24;
	private static final int TOKEN_IF = 25;
	private static final int TOKEN_ELSE = 26;

	private static final int TOKEN_LP = 30;
	private static final int TOKEN_RP = 31;
	private static final int TOKEN_LB = 32;
	private static final int TOKEN_RB = 33;
	private static final int TOKEN_LC = 34;
	private static final int TOKEN_RC = 35;

	private static final int TOKEN_IDENTIFIER = 40;
	private static final int TOKEN_LITERAL = 41;
	private static final int TOKEN_ANY = 42;

	private static final int TOKEN_NULL = 50;
	private static final int TOKEN_TRUE = 51;
	private static final int TOKEN_FALSE = 52;

	private static final int TOKEN_LATEST = 60;
	private static final int TOKEN_LIMIT = 61;
	private static final int TOKEN_FIRST = 62;
	private static final int TOKEN_FLATTEN = 63;
	private static final int TOKEN_UNIQUE = 64;
	private static final int TOKEN_SELECT = 65;
	private static final int TOKEN_COLLECT = 66;
	private static final int TOKEN_TRAVERSE = 67;
	private static final int TOKEN_EXISTS = 68;
	private static final int TOKEN_ALL = 69;

	private static final int TOKEN_END = 0;
	private static final int TOKEN_ERROR = -1;

	private static final Map keywords;
	static {
		keywords = new HashMap();
		keywords.put(KEYWORD_ALL, new Integer(TOKEN_ALL));
		keywords.put(KEYWORD_COLLECT, new Integer(TOKEN_COLLECT));
		keywords.put(KEYWORD_EXISTS, new Integer(TOKEN_EXISTS));
		keywords.put(KEYWORD_FALSE, new Integer(TOKEN_FALSE));
		keywords.put(KEYWORD_FIRST, new Integer(TOKEN_FIRST));
		keywords.put(KEYWORD_FLATTEN, new Integer(TOKEN_FLATTEN));
		keywords.put(KEYWORD_LATEST, new Integer(TOKEN_LATEST));
		keywords.put(KEYWORD_LIMIT, new Integer(TOKEN_LIMIT));
		keywords.put(KEYWORD_NULL, new Integer(TOKEN_NULL));
		keywords.put(KEYWORD_SELECT, new Integer(TOKEN_SELECT));
		keywords.put(KEYWORD_TRAVERSE, new Integer(TOKEN_TRAVERSE));
		keywords.put(KEYWORD_TRUE, new Integer(TOKEN_TRUE));
		keywords.put(KEYWORD_UNIQUE, new Integer(TOKEN_UNIQUE));
		keywords.put(OPERATOR_EACH, new Integer(TOKEN_ANY));
	}

	private final IExpressionFactory factory;

	private String expression;
	private int tokenPos;
	private int currentToken;
	private int lastTokenPos;
	private Object tokenValue;
	private String rootVariable;

	public ExpressionParser(IExpressionFactory factory) {
		this.factory = factory;
	}

	public synchronized IMatchExpression parsePredicate(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		rootVariable = IExpression.VARIABLE_ITEM;
		IExpression itemVariable = factory.variable(IExpression.VARIABLE_ITEM);
		push(itemVariable);
		try {
			nextToken();
			IExpression expr = currentToken == TOKEN_END ? factory.constant(Boolean.TRUE) : parseCondition();
			assertToken(TOKEN_END);
			return factory.matchExpression(expr);
		} finally {
			popVariable(); // pop item
		}
	}

	public synchronized IContextExpression parseQuery(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		rootVariable = IExpression.VARIABLE_EVERYTHING;
		IExpression everythingVariable = factory.variable(IExpression.VARIABLE_EVERYTHING);
		push(everythingVariable);
		try {
			nextToken();
			IExpression expr = parseCondition();
			assertToken(TOKEN_END);
			return factory.contextExpression(expr);
		} finally {
			popVariable(); // pop context
		}
	}

	private IExpression parseCondition() {
		IExpression expr = parseOr();
		if (currentToken == TOKEN_IF) {
			nextToken();
			IExpression ifTrue = parseOr();
			assertToken(TOKEN_ELSE);
			nextToken();
			expr = factory.condition(expr, ifTrue, parseOr());
		}
		return expr;
	}

	private IExpression parseOr() {
		IExpression expr = parseAnd();
		if (currentToken != TOKEN_OR)
			return expr;

		ArrayList exprs = new ArrayList();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseAnd());
		} while (currentToken == TOKEN_OR);
		return factory.or((IExpression[]) exprs.toArray(new IExpression[exprs.size()]));
	}

	private IExpression parseAnd() {
		IExpression expr = parseBinary();
		if (currentToken != TOKEN_AND)
			return expr;

		ArrayList exprs = new ArrayList();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseBinary());
		} while (currentToken == TOKEN_AND);
		return factory.and((IExpression[]) exprs.toArray(new IExpression[exprs.size()]));
	}

	private IExpression parseBinary() {
		IExpression expr = parseNot();
		switch (currentToken) {
			case TOKEN_OR :
			case TOKEN_AND :
			case TOKEN_RP :
			case TOKEN_RB :
			case TOKEN_RC :
			case TOKEN_COMMA :
			case TOKEN_IF :
			case TOKEN_ELSE :
			case TOKEN_END :
				break;
			case TOKEN_EQUAL :
			case TOKEN_NOT_EQUAL :
			case TOKEN_GREATER :
			case TOKEN_GREATER_EQUAL :
			case TOKEN_LESS :
			case TOKEN_LESS_EQUAL :
			case TOKEN_MATCHES :
				int realToken = currentToken;
				nextToken();
				IExpression rhs = parseNot();
				switch (realToken) {
					case TOKEN_EQUAL :
						expr = factory.equals(expr, rhs);
						break;
					case TOKEN_NOT_EQUAL :
						expr = factory.not(factory.equals(expr, rhs));
						break;
					case TOKEN_GREATER :
						expr = factory.greater(expr, rhs);
						break;
					case TOKEN_GREATER_EQUAL :
						expr = factory.not(factory.less(expr, rhs));
						break;
					case TOKEN_LESS :
						expr = factory.less(expr, rhs);
						break;
					case TOKEN_LESS_EQUAL :
						expr = factory.not(factory.greater(expr, rhs));
						break;
					default :
						expr = factory.matches(expr, rhs);
				}
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	private IExpression parseNot() {
		if (currentToken == TOKEN_NOT) {
			nextToken();
			IExpression expr = parseNot();
			return factory.not(expr);
		}
		return parseCollectionExpression();
	}

	private IExpression parseCollectionExpression() {
		IExpression expr;
		switch (currentToken) {
			case TOKEN_SELECT :
			case TOKEN_COLLECT :
			case TOKEN_EXISTS :
			case TOKEN_FIRST :
			case TOKEN_FLATTEN :
			case TOKEN_ALL :
			case TOKEN_TRAVERSE :
			case TOKEN_LATEST :
			case TOKEN_LIMIT :
			case TOKEN_UNIQUE :
				expr = getVariableOrRootMember(rootVariable);
				break;
			default :
				expr = parseMember();
				if (currentToken != TOKEN_DOT)
					return expr;
				nextToken();
		}

		for (;;) {
			int filterToken = currentToken;
			nextToken();
			assertToken(TOKEN_LP);
			nextToken();
			switch (filterToken) {
				case TOKEN_SELECT :
					expr = factory.select(expr, parseLambdaDefinition());
					break;
				case TOKEN_COLLECT :
					expr = factory.collect(expr, parseLambdaDefinition());
					break;
				case TOKEN_EXISTS :
					expr = factory.exists(expr, parseLambdaDefinition());
					break;
				case TOKEN_FIRST :
					expr = factory.first(expr, parseLambdaDefinition());
					break;
				case TOKEN_ALL :
					expr = factory.all(expr, parseLambdaDefinition());
					break;
				case TOKEN_TRAVERSE :
					expr = factory.traverse(expr, parseLambdaDefinition());
					break;
				case TOKEN_LATEST :
					if (currentToken == TOKEN_RP) {
						expr = factory.latest(expr);
						assertToken(TOKEN_RP);
						nextToken();
					} else
						expr = factory.latest(factory.select(expr, parseLambdaDefinition()));
					break;
				case TOKEN_FLATTEN :
					if (currentToken == TOKEN_RP) {
						expr = factory.flatten(expr);
						assertToken(TOKEN_RP);
						nextToken();
					} else
						expr = factory.flatten(factory.select(expr, parseLambdaDefinition()));
					break;
				case TOKEN_LIMIT :
					expr = factory.limit(expr, parseCondition());
					assertToken(TOKEN_RP);
					nextToken();
					break;
				case TOKEN_UNIQUE :
					if (currentToken == TOKEN_RP)
						expr = factory.unique(expr, factory.constant(null));
					else {
						expr = factory.unique(expr, parseMember());
						assertToken(TOKEN_RP);
						nextToken();
					}
					break;
				default :
					throw syntaxError();
			}
			if (currentToken != TOKEN_DOT)
				break;
			nextToken();
		}
		return expr;
	}

	private IExpression parseMember() {
		IExpression expr = parseConstructor();
		String name;
		while (currentToken == TOKEN_DOT || currentToken == TOKEN_LB) {
			int savePos = tokenPos;
			int saveToken = currentToken;
			Object saveTokenValue = tokenValue;
			nextToken();
			if (saveToken == TOKEN_DOT) {
				switch (currentToken) {
					case TOKEN_SELECT :
					case TOKEN_COLLECT :
					case TOKEN_EXISTS :
					case TOKEN_FIRST :
					case TOKEN_FLATTEN :
					case TOKEN_ALL :
					case TOKEN_TRAVERSE :
					case TOKEN_LATEST :
					case TOKEN_LIMIT :
					case TOKEN_UNIQUE :
						tokenPos = savePos;
						currentToken = saveToken;
						tokenValue = saveTokenValue;
						return expr;

					case TOKEN_IDENTIFIER :
						name = (String) tokenValue;
						nextToken();
						if (currentToken == TOKEN_LP) {
							nextToken();
							IExpression[] callArgs = parseArray();
							assertToken(TOKEN_RP);
							nextToken();
							expr = factory.memberCall(expr, name, callArgs);
						} else
							expr = factory.memberCall(expr, name, IExpressionFactory.NO_ARGS);
						break;

					default :
						throw syntaxError();
				}
			} else {
				IExpression atExpr = parseMember();
				assertToken(TOKEN_RB);
				nextToken();
				expr = factory.at(expr, atExpr);
			}
		}
		return expr;
	}

	private IExpression parseLambdaDefinition() {
		boolean endingRC = false;
		int anyIndex = -1;
		IExpression[] initializers = IExpressionFactory.NO_ARGS;
		IExpression[] variables;
		if (currentToken == TOKEN_LC) {
			// Lambda starts without currying.
			endingRC = true;
			nextToken();
			anyIndex = 0;
			variables = parseVariables();
			if (variables == null)
				// empty means no pipe at the end.
				throw syntaxError();
		} else {
			anyIndex = 0;
			variables = parseVariables();
			if (variables == null) {
				anyIndex = -1;
				initializers = parseArray();
				assertToken(TOKEN_LC);
				nextToken();
				endingRC = true;
				for (int idx = 0; idx < initializers.length; ++idx) {
					IExpression initializer = initializers[idx];
					if (initializer.getExpressionType() == IExpression.TYPE_VARIABLE && OPERATOR_EACH.equals(initializer.toString())) {
						if (anyIndex == -1)
							anyIndex = idx;
						else
							anyIndex = -1; // Second Each. This is illegal
						break;
					}
				}
				if (anyIndex == -1)
					throw new IllegalArgumentException("Exaclty one _ must be present among the currying expressions"); //$NON-NLS-1$

				variables = parseVariables();
				if (variables == null)
					// empty means no pipe at the end.
					throw syntaxError();
			}

		}
		nextToken();
		IExpression body = parseCondition();
		if (endingRC) {
			assertToken(TOKEN_RC);
			nextToken();
		}

		assertToken(TOKEN_RP);
		nextToken();
		IExpression each;
		IExpression[] assignments;
		if (initializers.length == 0) {
			if (variables.length != 1)
				throw new IllegalArgumentException("Must have exactly one variable unless currying is used"); //$NON-NLS-1$
			each = variables[0];
			assignments = IExpressionFactory.NO_ARGS;
		} else {
			if (initializers.length != variables.length)
				throw new IllegalArgumentException("Number of currying expressions and variables differ"); //$NON-NLS-1$

			if (initializers.length == 1) {
				// This is just a map from _ to some variable
				each = variables[0];
				assignments = IExpressionFactory.NO_ARGS;
			} else {
				int idx;
				each = variables[anyIndex];
				assignments = new IExpression[initializers.length - 1];
				for (idx = 0; idx < anyIndex; ++idx)
					assignments[idx] = factory.assignment(variables[idx], initializers[idx]);
				for (++idx; idx < initializers.length; ++idx)
					assignments[idx] = factory.assignment(variables[idx], initializers[idx]);
			}
		}
		return factory.lambda(each, body, assignments);
	}

	private IExpression[] parseVariables() {
		int savePos = tokenPos;
		int saveToken = currentToken;
		Object saveTokenValue = tokenValue;
		List ids = null;
		while (currentToken == TOKEN_IDENTIFIER) {
			if (ids == null)
				ids = new ArrayList();
			ids.add(tokenValue);
			nextToken();
			if (currentToken == TOKEN_COMMA) {
				nextToken();
				continue;
			}
			break;
		}

		if (currentToken != TOKEN_PIPE) {
			// This was not a variable list
			tokenPos = savePos;
			currentToken = saveToken;
			tokenValue = saveTokenValue;
			return null;
		}

		if (ids == null)
			// Empty list but otherwise OK
			return IExpressionFactory.NO_ARGS;

		int top = ids.size();
		IExpression[] result = new IExpression[top];
		for (int idx = 0; idx < top; ++idx) {
			String name = (String) ids.get(idx);
			IExpression var = factory.variable(name);
			push(var);
			result[idx] = var;
		}
		return result;
	}

	private IExpression parseConstructor() {
		if (currentToken == TOKEN_IDENTIFIER) {
			int savePos = tokenPos;
			int saveToken = currentToken;
			Object saveTokenValue = tokenValue;

			Object function = factory.getFunctionMap().get(tokenValue);
			if (function != null) {
				nextToken();
				if (currentToken == TOKEN_LP) {
					nextToken();
					IExpression[] args = currentToken == TOKEN_RP ? IExpressionFactory.NO_ARGS : parseArray();
					assertToken(TOKEN_RP);
					nextToken();
					return factory.function(function, args);
				}
				tokenPos = savePos;
				currentToken = saveToken;
				tokenValue = saveTokenValue;
			}
		}
		return parseUnary();
	}

	private IExpression parseUnary() {
		IExpression expr;
		switch (currentToken) {
			case TOKEN_LP :
				nextToken();
				expr = parseCondition();
				assertToken(TOKEN_RP);
				nextToken();
				break;
			case TOKEN_LB :
				nextToken();
				expr = factory.array(parseArray());
				assertToken(TOKEN_RB);
				nextToken();
				break;
			case TOKEN_LITERAL :
				expr = factory.constant(tokenValue);
				nextToken();
				break;
			case TOKEN_DOLLAR :
				expr = parseParameter();
				break;
			case TOKEN_IDENTIFIER :
				expr = getVariableOrRootMember((String) tokenValue);
				nextToken();
				break;
			case TOKEN_ANY :
				expr = factory.variable(OPERATOR_EACH);
				nextToken();
				break;
			case TOKEN_NULL :
				expr = factory.constant(null);
				nextToken();
				break;
			case TOKEN_TRUE :
				expr = factory.constant(Boolean.TRUE);
				nextToken();
				break;
			case TOKEN_FALSE :
				expr = factory.constant(Boolean.FALSE);
				nextToken();
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	private IExpression parseParameter() {
		if (currentToken == TOKEN_DOLLAR) {
			nextToken();

			IExpression param = null;
			if (currentToken == TOKEN_LITERAL && tokenValue instanceof Integer)
				param = factory.indexedParameter(((Integer) tokenValue).intValue());
			else if (currentToken == TOKEN_IDENTIFIER)
				param = factory.keyedParameter((String) tokenValue);

			if (param != null) {
				nextToken();
				return param;
			}
		}
		throw syntaxError();
	}

	private IExpression[] parseArray() {
		IExpression expr = parseCondition();
		if (currentToken != TOKEN_COMMA)
			return new IExpression[] {expr};

		ArrayList operands = new ArrayList();
		operands.add(expr);
		do {
			nextToken();
			if (currentToken == TOKEN_LC)
				// We don't allow lambdas in the array
				break;
			operands.add(parseCondition());
		} while (currentToken == TOKEN_COMMA);
		return (IExpression[]) operands.toArray(new IExpression[operands.size()]);
	}

	private void assertToken(int token) {
		if (currentToken != token)
			throw syntaxError();
	}

	private IExpression getVariableOrRootMember(String id) {
		int idx = size();
		while (--idx >= 0) {
			IExpression v = (IExpression) get(idx);
			if (id.equals(v.toString()))
				return v;
		}

		if (rootVariable.equals(id))
			throw syntaxError("No such variable: " + id); //$NON-NLS-1$

		return factory.memberCall(getVariableOrRootMember(rootVariable), id, IExpressionFactory.NO_ARGS);
	}

	private void nextToken() {
		tokenValue = null;
		int top = expression.length();
		char c = 0;
		while (tokenPos < top) {
			c = expression.charAt(tokenPos);
			if (!Character.isWhitespace(c))
				break;
			++tokenPos;
		}
		if (tokenPos >= top) {
			lastTokenPos = top;
			currentToken = TOKEN_END;
			return;
		}

		lastTokenPos = tokenPos;
		switch (c) {
			case '|' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '|') {
					tokenValue = OPERATOR_OR;
					currentToken = TOKEN_OR;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_PIPE;
					++tokenPos;
				}
				break;

			case '&' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '&') {
					tokenValue = OPERATOR_ARRAY;
					currentToken = TOKEN_AND;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '=' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = OPERATOR_EQUALS;
					currentToken = TOKEN_EQUAL;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '!' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = OPERATOR_NOT_EQUALS;
					currentToken = TOKEN_NOT_EQUAL;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_NOT;
					++tokenPos;
				}
				break;

			case '~' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = IParserConstants.OPERATOR_MATCHES;
					currentToken = TOKEN_MATCHES;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '>' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = OPERATOR_GT_EQUAL;
					currentToken = TOKEN_GREATER_EQUAL;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_GREATER;
					++tokenPos;
				}
				break;

			case '<' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = OPERATOR_LT_EQUAL;
					currentToken = TOKEN_LESS_EQUAL;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_LESS;
					++tokenPos;
				}
				break;

			case '?' :
				currentToken = TOKEN_IF;
				++tokenPos;
				break;

			case ':' :
				currentToken = TOKEN_ELSE;
				++tokenPos;
				break;

			case '.' :
				currentToken = TOKEN_DOT;
				++tokenPos;
				break;

			case '$' :
				currentToken = TOKEN_DOLLAR;
				++tokenPos;
				break;

			case '{' :
				currentToken = TOKEN_LC;
				++tokenPos;
				break;

			case '}' :
				currentToken = TOKEN_RC;
				++tokenPos;
				break;

			case '(' :
				currentToken = TOKEN_LP;
				++tokenPos;
				break;

			case ')' :
				currentToken = TOKEN_RP;
				++tokenPos;
				break;

			case '[' :
				currentToken = TOKEN_LB;
				++tokenPos;
				break;

			case ']' :
				currentToken = TOKEN_RB;
				++tokenPos;
				break;

			case ',' :
				currentToken = TOKEN_COMMA;
				++tokenPos;
				break;

			case '"' :
			case '\'' : {
				int start = ++tokenPos;
				while (tokenPos < top && expression.charAt(tokenPos) != c)
					++tokenPos;
				if (tokenPos == top) {
					tokenPos = start - 1;
					currentToken = TOKEN_ERROR;
				} else {
					tokenValue = expression.substring(start, tokenPos++);
					currentToken = TOKEN_LITERAL;
				}
				break;
			}

			case '/' : {
				int start = ++tokenPos;
				StringBuffer buf = new StringBuffer();
				while (tokenPos < top) {
					c = expression.charAt(tokenPos);
					if (c == '\\' && tokenPos + 1 < top) {
						c = expression.charAt(++tokenPos);
						if (c != '/')
							buf.append('\\');
					} else if (c == '/')
						break;
					buf.append(c);
					++tokenPos;
				}
				if (tokenPos == top) {
					tokenPos = start - 1;
					currentToken = TOKEN_ERROR;
				} else {
					tokenValue = SimplePattern.compile(expression.substring(start, tokenPos++));
					currentToken = TOKEN_LITERAL;
				}
				break;
			}

			default :
				if (Character.isDigit(c)) {
					int start = tokenPos++;
					while (tokenPos < top && Character.isDigit(expression.charAt(tokenPos)))
						++tokenPos;
					tokenValue = Integer.valueOf(expression.substring(start, tokenPos));
					currentToken = TOKEN_LITERAL;
					break;
				}
				if (Character.isJavaIdentifierStart(c)) {
					int start = tokenPos++;
					while (tokenPos < top && Character.isJavaIdentifierPart(expression.charAt(tokenPos)))
						++tokenPos;
					String word = expression.substring(start, tokenPos);
					Integer token = (Integer) keywords.get(word);
					if (token == null)
						currentToken = TOKEN_IDENTIFIER;
					else
						currentToken = token.intValue();
					tokenValue = word;
					break;
				}
				throw syntaxError();
		}
	}

	private void popVariable() {
		if (isEmpty())
			throw syntaxError();
		pop();
	}

	private QLParseException syntaxError() {
		Object tv = tokenValue;
		if (tv == null) {
			if (lastTokenPos >= expression.length())
				return syntaxError("Unexpeced end of expression"); //$NON-NLS-1$
			tv = expression.substring(lastTokenPos, lastTokenPos + 1);
		}
		return syntaxError("Unexpected token \"" + tv + '"'); //$NON-NLS-1$
	}

	private QLParseException syntaxError(String message) {
		return new QLParseException(expression, message, tokenPos);
	}
}
