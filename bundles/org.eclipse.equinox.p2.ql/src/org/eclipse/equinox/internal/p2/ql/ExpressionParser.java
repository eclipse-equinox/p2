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
package org.eclipse.equinox.internal.p2.ql;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ExpressionParser extends Stack {
	private static final long serialVersionUID = 882034383978853143L;

	static final int PRIORITY_PARAMETER = 1;
	static final int PRIORITY_LITERAL = 1;
	static final int PRIORITY_VARIABLE = 1;
	static final int PRIORITY_CONSTRUCTOR = 2;
	static final int PRIORITY_MEMBER = 3;
	static final int PRIORITY_COLLECTION = 4;
	static final int PRIORITY_NOT = 5;
	static final int PRIORITY_BINARY = 6;
	static final int PRIORITY_AND = 7;
	static final int PRIORITY_OR = 8;
	static final int PRIORITY_CONDITION = 9;
	static final int PRIORITY_LAMBDA = 10;
	static final int PRIORITY_COMMA = 11;

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
	private static final int TOKEN_SELECT = 64;
	private static final int TOKEN_REJECT = 65;
	private static final int TOKEN_COLLECT = 66;
	private static final int TOKEN_EXISTS = 67;
	private static final int TOKEN_ALL = 68;
	private static final int TOKEN_TRAVERSE = 69;
	private static final int TOKEN_UNIQUE = 70;

	private static final int TOKEN_END = 0;
	private static final int TOKEN_ERROR = -1;

	private static final Variable[] emptyVariableArray = new Variable[0];

	private static final Map constructors;

	static {
		Class[] args = new Class[] {Expression[].class};
		constructors = new HashMap();
		try {
			constructors.put(FilterConstructor.KEYWORD, FilterConstructor.class.getConstructor(args));
			constructors.put(VersionConstructor.KEYWORD, VersionConstructor.class.getConstructor(args));
			constructors.put(RangeConstructor.KEYWORD, RangeConstructor.class.getConstructor(args));
			constructors.put(SetConstructor.KEYWORD, SetConstructor.class.getConstructor(args));
			constructors.put(ClassConstructor.KEYWORD, ClassConstructor.class.getConstructor(args));
			constructors.put(WrappedIQuery.KEYWORD, WrappedIQuery.class.getConstructor(args));
			constructors.put(LocalizedKeys.KEYWORD, LocalizedKeys.class.getConstructor(args));
			constructors.put(LocalizedMap.KEYWORD, LocalizedMap.class.getConstructor(args));
			constructors.put(LocalizedProperty.KEYWORD, LocalizedProperty.class.getConstructor(args));
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private String expression;
	private int tokenPos;
	private int currentToken;
	private int lastTokenPos;
	private Object tokenValue;
	private String rootVariable;

	public synchronized ItemExpression parsePredicate(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		rootVariable = Variable.KEYWORD_ITEM;
		push(Variable.ITEM);
		try {
			nextToken();
			Expression expr = currentToken == TOKEN_END ? Constant.TRUE_CONSTANT : parseCondition();
			assertToken(TOKEN_END);
			return new ItemExpression(Variable.ITEM, expr);
		} finally {
			popVariable(); // pop item
		}
	}

	public synchronized ContextExpression parseQuery(String exprString) {
		expression = exprString;
		tokenPos = 0;
		currentToken = 0;
		tokenValue = null;
		rootVariable = Variable.KEYWORD_EVERYTHING;
		push(Variable.EVERYTHING);
		try {
			nextToken();
			Expression expr = parseCondition();
			assertToken(TOKEN_END);
			return new ContextExpression(Variable.EVERYTHING, expr);

		} finally {
			popVariable(); // pop context
		}
	}

	private Expression parseCondition() {
		Expression expr = parseOr();
		if (currentToken == TOKEN_IF) {
			nextToken();
			Expression ifTrue = parseOr();
			assertToken(TOKEN_ELSE);
			nextToken();
			expr = new Condition(expr, ifTrue, parseOr());
		}
		return expr;
	}

	private Expression parseOr() {
		Expression expr = parseAnd();
		if (currentToken != TOKEN_OR)
			return expr;

		ArrayList exprs = new ArrayList();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseAnd());
		} while (currentToken == TOKEN_OR);
		return new Or((Expression[]) exprs.toArray(new Expression[exprs.size()]));
	}

	private Expression parseAnd() {
		Expression expr = parseBinary();
		if (currentToken != TOKEN_AND)
			return expr;

		ArrayList exprs = new ArrayList();
		exprs.add(expr);
		do {
			nextToken();
			exprs.add(parseBinary());
		} while (currentToken == TOKEN_AND);
		return new And((Expression[]) exprs.toArray(new Expression[exprs.size()]));
	}

	private Expression parseBinary() {
		Expression expr = parseNot();
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
				Expression rhs = parseNot();
				switch (realToken) {
					case TOKEN_EQUAL :
						expr = new Equals(expr, rhs, false);
						break;
					case TOKEN_NOT_EQUAL :
						expr = new Equals(expr, rhs, true);
						break;
					case TOKEN_GREATER :
						expr = new Compare(expr, rhs, false, false);
						break;
					case TOKEN_GREATER_EQUAL :
						expr = new Compare(expr, rhs, false, true);
						break;
					case TOKEN_LESS :
						expr = new Compare(expr, rhs, true, false);
						break;
					case TOKEN_LESS_EQUAL :
						expr = new Compare(expr, rhs, true, true);
						break;
					default :
						expr = new Matches(expr, rhs);
				}
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	private Expression parseNot() {
		if (currentToken == TOKEN_NOT) {
			nextToken();
			Expression expr = parseNot();
			return (expr instanceof Not) ? ((Not) expr).operand : new Not(expr);
		}
		return parseCollectionExpression();
	}

	private Expression parseCollectionExpression() {
		Expression expr;
		switch (currentToken) {
			case TOKEN_SELECT :
			case TOKEN_REJECT :
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
					expr = new Select(expr, parseLambdaDefinition());
					break;
				case TOKEN_REJECT :
					expr = new Reject(expr, parseLambdaDefinition());
					break;
				case TOKEN_COLLECT :
					expr = new Collect(expr, parseLambdaDefinition());
					break;
				case TOKEN_EXISTS :
					expr = new Exists(expr, parseLambdaDefinition());
					break;
				case TOKEN_FIRST :
					expr = new First(expr, parseLambdaDefinition());
					break;
				case TOKEN_ALL :
					expr = new All(expr, parseLambdaDefinition());
					break;
				case TOKEN_TRAVERSE :
					expr = new Traverse(expr, parseLambdaDefinition());
					break;
				case TOKEN_LATEST :
					if (currentToken == TOKEN_RP) {
						expr = new Latest(expr);
						assertToken(TOKEN_RP);
						nextToken();
					} else
						expr = new Latest(new Select(expr, parseLambdaDefinition()));
					break;
				case TOKEN_FLATTEN :
					if (currentToken == TOKEN_RP) {
						expr = new Flatten(expr);
						assertToken(TOKEN_RP);
						nextToken();
					} else
						expr = new Flatten(new Select(expr, parseLambdaDefinition()));
					break;
				case TOKEN_LIMIT :
					expr = new Limit(expr, parseMember());
					assertToken(TOKEN_RP);
					nextToken();
					break;
				case TOKEN_UNIQUE :
					if (currentToken == TOKEN_RP)
						expr = new Unique(expr, Constant.NULL_CONSTANT);
					else {
						expr = new Unique(expr, parseMember());
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

	private Expression parseMember() {
		Expression expr = parseConstructor();
		while (currentToken == TOKEN_DOT || currentToken == TOKEN_LB) {
			int savePos = tokenPos;
			int saveToken = currentToken;
			Object saveTokenValue = tokenValue;
			nextToken();
			if (saveToken == TOKEN_DOT) {
				switch (currentToken) {
					case TOKEN_SELECT :
					case TOKEN_REJECT :
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
						expr = new Member(expr, (String) tokenValue);
						nextToken();
						break;

					default :
						throw syntaxError();
				}
			} else {
				Expression atExpr = parseMember();
				assertToken(TOKEN_RB);
				nextToken();
				expr = new At(expr, atExpr);
			}
		}
		return expr;
	}

	private LambdaExpression parseLambdaDefinition() {
		boolean endingRC = false;
		Expression[] initializers = Expression.emptyArray;
		Variable[] variables;
		if (currentToken == TOKEN_LC) {
			// Lambda starts without currying.
			endingRC = true;
			nextToken();
			variables = parseVariables(0);
			if (variables == null)
				// empty means no pipe at the end.
				throw syntaxError();
		} else {
			variables = parseVariables(0);
			if (variables == null) {
				initializers = parseArray();
				assertToken(TOKEN_LC);
				nextToken();
				endingRC = true;
				int anyIndex = -1;
				for (int idx = 0; idx < initializers.length; ++idx)
					if (initializers[idx] instanceof EachVariable) {
						anyIndex = idx;
						break;
					}

				variables = parseVariables(anyIndex);
				if (variables == null)
					// empty means no pipe at the end.
					throw syntaxError();
			}

		}
		nextToken();
		Expression body = parseCondition();
		if (endingRC) {
			assertToken(TOKEN_RC);
			nextToken();
		}

		assertToken(TOKEN_RP);
		nextToken();
		return new LambdaExpression(body, initializers, variables);
	}

	private Variable[] parseVariables(int anyIndex) {
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
			return emptyVariableArray;

		int top = ids.size();
		Variable[] result = new Variable[top];
		if (top == 1) {
			String name = (String) ids.get(0);
			Variable each = new EachVariable(name);
			push(each);
			result[0] = each;
		} else
			for (int idx = 0; idx < top; ++idx) {
				String name = (String) ids.get(idx);
				Variable each;
				if (idx == anyIndex)
					each = new EachVariable(name);
				else
					each = Variable.create(name);
				push(each);
				result[idx] = each;
			}
		return result;
	}

	private Expression parseConstructor() {
		if (currentToken == TOKEN_IDENTIFIER) {
			int savePos = tokenPos;
			int saveToken = currentToken;
			Object saveTokenValue = tokenValue;

			java.lang.reflect.Constructor ctor = (java.lang.reflect.Constructor) constructors.get(tokenValue);
			if (ctor != null) {
				nextToken();
				if (currentToken == TOKEN_LP) {
					// This is a constructor(<expr>)
					nextToken();
					Expression[] args = currentToken == TOKEN_RP ? Expression.emptyArray : parseArray();
					assertToken(TOKEN_RP);
					nextToken();
					try {
						return (Expression) ctor.newInstance(new Object[] {args});
					} catch (InvocationTargetException e) {
						Throwable cause = e.getCause();
						if (cause instanceof RuntimeException)
							throw (RuntimeException) cause;
						throw new RuntimeException("Internal bogus", cause); //$NON-NLS-1$
					} catch (Exception e) {
						// This should never happen.
						throw new RuntimeException("Internal bogus", e); //$NON-NLS-1$
					}
				}
				tokenPos = savePos;
				currentToken = saveToken;
				tokenValue = saveTokenValue;
			}
		}
		return parseUnary();
	}

	private Expression parseUnary() {
		Expression expr;
		switch (currentToken) {
			case TOKEN_LP :
				nextToken();
				expr = parseCondition();
				assertToken(TOKEN_RP);
				nextToken();
				break;
			case TOKEN_LB :
				nextToken();
				expr = new Array(parseArray());
				assertToken(TOKEN_RB);
				nextToken();
				break;
			case TOKEN_LITERAL :
				expr = new Constant(tokenValue);
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
				expr = new EachVariable(EachVariable.KEYWORD_EACH);
				nextToken();
				break;
			case TOKEN_NULL :
				expr = Constant.NULL_CONSTANT;
				nextToken();
				break;
			case TOKEN_TRUE :
				expr = Constant.TRUE_CONSTANT;
				nextToken();
				break;
			case TOKEN_FALSE :
				expr = Constant.FALSE_CONSTANT;
				nextToken();
				break;
			default :
				throw syntaxError();
		}
		return expr;
	}

	private Expression parseParameter() {
		if (currentToken == TOKEN_DOLLAR) {
			nextToken();

			Parameter param = null;
			if (currentToken == TOKEN_LITERAL && tokenValue instanceof Integer)
				param = new IndexedParameter(((Integer) tokenValue).intValue());
			else if (currentToken == TOKEN_IDENTIFIER)
				param = new KeyedParameter((String) tokenValue);

			if (param != null) {
				nextToken();
				return param;
			}
		}
		throw syntaxError();
	}

	private Expression[] parseArray() {
		Expression expr = parseCondition();
		if (currentToken != TOKEN_COMMA)
			return new Expression[] {expr};

		ArrayList operands = new ArrayList();
		operands.add(expr);
		do {
			nextToken();
			if (currentToken == TOKEN_LC)
				// We don't allow lambdas in the array
				break;
			operands.add(parseCondition());
		} while (currentToken == TOKEN_COMMA);
		return (Expression[]) operands.toArray(new Expression[operands.size()]);
	}

	private void assertToken(int token) {
		if (currentToken != token)
			throw syntaxError();
	}

	private Expression getVariableOrRootMember(String id) {
		int idx = size();
		while (--idx >= 0) {
			Variable v = (Variable) get(idx);
			if (id.equals(v.getName()))
				return v;
		}

		if (rootVariable.equals(id))
			throw syntaxError("No such variable: " + id); //$NON-NLS-1$

		return new Member(getVariableOrRootMember(rootVariable), id);
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
					tokenValue = Or.OPERATOR;
					currentToken = TOKEN_OR;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_PIPE;
					++tokenPos;
				}
				break;

			case '&' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '&') {
					tokenValue = And.OPERATOR;
					currentToken = TOKEN_AND;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '=' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = Equals.EQUALS_OPERATOR;
					currentToken = TOKEN_EQUAL;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '!' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = Equals.NOT_EQUALS_OPERATOR;
					currentToken = TOKEN_NOT_EQUAL;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_NOT;
					++tokenPos;
				}
				break;

			case '~' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = Matches.OPERATOR;
					currentToken = TOKEN_MATCHES;
					tokenPos += 2;
				} else
					currentToken = TOKEN_ERROR;
				break;

			case '>' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = Compare.GT_EQUAL_OPERATOR;
					currentToken = TOKEN_GREATER_EQUAL;
					tokenPos += 2;
				} else {
					currentToken = TOKEN_GREATER;
					++tokenPos;
				}
				break;

			case '<' :
				if (tokenPos + 1 < top && expression.charAt(tokenPos + 1) == '=') {
					tokenValue = Compare.LT_EQUAL_OPERATOR;
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
					if (word.equals(EachVariable.KEYWORD_EACH))
						currentToken = TOKEN_ANY;
					else if (word.equals(Latest.OPERATOR))
						currentToken = TOKEN_LATEST;
					else if (word.equals(Limit.OPERATOR))
						currentToken = TOKEN_LIMIT;
					else if (word.equals(Unique.OPERATOR))
						currentToken = TOKEN_UNIQUE;
					else if (word.equals(Select.OPERATOR))
						currentToken = TOKEN_SELECT;
					else if (word.equals(Exists.OPERATOR))
						currentToken = TOKEN_EXISTS;
					else if (word.equals(First.OPERATOR))
						currentToken = TOKEN_FIRST;
					else if (word.equals(Flatten.OPERATOR))
						currentToken = TOKEN_FLATTEN;
					else if (word.equals(All.OPERATOR))
						currentToken = TOKEN_ALL;
					else if (word.equals(Reject.OPERATOR))
						currentToken = TOKEN_REJECT;
					else if (word.equals(Collect.OPERATOR))
						currentToken = TOKEN_COLLECT;
					else if (word.equals(Traverse.OPERATOR))
						currentToken = TOKEN_TRAVERSE;
					else if (word.equals(Constant.NULL_KEYWORD))
						currentToken = TOKEN_NULL;
					else if (word.equals(Constant.FALSE_KEYWORD))
						currentToken = TOKEN_FALSE;
					else if (word.equals(Constant.TRUE_KEYWORD))
						currentToken = TOKEN_TRUE;
					else
						currentToken = TOKEN_IDENTIFIER;
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

	private QueryParseException syntaxError() {
		Object tv = tokenValue;
		if (tv == null) {
			if (lastTokenPos >= expression.length())
				return syntaxError("Unexpeced end of expression"); //$NON-NLS-1$
			tv = expression.substring(lastTokenPos, lastTokenPos + 1);
		}
		return syntaxError("Unexpected token \"" + tv + '"'); //$NON-NLS-1$
	}

	private QueryParseException syntaxError(String message) {
		return new QueryParseException(expression, message, tokenPos);
	}
}
