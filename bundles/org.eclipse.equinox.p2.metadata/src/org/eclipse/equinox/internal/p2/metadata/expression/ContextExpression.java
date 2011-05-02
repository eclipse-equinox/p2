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

	public boolean accept(IExpressionVisitor visitor) {
		return super.accept(visitor) && operand.accept(visitor);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		operand.toString(bld, rootVariable);
	}

	public IEvaluationContext createContext(Class<? extends T> elementClass, IIndexProvider<T> indexProvider) {
		Variable everything = ExpressionFactory.EVERYTHING;
		IEvaluationContext context = EvaluationContext.create(parameters, everything);
		context.setValue(everything, new Everything<T>(elementClass, indexProvider));
		context.setIndexProvider(indexProvider);
		return context;
	}

	public IEvaluationContext createContext(Class<? extends T> elementClass, Iterator<T> iterator) {
		Variable everything = ExpressionFactory.EVERYTHING;
		IEvaluationContext context = EvaluationContext.create(parameters, everything);
		context.setValue(everything, new Everything<T>(elementClass, iterator, operand));
		return context;
	}

	public Object evaluate(IEvaluationContext context) {
		return operand.evaluate(parameters.length == 0 ? context : EvaluationContext.create(context, parameters));
	}

	public int getExpressionType() {
		return 0;
	}

	public String getOperator() {
		throw new UnsupportedOperationException();
	}

	public int getPriority() {
		return operand.getPriority();
	}

	public Object[] getParameters() {
		return parameters;
	}

	public int hashCode() {
		return operand.hashCode();
	}

	@SuppressWarnings("unchecked")
	public Iterator<T> iterator(IEvaluationContext context) {
		return (Iterator<T>) operand.evaluateAsIterator(context);
	}

	public void toString(StringBuffer bld) {
		toString(bld, ExpressionFactory.EVERYTHING);
	}
}
