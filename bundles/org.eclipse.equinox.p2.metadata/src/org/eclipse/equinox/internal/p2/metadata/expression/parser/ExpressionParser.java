/*******************************************************************************
 * Copyright (c) 2010, 2016 Cloudsmith Inc. and others.
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
import org.eclipse.equinox.internal.p2.metadata.expression.IExpressionConstants;
import org.eclipse.equinox.internal.p2.metadata.expression.LDAPApproximation;
import org.eclipse.equinox.p2.metadata.expression.*;

public class ExpressionParser extends Stack<IExpression> implements IExpressionConstants, IExpressionParser {
	private static final long serialVersionUID = 5481439062356612378L;

	protected static final int TOKEN_OR = 1;
	protected static final int TOKEN_AND = 2;

	protected static final int TOKEN_EQUAL = 10;
	protected static final int TOKEN_NOT_EQUAL = 11;
	protected static final int TOKEN_LESS = 12;
	protected static final int TOKEN_LESS_EQUAL = 13;
	protected static final int TOKEN_GREATER = 14;
	protected static final int TOKEN_GREATER_EQUAL = 15;
	protected static final int TOKEN_MATCHES = 16;

	protected static final int TOKEN_NOT = 20;
	protected static final int TOKEN_DOT = 21;
	protected static final int TOKEN_COMMA = 22;
	protected static final int TOKEN_PIPE = 23;
	protected static final int TOKEN_DOLLAR = 24;
	protected static final int TOKEN_IF = 25;
	protected static final int TOKEN_ELSE = 26;

	protected static final int TOKEN_LP = 30;
	protected static final int TOKEN_RP = 31;
	protected static final int TOKEN_LB = 32;
	protected static final int TOKEN_RB = 33;
	protected static final int TOKEN_LC = 34;
	protected static final int TOKEN_RC = 35;

	protected static final int TOKEN_IDENTIFIER = 40;
	protected static final int TOKEN_LITERAL = 41;

	protected static final int TOKEN_NULL = 50;
	protected static final int TOKEN_TRUE = 51;
	protected static final int TOKEN_FALSE = 52;

	private static final int TOKEN_ALL = 60;
	private static final int TOKEN_EXISTS = 61;

	protected static final int TOKEN_END = 0;
	protected static final int TOKEN_ERROR = -1;

	protected static final Map<String, Integer> keywords;
	static {
		keywords = new HashMap<String, Integer>();
		keywords.put(KEYWORD_FALSE, Integer.valueOf(TOKEN_FALSE));
		keywords.put(KEYWORD_NULL, Integer.valueOf(TOKEN_NULL));
		keywords.put(KEYWORD_TRUE, Integer.valueOf(TOKEN_TRUE));
		keywords.put(KEYWORD_ALL, Integer.valueOf(TOKEN_ALL));
		keywords.put(KEYWORD_EXISTS, Integer.valueOf(TOKEN_EXISTS));
	}

	protected final IExpressionFactory factory;

	protected String expression;
	protected int tokenPos;
	protected int currentToken;
	protected int lastTokenPos;
	protected Object tokenValue;
	protected String rootVariable;

	public ExpressionParser(IExpressionFactory factory) {
		this.factory = factory;
	}

	public synchronized IExpression parse(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		IExpression thisVariable = factory.thisVariable();
		rootVariable = ExpressionUtil.getName(thisVariable);
		push(thisVariable);
		try {
			nextToken();
			IExpression expr = currentToken == TOKEN_END ? factory.constant(Boolean.TRUE) : parseCondition();
			assertToken(TOKEN_END);
			return expr;
		} finally {
			clear(); // pop all items
		}
	}

	public synchronized IExpression parseQuery(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		rootVariable = VARIABLE_EVERYTHING;
		IExpression everythingVariable = factory.variable(VARIABLE_EVERYTHING);
		push(everythingVariable);
		try {
			nextToken();
			IExpression expr = parseCondition();
			assertToken(TOKEN_END);
			return expr;
		} finally {
			clear(); // pop all items
		}
	}

	protected Map<String, Integer> keywordToTokenMap() {
		return keywords;
	}

	protected IExpression parseCondition() {
		// Just a hook in this parser. Conditions are not supported
		return parseOr();
	}

	protected IExpression parseOr() {
		IExpression expr = parseAnd();
		if (currentToken != TOKEN_OR)
			return expr;

		ArrayList<IExpression> exprs = new ArrayList<IExpression>();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseAnd());
		} while (currentToken == TOKEN_OR);
		return factory.or(exprs.toArray(new IExpression[exprs.size()]));
	}

	protected IExpression parseAnd() {
		IExpression expr = parseBinary();
		if (currentToken != TOKEN_AND)
			return expr;

		ArrayList<IExpression> exprs = new ArrayList<IExpression>();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseBinary());
		} while (currentToken == TOKEN_AND);
		return factory.and(exprs.toArray(new IExpression[exprs.size()]));
	}

	protected IExpression parseBinary() {
		IExpression expr = parseNot();
		for (;;) {
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
					IExpression rhs;
					if (realToken == TOKEN_MATCHES && currentToken == TOKEN_LITERAL && tokenValue instanceof String)
						rhs = factory.constant(new LDAPApproximation((String) tokenValue));
					else
						rhs = parseNot();
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
							expr = factory.greaterEqual(expr, rhs);
							break;
						case TOKEN_LESS :
							expr = factory.less(expr, rhs);
							break;
						case TOKEN_LESS_EQUAL :
							expr = factory.lessEqual(expr, rhs);
							break;
						default :
							expr = factory.matches(expr, rhs);
					}
					continue;
				default :
					throw syntaxError();
			}
			break;
		}
		return expr;
	}

	protected IExpression parseNot() {
		if (currentToken == TOKEN_NOT) {
			nextToken();
			IExpression expr = parseNot();
			return factory.not(expr);
		}
		return parseCollectionExpression();
	}

	protected IExpression parseCollectionExpression() {
		IExpression expr = parseCollectionLHS();
		if (expr == null) {
			expr = parseMember();
			if (currentToken != TOKEN_DOT)
				return expr;
			nextToken();
		}
		for (;;) {
			int funcToken = currentToken;
			nextToken();
			assertToken(TOKEN_LP);
			nextToken();
			expr = parseCollectionRHS(expr, funcToken);
			if (currentToken != TOKEN_DOT)
				break;
			nextToken();
		}
		return expr;
	}

	protected IExpression parseCollectionLHS() {
		IExpression expr = null;
		switch (currentToken) {
			case TOKEN_EXISTS :
			case TOKEN_ALL :
				expr = getVariableOrRootMember(rootVariable);
				break;
		}
		return expr;
	}

	protected IExpression parseCollectionRHS(IExpression expr, int funcToken) {
		switch (funcToken) {
			case TOKEN_EXISTS :
				expr = factory.exists(expr, parseLambdaDefinition());
				break;
			case TOKEN_ALL :
				expr = factory.all(expr, parseLambdaDefinition());
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	protected IExpression parseLambdaDefinition() {
		assertToken(TOKEN_IDENTIFIER);
		IExpression each = factory.variable((String) tokenValue);
		push(each);
		try {
			nextToken();
			assertToken(TOKEN_PIPE);
			nextToken();
			IExpression body = parseCondition();
			assertToken(TOKEN_RP);
			nextToken();
			return factory.lambda(each, body);
		} finally {
			pop();
		}
	}

	protected IExpression parseMember() {
		IExpression expr = parseUnary();
		String name;
		while (currentToken == TOKEN_DOT || currentToken == TOKEN_LB) {
			int savePos = tokenPos;
			int saveToken = currentToken;
			Object saveTokenValue = tokenValue;
			nextToken();
			if (saveToken == TOKEN_DOT) {
				switch (currentToken) {
					case TOKEN_IDENTIFIER :
						name = (String) tokenValue;
						nextToken();
						expr = factory.member(expr, name);
						break;

					default :
						tokenPos = savePos;
						currentToken = saveToken;
						tokenValue = saveTokenValue;
						return expr;
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

	protected IExpression parseUnary() {
		IExpression expr;
		switch (currentToken) {
			case TOKEN_LP :
				nextToken();
				expr = parseCondition();
				assertToken(TOKEN_RP);
				nextToken();
				break;
			case TOKEN_LITERAL :
				expr = factory.constant(tokenValue);
				nextToken();
				break;
			case TOKEN_IDENTIFIER :
				expr = getVariableOrRootMember((String) tokenValue);
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
			case TOKEN_DOLLAR :
				expr = parseParameter();
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	private IExpression parseParameter() {
		if (currentToken == TOKEN_DOLLAR) {
			nextToken();
			if (currentToken == TOKEN_LITERAL && tokenValue instanceof Integer) {
				IExpression param = factory.indexedParameter(((Integer) tokenValue).intValue());
				nextToken();
				return param;
			}
		}
		throw syntaxError();
	}

	protected IExpression[] parseArray() {
		IExpression expr = parseCondition();
		if (currentToken != TOKEN_COMMA)
			return new IExpression[] {expr};

		ArrayList<IExpression> operands = new ArrayList<IExpression>();
		operands.add(expr);
		do {
			nextToken();
			if (currentToken == TOKEN_LC)
				// We don't allow lambdas in the array
				break;
			operands.add(parseCondition());
		} while (currentToken == TOKEN_COMMA);
		return operands.toArray(new IExpression[operands.size()]);
	}

	protected void assertToken(int token) {
		if (currentToken != token)
			throw syntaxError();
	}

	protected IExpression getVariableOrRootMember(String id) {
		int idx = size();
		while (--idx >= 0) {
			IExpression v = get(idx);
			if (id.equals(v.toString()))
				return v;
		}

		if (rootVariable == null || rootVariable.equals(id))
			throw syntaxError("No such variable: " + id); //$NON-NLS-1$

		return factory.member(getVariableOrRootMember(rootVariable), id);
	}

	protected void nextToken() {
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
					tokenValue = OPERATOR_AND;
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
					tokenValue = OPERATOR_MATCHES;
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
			case '\'' :
				parseDelimitedString(c);
				break;

			case '/' :
				parseDelimitedString(c);
				if (currentToken == TOKEN_LITERAL)
					tokenValue = SimplePattern.compile((String) tokenValue);
				break;

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
					Integer token = keywordToTokenMap().get(word);
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

	protected void popVariable() {
		if (isEmpty())
			throw syntaxError();
		pop();
	}

	protected ExpressionParseException syntaxError() {
		Object tv = tokenValue;
		if (tv == null) {
			if (lastTokenPos >= expression.length())
				return syntaxError("Unexpected end of expression"); //$NON-NLS-1$
			tv = expression.substring(lastTokenPos, lastTokenPos + 1);
		}
		return syntaxError("Unexpected token \"" + tv + '"'); //$NON-NLS-1$
	}

	protected ExpressionParseException syntaxError(String message) {
		return new ExpressionParseException(expression, message, tokenPos);
	}

	private void parseDelimitedString(char delim) {
		int start = ++tokenPos;
		StringBuffer buf = new StringBuffer();
		int top = expression.length();
		while (tokenPos < top) {
			char ec = expression.charAt(tokenPos);
			if (ec == delim)
				break;
			if (ec == '\\') {
				if (++tokenPos == top)
					break;
				ec = expression.charAt(tokenPos);
			}
			buf.append(ec);
			++tokenPos;
		}
		if (tokenPos == top) {
			tokenPos = start - 1;
			currentToken = TOKEN_ERROR;
		} else {
			++tokenPos;
			tokenValue = buf.toString();
			currentToken = TOKEN_LITERAL;
		}
	}
}
