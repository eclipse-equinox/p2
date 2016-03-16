/*******************************************************************************
 * Copyright (c) 2009, 2016 Cloudsmith Inc. and others.
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
import org.eclipse.equinox.internal.p2.metadata.expression.Variable;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.expression.IExpressionFactory;

public class QLParser extends ExpressionParser {
	private static final long serialVersionUID = 882034383978853143L;

	private static final int TOKEN_ANY = 42;

	private static final int TOKEN_LATEST = 70;
	private static final int TOKEN_LIMIT = 71;
	private static final int TOKEN_FIRST = 72;
	private static final int TOKEN_FLATTEN = 73;
	private static final int TOKEN_UNIQUE = 74;
	private static final int TOKEN_SELECT = 75;
	private static final int TOKEN_COLLECT = 76;
	private static final int TOKEN_TRAVERSE = 77;
	private static final int TOKEN_INTERSECT = 78;
	private static final int TOKEN_UNION = 79;

	private static final Map<String, Integer> qlKeywords;
	static {
		qlKeywords = new HashMap<String, Integer>();
		qlKeywords.putAll(keywords);
		qlKeywords.put(KEYWORD_COLLECT, Integer.valueOf(TOKEN_COLLECT));
		qlKeywords.put(KEYWORD_FALSE, Integer.valueOf(TOKEN_FALSE));
		qlKeywords.put(KEYWORD_FIRST, Integer.valueOf(TOKEN_FIRST));
		qlKeywords.put(KEYWORD_FLATTEN, Integer.valueOf(TOKEN_FLATTEN));
		qlKeywords.put(KEYWORD_LATEST, Integer.valueOf(TOKEN_LATEST));
		qlKeywords.put(KEYWORD_LIMIT, Integer.valueOf(TOKEN_LIMIT));
		qlKeywords.put(KEYWORD_NULL, Integer.valueOf(TOKEN_NULL));
		qlKeywords.put(KEYWORD_SELECT, Integer.valueOf(TOKEN_SELECT));
		qlKeywords.put(KEYWORD_TRAVERSE, Integer.valueOf(TOKEN_TRAVERSE));
		qlKeywords.put(KEYWORD_TRUE, Integer.valueOf(TOKEN_TRUE));
		qlKeywords.put(KEYWORD_UNIQUE, Integer.valueOf(TOKEN_UNIQUE));
		qlKeywords.put(KEYWORD_INTERSECT, Integer.valueOf(TOKEN_INTERSECT));
		qlKeywords.put(KEYWORD_UNION, Integer.valueOf(TOKEN_UNION));
		qlKeywords.put(OPERATOR_EACH, Integer.valueOf(TOKEN_ANY));
	}

	public QLParser(IExpressionFactory factory) {
		super(factory);
	}

	protected Map<String, Integer> keywordToTokenMap() {
		return qlKeywords;
	}

	protected IExpression parseCondition() {
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

	protected IExpression parseMember() {
		IExpression expr = parseFunction();
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
						if (currentToken == TOKEN_LP) {
							nextToken();
							IExpression[] callArgs = parseArray();
							assertToken(TOKEN_RP);
							nextToken();
							expr = factory.memberCall(expr, name, callArgs);
						} else
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

	protected IExpression parseFunction() {
		if (currentToken == TOKEN_IDENTIFIER) {
			Object function = factory.getFunctionMap().get(tokenValue);
			if (function != null) {
				int savePos = tokenPos;
				int saveToken = currentToken;
				Object saveTokenValue = tokenValue;

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

	protected IExpression parseCollectionLHS() {
		IExpression expr;
		switch (currentToken) {
			case TOKEN_SELECT :
			case TOKEN_COLLECT :
			case TOKEN_FIRST :
			case TOKEN_FLATTEN :
			case TOKEN_TRAVERSE :
			case TOKEN_LATEST :
			case TOKEN_LIMIT :
			case TOKEN_INTERSECT :
			case TOKEN_UNION :
			case TOKEN_UNIQUE :
				expr = getVariableOrRootMember(rootVariable);
				break;
			default :
				expr = super.parseCollectionLHS();
		}
		return expr;
	}

	protected IExpression parseCollectionRHS(IExpression expr, int funcToken) {
		switch (funcToken) {
			case TOKEN_SELECT :
				expr = factory.select(expr, parseLambdaDefinition());
				break;
			case TOKEN_COLLECT :
				expr = factory.collect(expr, parseLambdaDefinition());
				break;
			case TOKEN_FIRST :
				expr = factory.first(expr, parseLambdaDefinition());
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
			case TOKEN_INTERSECT :
				expr = factory.intersect(expr, parseCondition());
				assertToken(TOKEN_RP);
				nextToken();
				break;
			case TOKEN_UNION :
				expr = factory.union(expr, parseCondition());
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
				expr = super.parseCollectionRHS(expr, funcToken);
		}
		return expr;
	}

	protected IExpression parseUnary() {
		IExpression expr;
		switch (currentToken) {
			case TOKEN_LB :
				nextToken();
				expr = factory.array(parseArray());
				assertToken(TOKEN_RB);
				nextToken();
				break;
			case TOKEN_ANY :
				expr = factory.variable(OPERATOR_EACH);
				nextToken();
				break;
			default :
				expr = super.parseUnary();
		}
		return expr;
	}

	protected IExpression parseLambdaDefinition() {
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
					if (initializer instanceof Variable && OPERATOR_EACH.equals(initializer.toString())) {
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
		return factory.lambda(each, assignments, body);
	}

	private IExpression[] parseVariables() {
		int savePos = tokenPos;
		int saveToken = currentToken;
		Object saveTokenValue = tokenValue;
		List<Object> ids = null;
		while (currentToken == TOKEN_IDENTIFIER) {
			if (ids == null)
				ids = new ArrayList<Object>();
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
}
