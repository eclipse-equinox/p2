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
package org.eclipse.equinox.internal.p2.ql;

import java.util.Iterator;
import java.util.Map;

public final class LocalizedProperty extends Function {

	static final String KEYWORD = "localizedProperty"; //$NON-NLS-1$

	private static ContextExpression localePropertiesExpr = new ExpressionParser().parseQuery("" + // //$NON-NLS-1$
			"localizedMap($0, $1).select(e | localizedKeys($0, $2).exists(k | k == e.key))"); //$NON-NLS-1$

	public LocalizedProperty(Expression[] operands) {
		super(operands);
		if (operands.length != 3)
			throw new IllegalArgumentException(KEYWORD + " must have exactly three arguments. The Locale, IU, and key"); //$NON-NLS-1$
	}

	public synchronized Object evaluate(ExpressionContext context, VariableScope scope) {
		Object[] args = new Object[] {operands[0].evaluate(context, scope), operands[1].evaluate(context, scope), operands[2].evaluate(context, scope)};
		ExpressionContext subContext = new ExpressionContext(Map.Entry.class, args, context, false);
		Iterator itor = localePropertiesExpr.evaluateAsIterator(subContext, localePropertiesExpr.defineScope());
		return itor.hasNext() ? ((Map.Entry) itor.next()).getValue() : null;
	}

	int countReferenceToEverything() {
		return 1; // Since we use a LocalizedMap
	}

	String getOperator() {
		return KEYWORD;
	}
}
