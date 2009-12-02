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

import java.util.ArrayList;
import java.util.Locale;

public class LocalizedKeys extends Function {
	static final String KEYWORD = "localizedKeys"; //$NON-NLS-1$

	public LocalizedKeys(Expression[] operands) {
		super(operands);
		int argCount = operands.length;
		if (argCount != 2)
			throw new IllegalArgumentException("localizedKeys must have exactly two arguments. The Locale and the key"); //$NON-NLS-1$
	}

	public synchronized Object evaluate(ExpressionContext context, VariableScope scope) {
		Object arg = operands[0].evaluate(context, scope);
		if (!(arg instanceof Locale))
			throw new IllegalArgumentException("localizedKeys first argument must be a java.util.Locale"); //$NON-NLS-1$
		Locale locale = (Locale) arg;

		arg = operands[1].evaluate(context, scope);
		if (!(arg instanceof String))
			throw new IllegalArgumentException("localizedKeys second argument must be a string"); //$NON-NLS-1$
		String key = (String) arg;

		ArrayList keyList = new ArrayList();
		StringBuffer bld = new StringBuffer();
		bld.append(locale.getLanguage());
		int pos = bld.length();
		bld.append('.');
		bld.append(key);
		keyList.add(bld.toString());
		bld.setLength(pos);

		bld.append('_');
		if (locale.getCountry().length() > 0) {
			bld.append(locale.getCountry());
			pos = bld.length();
			bld.append('.');
			bld.append(key);
			keyList.add(bld.toString());
			bld.setLength(pos);
		}

		if (locale.getVariant().length() > 0) {
			bld.append('_');
			bld.append(locale.getVariant());
			bld.append('.');
			bld.append(key);
			keyList.add(bld.toString());
		}
		bld.setLength(0);
		bld.append("df_LT."); //$NON-NLS-1$
		bld.append(key);
		keyList.add(bld.toString());
		return keyList.toArray(new String[keyList.size()]);
	}

	String getOperator() {
		return KEYWORD;
	}
}
