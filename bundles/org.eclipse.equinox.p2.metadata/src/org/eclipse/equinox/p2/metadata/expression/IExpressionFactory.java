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

import java.util.List;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.IQuery;

/**
 * This interface provides all the factory methods needed to create the
 * nodes of the expression tree.
 * @since 2.0
 */
public interface IExpressionFactory {
	String FUNC_BOOLEAN = "boolean"; //$NON-NLS-1$
	String FUNC_VERSION = "version"; //$NON-NLS-1$
	String FUNC_CLASS = "class"; //$NON-NLS-1$
	String FUNC_RANGE = "range"; //$NON-NLS-1$
	String FUNC_FILTER = "filter"; //$NON-NLS-1$

	IExpression[] NO_ARGS = new IExpression[0];

	/**
	 * Create a collection filter that yields true if the <code>lambda</code> yields true for
	 * all of the elements of the <code>collection</code>
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that performs the test
	 * @return A boolean expression
	 */
	IExpression all(IExpression collection, IExpression lambda);

	/**
	 * Create a logical <i>and</i> of its <code>operands</code>.
	 * @param operands The boolean operands
	 * @return A boolean expression
	 */
	IExpression and(IExpression... operands);

	/**
	 * Create an array of elements.
	 * @param elements The elements of the array
	 * @return An array expression
	 */
	IExpression array(IExpression... elements);

	/**
	 * Create an lookup of <code>key</code> in the <code>target</code>.
	 * The key expression should evaluate to a string or an integer.
	 * @param target The target for the lookup
	 * @param key The key to use for the lookup
	 * @return A lookup expression
	 */
	IExpression at(IExpression target, IExpression key);

	/**
	 * Create an evaluation context with one single variable
	 * @param params Indexed parameters to use in the expression
	 * @return the context
	 */
	IEvaluationContext createContext(Object... params);

	/**
	 * Create an evaluation context with one single variable
	 * @param params Indexed parameters to use in the expression
	 * @param variables The variables that will be maintained by the context
	 * @return the context
	 */
	IEvaluationContext createContext(IExpression[] variables, Object... params);

	/**
	 * Creates an expression that evaluates to the constant <code>value</code>.
	 * @param value The constant
	 * @return A constant expression
	 */
	IExpression constant(Object value);

	/**
	 * Creates a top level expression that represents a full query.
	 * @param expr The query
	 * @param parameters The parameters of the query
	 * @return A top level query expression
	 */
	<T> IContextExpression<T> contextExpression(IExpression expr, Object... parameters);

	/**
	 * Create an expression that tests if <code>lhs</code> is equal to <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression equals(IExpression lhs, IExpression rhs);

	/**
	 * Create a collection filter that yields true if the <code>lambda</code> yields true for
	 * at least one of the elements of the <code>collection</code>
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that performs the test
	 * @return A boolean expression
	 */
	IExpression exists(IExpression collection, IExpression lambda);

	/**
	 * Creates a top level expression suitable for predicate matching
	 * @param expression The boolean expression
	 * @return A top level predicate expression
	 */
	IFilterExpression filterExpression(IExpression expression);

	/**
	 * Create an expression that tests if <code>lhs</code> is greater than <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression greater(IExpression lhs, IExpression rhs);

	/**
	 * Create an expression that tests if <code>lhs</code> is greater than or equal to <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression greaterEqual(IExpression lhs, IExpression rhs);

	/**
	 * Creates an indexed parameter expression
	 * @param index The index to use
	 * @return a parameter expression
	 */
	IExpression indexedParameter(int index);

	/**
	 * Create an <i>intersection</i> of <code>c1</code> and <code>c2</code> 
	 * @param c1 first collection
	 * @param c2 second collection
	 * @return An intersect expression
	 */
	IExpression intersect(IExpression c1, IExpression c2);

	/**
	 * Creates a lambda expression that takes exactly one variable. Suitable for use
	 * in most collection expressions.
	 * @param variable The element variable that the lambda uses
	 * @param body The body of the lambda
	 * @return A lambda expression
	 */
	IExpression lambda(IExpression variable, IExpression body);

	/**
	 * Create an expression that yields a new collection consisting of the latest version of
	 * the elements of the <code>collection</code>. Each element in <code>collection</code>
	 * must implement the {@link IVersionedId} interface.
	 * @param collection The collection providing the versioned elements
	 * @return A collection expression
	 */
	IExpression latest(IExpression collection);

	/**
	 * Create an expression that tests if <code>lhs</code> is less than <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression less(IExpression lhs, IExpression rhs);

	/**
	 * Create an expression that tests if <code>lhs</code> is less than or equal to <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression lessEqual(IExpression lhs, IExpression rhs);

	/**
	 * Create an expression that yields a new collection consisting of the <i>count</i>
	 * first elements of the source collection.
	 * @param collection The source collection
	 * @param count The element count limit
	 * @return A collection expression
	 */
	IExpression limit(IExpression collection, int count);

	/**
	 * Create an expression that yields a new collection consisting of the <i>n</i> first
	 * elements of the source collection where <i>n</i> is determined by <code>limit</code>.
	 * @param collection The source collection
	 * @param limit The expression that evaluates to the element count limit
	 * @return A collection expression
	 */
	IExpression limit(IExpression collection, IExpression limit);

	/**
	 * Performs boolean normalization on the expression to create a canonical form.
	 * @param operands The operands to normalize
	 * @param expressionType The type (must be either {@link IExpression#TYPE_AND}
	 * or {@link IExpression#TYPE_OR}.
	 * @return The normalized expression
	 */
	IExpression normalize(List<? extends IExpression> operands, int expressionType);

	/**
	 * Create an expression that tests if <code>lhs</code> matches <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression matches(IExpression lhs, IExpression rhs);

	/**
	 * Creates a parameterized top level expression suitable for predicate matching
	 * @param expression The boolean expression
	 * @param parameters The parameters to use in the call
	 * @return A top level predicate expression
	 */
	<T> IMatchExpression<T> matchExpression(IExpression expression, Object... parameters);

	/**
	 * Creates a member accessor expression.
	 * @param target The target for the member access
	 * @param name The name of the member
	 * @return A member expression
	 */
	IExpression member(IExpression target, String name);

	/**
	 * Creates an expression that negates the result of evaluating its <code>operand</code>.
	 * @param operand The boolean expression to negate
	 * @return A boolean expression
	 */
	IExpression not(IExpression operand);

	/**
	 * Create a logical <i>or</i> of its <code>operands</code>.
	 * @param operands The boolean operands
	 * @return A boolean expression
	 */
	IExpression or(IExpression... operands);

	/**
	 * Create a pipe of expressions.
	 * @param expressions The expressions that make out the pipe
	 * @return A pipe expression
	 */
	IExpression pipe(IExpression... expressions);

	/**
	 * Create an expression that yields a new collection consisting of all elements of the
	 * <code>collection</code> for which the <code>lambda</code> yields <code>true</code>.
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that performs the test
	 * @return A collection expression
	 */
	IExpression select(IExpression collection, IExpression lambda);

	/**
	 * Returns the variable that represents <code>this</this> in an expression
	 * @return The <code>this</this> variable.
	 */
	IExpression thisVariable();

	/**
	 * Wrap an {@link IQuery} as an expression.
	 * @param query
	 * @return An expression that wraps the query
	 */
	IExpression toExpression(IQuery<?> query);

	/**
	 * Create a <i>union</i> of <code>c1</code> and <code>c2</code> 
	 * @param c1 first collection
	 * @param c2 second collection
	 * @return A union expression
	 */
	IExpression union(IExpression c1, IExpression c2);

	/**
	 * Creates an expression that represents a variable
	 * @param name The name of the variable
	 * @return A variable expression
	 */
	IExpression variable(String name);
}
