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
package org.eclipse.equinox.internal.p2.ql.expression;

import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.expression.*;
import org.eclipse.equinox.internal.p2.ql.Everything;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.ql.IContextExpression;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

/**
 * The context expression is the top expression in context queries. It introduces the
 * variable 'everything' and initialized it with the iterator that represents all
 * available items.
 */
public final class ContextExpression<T> extends Unary implements IContextExpression<T>, IQLConstants {
	private static final Object[] noParams = new Object[0];
	private final Class<T> elementClass;
	private final Object[] parameters;

	public ContextExpression(Class<T> elementClass, Expression expression, Object[] parameters) {
		super(expression);
		this.elementClass = elementClass;
		this.parameters = parameters == null ? noParams : parameters;
	}

	public boolean accept(IExpressionVisitor visitor) {
		return visitor.visit(operand);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		operand.toString(bld, rootVariable);
	}

	public IEvaluationContext createContext(Iterator<T> iterator) {
		Variable everything = QLFactory.EVERYTHING;
		IEvaluationContext context = EvaluationContext.create(parameters, everything);
		context.setValue(everything, new Everything<T>(elementClass, iterator, QLUtil.needsRepeadedAccessToEverything(operand)));
		return context;
	}

	public IEvaluationContext createContext(Iterator<T> iterator, ITranslationSupport ts) {
		Variable everything = QLFactory.EVERYTHING;
		IExpression translations = QLFactory.TRANSLATIONS;
		IEvaluationContext context = EvaluationContext.create(parameters, new IExpression[] {everything, translations});
		context.setValue(everything, new Everything<T>(elementClass, iterator, QLUtil.needsRepeadedAccessToEverything(operand)));
		context.setValue(translations, ts);
		return context;
	}

	public Class<T> getElementClass() {
		return elementClass;
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
		return (Iterator<T>) evaluateAsIterator(context);
	}

	public void toString(StringBuffer bld) {
		toString(bld, QLFactory.EVERYTHING);
	}
}
