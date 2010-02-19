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
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.ql.IContextExpression;
import org.eclipse.equinox.p2.ql.ITranslationSupport;

/**
 * The context expression is the top expression in context queries. It introduces the
 * variable 'everything' and initialized it with the iterator that represents all
 * available items.
 */
public final class ContextExpression<T> extends org.eclipse.equinox.internal.p2.metadata.expression.ContextExpression<T> implements IContextExpression<T> {
	public ContextExpression(Expression expression, Object[] parameters) {
		super(expression, parameters);
	}

	public IEvaluationContext createContext(Class<T> elementClass, Iterator<T> iterator, ITranslationSupport ts) {
		Variable everything = ExpressionFactory.EVERYTHING;
		IExpression translations = QLFactory.TRANSLATIONS;
		IEvaluationContext context = EvaluationContext.create(parameters, new IExpression[] {everything, translations});
		context.setValue(everything, new Everything<T>(elementClass, iterator, operand));
		context.setValue(translations, ts);
		return context;
	}
}
