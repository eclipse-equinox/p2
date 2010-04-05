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
import java.util.Map;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.IQuery;

/**
 * This interface provides all the factory methods needed to create the
 * nodes of the expression tree.
 * @since 2.0
 * @noimplement This interface is not intended to be implemented directly by clients.
 * @noextend This interface is not intended to be extended directly by clients.
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
	 * Creates an expression that represents a variable assignment
	 * @param variable The variable
	 * @param expression The expression that yields the value to assign to the variable
	 * @return An assignment expression
	 */
	IExpression assignment(IExpression variable, IExpression expression);

	/**
	 * Create an expression that collects the result of evaluating each element in a new collection.
	 * @param collection The collection providing the elements to evaluate
	 * @param lambda The lambda that creates each new element
	 * @return A collection expression
	 */
	IExpression collect(IExpression collection, IExpression lambda);

	/**
	 * Create an expression that first evaluates a <code>test</code> and then, depending on the outcome,
	 * evaluates either <code>ifTrue</code> or <code>ifFalse</code>. The expression yields the result
	 * of the <code>ifTrue</code> or <code>ifFalse</code> evaluation.
	 * @param test The test
	 * @param ifTrue The code to evaluate when the test evaluates to <code>true</code>
	 * @param ifFalse The code to evaluate when the test evaluates to <code>false</code>
	 * @return The conditional expression
	 */
	IExpression condition(IExpression test, IExpression ifTrue, IExpression ifFalse);

	/**
	 * Create an expression that yields the first element of the
	 * <code>collection</code> for which the <code>lambda</code> yields <code>true</code>.
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that performs the test
	 * @return An element expression
	 */
	IExpression first(IExpression collection, IExpression lambda);

	/**
	 * Intended to be applied on collections of collections. Yields a single collection with
	 * all elements from the source collections, in the order they are evaluated.
	 * @param collection The collection providing the collections that provides all elements
	 * @return A collection expression
	 */
	IExpression flatten(IExpression collection);

	/**
	 * Creates a lambda expression that takes more then one variable (currying). Suitable for use
	 * in most collection expressions.
	 * @param variable The element variable that the lambda uses
	 * @param body The body of the lambda
	 * @param initialAssignments Assignments to evaluate once before calling the body for each element.
	 * @return A lambda expression with currying
	 */
	IExpression lambda(IExpression variable, IExpression[] initialAssignments, IExpression body);

	/**
	 * Creates a member call expression.
	 * @param target The target for the member call
	 * @param name The name of the member
	 * @param args The arguments to use for the call
	 * @return A member expression
	 */
	IExpression memberCall(IExpression target, String name, IExpression... args);

	/**
	 * <p>Recursively traverse and collect elements based on a condition</p>
	 * <p>A common scenario in p2 is that you want to start with a set of roots and then find
	 * all items that fulfill the root requirements. Those items in turn introduce new
	 * requirements so you want to find them too. The process continues until no more
	 * requirements can be satisfied. This type of query can be performed using the traverse
	 * function.</p>
	 * <p>The function will evaluate an expression, once for each element, collect
	 * elements for which the evaluation returned true, then then re-evaluate using the
	 * collected result as source of elements. No element is evaluated twice. This continues
	 * until no more elements are found.</p>
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that collects the children for the next iteration
	 * @return A collection expression
	 */
	IExpression traverse(IExpression collection, IExpression lambda);

	/**
	 * Create an expression that yields a new collection where each element is unique. An
	 * optional <code>cache</code> can be provided if the uniqueness should span a larger
	 * scope then just the source collection.
	 * @param collection The source collection
	 * @param cache Optional cache to use when uniqueness should span over several invocations
	 * @return A collection expression
	 */
	IExpression unique(IExpression collection, IExpression cache);

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
	 * Given one of the values in the map returned by {@link #getFunctionMap()}, this method
	 * returns a function expression.
	 * @param function The value obtained from the map.
	 * @param args The arguments to evaluate and pass when evaluating the function.
	 * @return A function expression
	 */
	IExpression function(Object function, IExpression... args);

	/**
	 * Returns a map of functions supported by this factory. The map is keyed by
	 * function names and the value is an object suitable to pass to the {@link #function(Object, IExpression[])}
	 * method.
	 * @return A key/function map.
	 */
	Map<String, ? extends Object> getFunctionMap();

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
