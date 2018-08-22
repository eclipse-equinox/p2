/*******************************************************************************
 * Copyright (c) 2009, 2017 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * The context expression is the top expression in context queries. It introduces the
 * variable 'everything' and initialized it with the iterator that represents all
 * available items.
 */
public class ContextExpression<T> extends Unary implements IContextExpression<T> {
	private static final Object[] noParams = new Object[0];
	protected final Object[] parameters;

	public ContextExpression(Expression expression, Object[] parameters) {
		super(expression);
		this.parameters = parameters == null ? noParams : parameters;
	}

	@Override
	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	@Override
	public void toString(StringBuffer bld, Variable rootVariable) {
		operand.toString(bld, rootVariable);
	}

	@Override
	public IEvaluationContext createContext(Class<? extends T> elementClass, IIndexProvider<T> indexProvider) {
		Variable everything = ExpressionFactory.EVERYTHING;
		IEvaluationContext context = EvaluationContext.create(parameters, everything);
		context.setValue(everything, new Everything<>(elementClass, indexProvider));
		context.setIndexProvider(indexProvider);
		return context;
	}

	@Override
	public IEvaluationContext createContext(Class<? extends T> elementClass, Iterator<T> iterator) {
		Variable everything = ExpressionFactory.EVERYTHING;
		IEvaluationContext context = EvaluationContext.create(parameters, everything);
		context.setValue(everything, new Everything<>(elementClass, iterator, operand));
		return context;
	}

	@Override
	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(parameters.length == 0 ? context : EvaluationContext.create(context, parameters));
	}

	@Override
	public int getExpressionType() {
		return 0;
	}

	@Override
	public String getOperator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPriority() {
		return operand.getPriority();
	}

	@Override
	public Object[] getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		return operand.hashCode();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<T> iterator(IEvaluationContext context) {
		return (Iterator<T>) operand.evaluateAsIterator(context);
	}

	@Override
	public void toString(StringBuffer bld) {
		toString(bld, ExpressionFactory.EVERYTHING);
	}
}
