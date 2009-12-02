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
public abstract class Member extends Unary {

	public static Member createDynamicMember(Expression operand, String name) {
		return new DynamicMember(operand, name);
	}

	static final String OPERATOR = "."; //$NON-NLS-1$
	static final Object[] NO_ARGS = new Object[0];

	final String name;
	final Expression[] argExpressions;

	static final class CapabilityIndex_satisfiesAny extends Member {

		static final String ID = "satisfiesAny"; //$NON-NLS-1$

		public CapabilityIndex_satisfiesAny(Expression operand, Expression[] argExpressions) {
			super(operand, ID, NAry.assertLength(argExpressions, 1, 1, ID));
		}

		Object invoke(Object self, Object[] args) {
			return ((CapabilityIndex) self).satisfiesAny(args[0]);
		}
	}

	static final class CapabilityIndex_satisfiesAll extends Member {

		static final String ID = "satisfiesAll"; //$NON-NLS-1$

		public CapabilityIndex_satisfiesAll(Expression operand, Expression[] argExpressions) {
			super(operand, ID, NAry.assertLength(argExpressions, 1, 1, ID));
		}

		Object invoke(Object self, Object[] args) {
			return ((CapabilityIndex) self).satisfiesAll(args[0]);
		}
	}

	public boolean accept(Visitor visitor) {
		if (super.accept(visitor))
			for (int idx = 0; idx < argExpressions.length; ++idx)
				if (!argExpressions[idx].accept(visitor))
					return false;
		return true;
	}

	public Member(Expression operand, String name, Expression[] args) {
		super(operand);
		this.name = name;
		this.argExpressions = args;
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Object value = operand.evaluate(context, scope);
		if (value == null)
			throw new IllegalArgumentException("Cannot access member " + name + " in null"); //$NON-NLS-1$//$NON-NLS-2$
		Object[] args = NO_ARGS;
		int top = argExpressions.length;
		if (top > 0) {
			args = new Object[top];
			for (int idx = 0; idx < top; ++idx)
				args[idx] = argExpressions[idx].evaluate(context, scope);
		}
		return invoke(value, args);
	}

	public void toString(StringBuffer bld) {
		if (operand == Variable.ITEM || operand == Variable.EVERYTHING)
			bld.append(name);
		else {
			appendOperand(bld, operand, getPriority());
			bld.append('.');
			bld.append(name);
		}
		if (argExpressions.length > 0) {
			bld.append('(');
			Array.elementsToString(bld, argExpressions);
			bld.append(')');
		}
	}

	String getOperator() {
		return OPERATOR;
	}

	int getPriority() {
		return ExpressionParser.PRIORITY_MEMBER;
	}

	abstract Object invoke(Object value, Object[] args);

	static final class DynamicMember extends Member {
		private static final Class[] NO_ARG_TYPES = new Class[0];
		private static final String GET_PREFIX = "get"; //$NON-NLS-1$
		private static final String IS_PREFIX = "is"; //$NON-NLS-1$

		public DynamicMember(Expression operand, String name) {
			super(operand, name, Expression.emptyArray);
			if (!(name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)))
				name = GET_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
			this.methodName = name;
		}

		private Class lastClass;
		private Method method;
		private String methodName;

		Object invoke(Object value, Object[] args) {
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

		Class getMemberClass() {
			return null;
		}
	}
}
