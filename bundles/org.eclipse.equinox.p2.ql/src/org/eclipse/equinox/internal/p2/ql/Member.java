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

import java.lang.reflect.*;

/**
 * <p>An expression that performs member calls to obtain some value
 * from some object instance. It uses standard bean semantics so
 * that an attempt to obtain &quot;value&quot; will cause an
 * attempt to call <code>getValue()</code> and if no such method
 * exists, <code>isValue()</code> and if that doesn't work either,
 * <code>value()</code>.</p>
 */
public final class Member extends Unary {
	static final String OPERATOR = "."; //$NON-NLS-1$

	private static final Class[] NO_ARG_TYPES = new Class[0];
	private static final Object[] NO_ARGS = new Object[0];
	private static final String GET_PREFIX = "get"; //$NON-NLS-1$
	private static final String IS_PREFIX = "is"; //$NON-NLS-1$

	protected final String name;

	private String methodName;
	private Class lastClass;
	private Method method;

	public Member(Expression operand, String name) {
		super(operand);
		this.name = name;

		if (!(name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)))
			name = GET_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		this.methodName = name;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object value = operand.evaluate(context, scope);
		if (value == null)
			throw new IllegalArgumentException("Cannot access member " + name + " in null"); //$NON-NLS-1$//$NON-NLS-2$
		return invoke(value);
	}

	public void toString(StringBuffer bld) {
		if (operand == Variable.ITEM || operand == Variable.EVERYTHING)
			bld.append(name);
		else {
			appendOperand(bld, operand, getPriority());
			bld.append('.');
			bld.append(name);
		}
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_MEMBER;
	}

	Object invoke(Object value) {
		Class c = value.getClass();
		if (lastClass == null || !lastClass.isAssignableFrom(c)) {
			Method m;
			for (;;) {
				try {
					m = c.getMethod(methodName, NO_ARG_TYPES);
					if (!Modifier.isPublic(m.getModifiers()))
						throw new NoSuchMethodException();
					break;
				} catch (NoSuchMethodException e) {
					if (methodName.startsWith(GET_PREFIX))
						// Switch from using getXxx() to isXxx()
						methodName = IS_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
					else if (methodName.startsWith(IS_PREFIX))
						// Switch from using isXxx() to xxx()
						methodName = name;
					else
						throw new IllegalArgumentException("Cannot find a public member " + name + " in a " + value.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
				}
			}

			// Since we already checked that it's public. This will speed
			// up the calls a bit.
			m.setAccessible(true);
			lastClass = c;
			method = m;
		}

		Exception checked;
		try {
			return method.invoke(value, NO_ARGS);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			checked = e;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getTargetException();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			if (cause instanceof Error)
				throw (Error) cause;
			checked = (Exception) cause;
		}
		throw new RuntimeException("Problem invoking " + methodName + " on a " + value.getClass().getName(), checked); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
