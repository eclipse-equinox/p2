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
package org.eclipse.equinox.p2.ql;

import java.util.Map;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * This inteface provides all the factory methods needed to create the all possible
 * nodes of the expression tree.
 */
public interface IExpressionFactory {
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
	IExpression and(IExpression[] operands);

	/**
	 * Create an array of elements.
	 * @param elements The elements of the array
	 * @return An array expression
	 */
	IExpression array(IExpression[] elements);

	/**
	 * Creates an expression that represents a variable assignment
	 * @param variable The variable
	 * @param expression The expression that yields the value to assign to the variable
	 * @return An assignment expression
	 */
	IExpression assignment(IExpression variable, IExpression expression);

	/**
	 * Create an lookup of <code>key</code> in the <code>target</code>.
	 * The key expression should evaluate to a string or an integer.
	 * @param target The target for the lookup
	 * @param key The key to use for the lookup
	 * @return A lookup expression
	 */
	IExpression at(IExpression target, IExpression key);

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
	 * Creates an expression that evaluates to the constant <code>value</code>.
	 * @param value The constant
	 * @return A constant expression
	 */
	IExpression constant(Object value);

	/**
	 * Creates a top level expression that represents a full query.
	 * @param expr The query
	 * @return A top level query expression
	 */
	<T> IContextExpression<T> contextExpression(Class<T> elementClass, IExpression expr);

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
	 * Given one of the value in the {@link Map} returned by {@link #getFunctionMap()}, this method
	 * returns a function expression.
	 * @param function The value obtained from the map.
	 * @param args The arguments to evaluate and pass when evaluating the function.
	 * @return A function expression
	 */
	IExpression function(Object function, IExpression[] args);

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
	 * Creates an indexed parameter expression
	 * @param index The index to use
	 * @return a parameter expression
	 */
	IExpression indexedParameter(int index);

	/**
	 * Creates an keyed parameter expression
	 * @param key The key to use
	 * @return a parameter expression
	 */
	IExpression keyedParameter(String key);

	/**
	 * Creates a lambda expression that takes exactly one variable. Suitable for use
	 * in most collection expressions.
	 * @param body The body of the lambda
	 * @param variable The element variable that the lambda uses
	 * @return A lambda expression
	 */
	IExpression lambda(IExpression body, IExpression variable);

	/**
	 * Creates a lambda expression that takes more then one variable (currying). Suitable for use
	 * in most collection expressions.
	 * @param body The body of the lambda
	 * @param variable The element variable that the lambda uses
	 * @param initialAssignments Assignments to evaluate once before calling the body for each element.
	 * @return A lambda expression with currying
	 */
	IExpression lambda(IExpression body, IExpression variable, IExpression[] initialAssignments);

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
	 * Create an expression that yields a new collection consisting of the <i>n</i> first
	 * elements of the source collection where <i>n</i> is determined by <code>limit</code>.
	 * @param collection The source collection
	 * @return A collection expression
	 */
	IExpression limit(IExpression collection, IExpression limit);

	/**
	 * Create an expression that tests if <code>lhs</code> matches <code>rhs</code>.
	 * @param lhs The left hand side value.
	 * @param rhs The right hand side value.
	 * @return A boolean expression
	 */
	IExpression matches(IExpression lhs, IExpression rhs);

	/**
	 * Creates a top level expression suitable for predicate matching
	 * @param expr The boolean expression
	 * @return A top level predicate expression
	 */
	IMatchExpression matchExpression(IExpression expr);

	/**
	 * Creates a member accessor expression.
	 * @param target The target for the member access
	 * @param name The name of the member
	 * @return A member expression
	 */
	IExpression member(IExpression target, String name);

	/**
	 * Creates a member call expression.
	 * @param target The target for the member call
	 * @param name The name of the member
	 * @param args The arguments to use for the call
	 * @return A member expression
	 */
	IExpression memberCall(IExpression target, String name, IExpression[] args);

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
	IExpression or(IExpression[] operands);

	/**
	 * Create an expression that yields a new collection consisting of all elements of the
	 * <code>collection</code> for which the <code>lambda</code> yields <code>true</code>.
	 * @param collection The collection providing the elements to test
	 * @param lambda The lambda that performs the test
	 * @return A collection expression
	 */
	IExpression select(IExpression collection, IExpression lambda);

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
	 * Creates an expression that represents a variable
	 * @param name The name of the variable
	 * @return A variable expression
	 */
	IExpression variable(String name);
}
