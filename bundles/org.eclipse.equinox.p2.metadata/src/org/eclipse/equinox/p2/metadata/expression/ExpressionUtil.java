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

import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.internal.p2.metadata.expression.parser.LDAPFilterParser;
import org.eclipse.equinox.internal.p2.metadata.expression.parser.QLParser;

/**
 * Global access to factory, parser, and methods for introspection
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.0
 */
public final class ExpressionUtil {
	private static final IExpressionParser expressionParser = new QLParser(ExpressionFactory.INSTANCE);
	private static final LDAPFilterParser ldapFilterParser = new LDAPFilterParser(ExpressionFactory.INSTANCE);
	public static final IExpression TRUE_EXPRESSION = ExpressionFactory.INSTANCE.constant(Boolean.TRUE);
	public static final IExpression FALSE_EXPRESSION = ExpressionFactory.INSTANCE.constant(Boolean.FALSE);

	private ExpressionUtil() {
		//We don't want to ppl to instantiate this class
	}

	/**
	 * Returns the global expression factory
	 * @return The global expression factory.
	 */
	public static IExpressionFactory getFactory() {
		return ExpressionFactory.INSTANCE;
	}

	/**
	 * Creates and returns a new expression parser
	 * @return The new parser
	 */
	public static IExpressionParser getParser() {
		return expressionParser;
	}

	/**
	 * Parse an LDAP filter from the <code>filter</code> string. If <code>filter</code> is <code>null</code>
	 * or a string that is empty or only consists of whitespace, then this method returns <code>null</code>.
	 * @param filter The filter to parse. Can be <code>null</code> or empty.
	 * @return An expression that corresponds to the LDAP filter or <code>null</code>.
	 * @throws ExpressionParseException If the syntax was invalid
	 */
	public static IFilterExpression parseLDAP(String filter) throws IllegalArgumentException {
		filter = trimmedOrNull(filter);
		return filter == null ? null : ldapFilterParser.parse(filter);
	}

	/**
	 * Create a new expression. The expression will have access to the global
	 * variable 'this' and to the context parameters.
	 * @param expression The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws ExpressionParseException If the syntax was invalid
	 */
	public static IExpression parse(String expression) {
		expression = trimmedOrNull(expression);
		return expression == null ? null : getParser().parse(expression);
	}

	/**
	 * Create an arbitrary expression. The expression will have access to the global
	 * variable 'everything' and to the context parameters.
	 * @param expression The string representing the boolean expression.
	 * @return The resulting expression tree.
	 * @throws ExpressionParseException If the syntax was invalid
	 */
	public static IExpression parseQuery(String expression) {
		expression = trimmedOrNull(expression);
		return expression == null ? null : getParser().parseQuery(expression);
	}

	/**
	 * If <code>str</code> is <code>null</code>, then this method returns <code>null</code>.
	 * Otherwise <code>str</code> is trimmed from whitespace at both ends. If the result
	 * of the trim is an empty string, then <code>null</code> is returned, otherwise the
	 * result of the trim is returned.
	 * @param str The string to trim. Can be <code>null</code>.
	 * @return The trimmed string or <code>null</code>.
	 */
	public static String trimmedOrNull(String str) {
		if (str != null) {
			str = str.trim();
			if (str.length() == 0)
				str = null;
		}
		return str;
	}

	/**
	 * Obtains the Left Hand Side (LHS) of a binary expression.
	 * @param expression The expression to introspect
	 * @return The left hand side operator
	 * @throws IllegalArgumentException if the expression is not a binary expression
	 * @see IExpression#TYPE_AT
	 * @see IExpression#TYPE_EQUALS
	 * @see IExpression#TYPE_GREATER
	 * @see IExpression#TYPE_GREATER_EQUAL
	 * @see IExpression#TYPE_LESS
	 * @see IExpression#TYPE_LESS_EQUAL
	 * @see IExpression#TYPE_MATCHES
	 * @see IExpression#TYPE_NOT_EQUALS
	 */
	public static IExpression getLHS(IExpression expression) {
		if (expression instanceof Binary)
			return ((Binary) expression).lhs;
		throw new IllegalArgumentException();
	}

	/**
	 * Obtains the name of a variable or member expression.
	 * @param expression The expression to introspect
	 * @return The name of the expression
	 * @throws IllegalArgumentException if the expression is not a variable or a member
	 * @see IExpression#TYPE_MEMBER
	 * @see IExpression#TYPE_VARIABLE
	 */
	public static String getName(IExpression expression) {
		if (expression instanceof Member)
			return ((Member) expression).getName();
		if (expression instanceof Variable)
			return ((Variable) expression).getName();
		throw new IllegalArgumentException();
	}

	/**
	 * Obtains the operand of an unary expression
	 * @param expression The expression to introspect
	 * @return The expression operand
	 * @throws IllegalArgumentException if the expression is not an unary expression
	 * @see IExpression#TYPE_ALL
	 * @see IExpression#TYPE_EXISTS
	 * @see IExpression#TYPE_LAMBDA
	 * @see IExpression#TYPE_NOT
	 */
	public static IExpression getOperand(IExpression expression) {
		if (expression instanceof Unary)
			return ((Unary) expression).operand;
		throw new IllegalArgumentException();
	}

	/**
	 * Obtains the operands of an n-ary expression
	 * @param expression The expression to introspect
	 * @return The expression operand
	 * @throws IllegalArgumentException if the expression is not a n-ary expression
	 * @see IExpression#TYPE_AND
	 * @see IExpression#TYPE_OR
	 */
	public static IExpression[] getOperands(IExpression expression) {
		if (expression instanceof NAry)
			return ((NAry) expression).operands;
		throw new IllegalArgumentException();
	}

	/**
	 * Obtains the Right Hand Side (RHS) of a binary expression.
	 * @param expression The expression to introspect
	 * @return The right hand side operator
	 * @throws IllegalArgumentException if the expression is not a binary expression
	 * @see IExpression#TYPE_AT
	 * @see IExpression#TYPE_EQUALS
	 * @see IExpression#TYPE_GREATER
	 * @see IExpression#TYPE_GREATER_EQUAL
	 * @see IExpression#TYPE_LESS
	 * @see IExpression#TYPE_LESS_EQUAL
	 * @see IExpression#TYPE_MATCHES
	 * @see IExpression#TYPE_NOT_EQUALS
	 */
	public static IExpression getRHS(IExpression expression) {
		if (expression instanceof Binary)
			return ((Binary) expression).rhs;
		throw new IllegalArgumentException();
	}

	/**
	 * Obtains the value of a literal expression
	 * @param expression The expression to introspect
	 * @return The literal value
	 * @throws IllegalArgumentException if the expression is not a literal
	 * @see IExpression#TYPE_LITERAL
	 */
	public static Object getValue(IExpression expression) {
		if (expression instanceof Literal)
			return ((Literal) expression).value;
		throw new IllegalArgumentException();
	}
}
