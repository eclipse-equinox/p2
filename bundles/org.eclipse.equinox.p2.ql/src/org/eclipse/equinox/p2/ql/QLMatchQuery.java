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

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IMatchQuery;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;

/**
 * An IQuery implementation that is based on the p2 query language.
 */
public class QLMatchQuery extends QLQuery implements IMatchQuery {
	private final IMatchExpression expression;
	private IEvaluationContext context;

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param elementClass The class used for filtering elements before calling {@link #isMatch(Object)} 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLMatchQuery(Class instanceClass, IMatchExpression expression, Object[] parameters) {
		super(instanceClass, parameters);
		this.expression = expression;
	}

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param elementClass The class used for filtering elements before calling {@link #isMatch(Object)} 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLMatchQuery(Class instanceClass, String expression, Object[] parameters) {
		this(instanceClass, parser.parsePredicate(expression), parameters);
	}

	/**
	 * Convenience method that creates a new query instance without parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param expression The expression string that represents the query.
	 */
	public QLMatchQuery(String expression) {
		this(IVersionedId.class, expression, (Object[]) null);
	}

	/**
	 * Convenience method that creates a new query instance with one parameter.
	 * The element class defaults to {@link IVersionedId}.
	 * @param expression The expression string that represents the query.
	 * @param param The first parameter.
	 */
	public QLMatchQuery(String expression, Object param1) {
		this(IVersionedId.class, expression, new Object[] {param1});
	}

	/**
	 * Convenience method that creates a new query instance with two parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param expression The expression string that represents the query.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 */
	public QLMatchQuery(String expression, Object param1, Object param2) {
		this(IVersionedId.class, expression, new Object[] {param1, param2});
	}

	/**
	 * Convenience method that creates a new query instance with three parameters.
	 * The element class defaults to {@link IVersionedId}.
	 * @param expression The expression string that represents the query.
	 * @param param1 The first parameter.
	 * @param param2 The second parameter.
	 * @param param2 The third parameter.
	 */
	public QLMatchQuery(String expression, Object param1, Object param2, Object param3) {
		this(IVersionedId.class, parser.parsePredicate(expression), new Object[] {param1, param2, param3});
	}

	/**
	 * Checks if the <code>candidate</code> object is an instance of the <code>elementClass</code>
	 * used by this query. If it is, the result calling {@link IMatchExpression#isMatch(IEvaluationContext, Object)}
	 * on the contained expression is returned.
	 * @param candidate The object to test
	 * @return <code>true</code> if <code>candidate</code> is an instance of the element class and the
	 * expression match test returns true.
	 */
	public boolean isMatch(Object candidate) {
		return elementClass.isInstance(candidate) && expression.isMatch(context, candidate);
	}

	public void postPerform() {
		context = null;
	}

	public void prePerform() {
		//
	}

	public IQueryResult perform(Iterator iterator) {
		if (expression.needsTranslations()) {
			IQueryContext queryContext = QL.newQueryContext(iterator);
			context = expression.createContext(parameters, queryContext.getTranslationSupport(getLocale()));
		} else
			context = expression.createContext(parameters);

		prePerform();
		try {
			ArrayList result = new ArrayList();
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (isMatch(candidate))
					result.add(candidate);
			}
			return new QueryResult(result);
		} finally {
			postPerform();
		}
	}
}
