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
package org.eclipse.equinox.p2.ql;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.EvaluationContext;
import org.eclipse.equinox.internal.p2.ql.expression.QLFactory;
import org.eclipse.equinox.internal.p2.ql.expression.QLUtil;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.query.IMatchQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/**
 * An IQuery implementation that is based on the p2 query language.
 */
public class QLMatchQuery<T> extends QLQuery<T> implements IMatchQuery<T> {
	private final IMatchExpression<T> expression;
	private IEvaluationContext context;

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param instanceClass The class used for filtering elements before calling {@link #isMatch(Object)} 
	 * @param expression The expression that represents the query.
	 */
	public QLMatchQuery(Class<T> instanceClass, IMatchExpression<T> expression) {
		super(instanceClass);
		this.expression = expression;
	}

	/**
	 * Creates a new query instance with indexed parameters.
	 * @param instanceClass The class used for filtering elements before calling {@link #isMatch(Object)} 
	 * @param expression The expression that represents the query.
	 * @param parameters Parameters to use for the query.
	 */
	public QLMatchQuery(Class<T> instanceClass, String expression, Object... parameters) {
		this(instanceClass, ExpressionUtil.getFactory().<T> matchExpression(ExpressionUtil.getParser().parse(expression), parameters));
	}

	/**
	 * Checks if the <code>candidate</code> object is an instance of the <code>elementClass</code>
	 * used by this query. If it is, the result calling {@link IMatchExpression#isMatch(IEvaluationContext, Object)}
	 * on the contained expression is returned.
	 * @param candidate The object to test
	 * @return <code>true</code> if <code>candidate</code> is an instance of the element class and the
	 * expression match test returns true.
	 */
	public boolean isMatch(T candidate) {
		return elementClass.isInstance(candidate) && expression.isMatch(context, candidate);
	}

	public void postPerform() {
		context = null;
	}

	public void prePerform() {
		//
	}

	public IQueryResult<T> perform(Iterator<T> iterator) {
		if (QLUtil.needsTranslationSupport(expression)) {
			IQueryContext<T> queryContext = QL.newQueryContext(iterator);
			IExpression translations = QLFactory.TRANSLATIONS;
			context = EvaluationContext.create(expression.createContext(), translations);
			context.setValue(translations, queryContext.getTranslationSupport(getLocale()));
		} else
			context = expression.createContext();

		prePerform();
		try {
			ArrayList<T> result = new ArrayList<T>();
			while (iterator.hasNext()) {
				T candidate = iterator.next();
				if (isMatch(candidate))
					result.add(candidate);
			}
			return new QueryResult<T>(result);
		} finally {
			postPerform();
		}
	}
}
