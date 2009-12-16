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

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * An IQuery 'context query' implementation that is based on the p2 query language.
 */
public class QLContextQuery extends QLQuery {
	private final IContextExpression expression;

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLContextQuery(Class elementClass, IContextExpression expression, Object[] parameters) {
		super(elementClass, parameters);
		this.expression = expression;
	}

	/**
	 * Creates a new query instance with keyed parameters.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLContextQuery(Class elementClass, String expression, Object[] parameters) {
		this(elementClass, parser.parseQuery(expression), parameters);
	}

	/**
	 * Convenience method that creates a new query instance without parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression string that represents the query.
	 */
	public QLContextQuery(String expression) {
		this(IVersionedId.class, expression, noParameters);
	}

	/**
	 * Convenience method that creates a new query instance with one parameter.
	 * The element class defaults to {@link IVersionedId}.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression string that represents the query.
	 * @param param The first parameter.
	 */
	public QLContextQuery(String expression, Object param) {
		this(IVersionedId.class, expression, new Object[] {param});
	}

	/**
	 * Convenience method that creates a new query instance with two parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression string that represents the query.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 */
	public QLContextQuery(String expression, Object param1, Object param2) {
		this(IVersionedId.class, expression, new Object[] {param1, param2});
	}

	/**
	 * Convenience method that creates a new query instance with three parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param elementClass The class used for filtering elements in 'everything' 
	 * @param expression The expression string that represents the query.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @param param2 The third parameter.
	 */
	public QLContextQuery(String expression, Object param1, Object param2, Object param3) {
		this(IVersionedId.class, expression, new Object[] {param1, param2, param3});
	}

	public Collector perform(Iterator iterator, Collector collector) {
		iterator = evaluate(iterator);
		while (iterator.hasNext()) {
			Object nxt = iterator.next();
			if (!collector.accept(nxt))
				break;
		}
		return collector;
	}

	public Iterator evaluate(Iterator iterator) {
		IEvaluationContext ctx;
		if (expression.needsTranslations()) {
			IQueryContext queryContext = QL.newQueryContext(iterator);
			ctx = expression.createContext(elementClass, iterator, parameters, queryContext.getTranslationSupport(getLocale()));
		} else
			ctx = expression.createContext(elementClass, iterator, parameters);
		return expression.evaluateAsIterator(ctx);
	}

	/**
	 * Query without using a collector. Instead, return the result of the query directly.
	 * @param queryContext The context for the query.
	 * @return The result of the query.
	 */
	public Object query(IQueryContext queryContext) {
		// Check if we need translation support
		//
		IEvaluationContext ctx;
		if (expression.needsTranslations())
			ctx = expression.createContext(elementClass, queryContext.iterator(), parameters, queryContext.getTranslationSupport(getLocale()));
		else
			ctx = expression.createContext(elementClass, queryContext.iterator(), parameters);
		return expression.evaluate(ctx);
	}
}
