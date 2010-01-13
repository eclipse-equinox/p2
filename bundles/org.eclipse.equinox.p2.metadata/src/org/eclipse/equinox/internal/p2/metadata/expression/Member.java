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
package org.eclipse.equinox.internal.p2.metadata.expression;

import java.lang.reflect.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpressionVisitor;

/**
 * <p>An expression that performs member calls to obtain some value
 * from some object instance. It uses standard bean semantics so
 * that an attempt to obtain &quot;value&quot; will cause an
 * attempt to call <code>getValue()</code> and if no such method
 * exists, <code>isValue()</code> and if that doesn't work either,
 * <code>value()</code>.</p>
 */
public abstract class Member extends Unary {

	public static final class DynamicMember extends Member {
		private static final String GET_PREFIX = "get"; //$NON-NLS-1$
		private static final String IS_PREFIX = "is"; //$NON-NLS-1$
		private static final Class<?>[] NO_ARG_TYPES = new Class[0];

		private Class<?> lastClass;

		private Method method;
		private String methodName;

		DynamicMember(Expression operand, String name) {
			super(operand, name, Expression.emptyArray);
			if (!(name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)))
				name = GET_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
			this.methodName = name;
		}

		public Object evaluate(IEvaluationContext context) {
			return invoke(operand.evaluate(context));
		}

		public Object invoke(Object self) {
			if (self == null)
				throw new IllegalArgumentException("Cannot access member \'" + name + "\' in null"); //$NON-NLS-1$//$NON-NLS-2$

			if (self instanceof MemberProvider)
				return ((MemberProvider) self).getMember(name);

			Class<?> c = self.getClass();
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
							throw new IllegalArgumentException("Cannot find a public member \'" + name + "\' in a " + self.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
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
				return method.invoke(self, NO_ARGS);
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
			throw new RuntimeException("Problem invoking " + methodName + " on a " + self.getClass().getName(), checked); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	static final Object[] NO_ARGS = new Object[0];

	static Member createDynamicMember(Expression operand, String name) {
		return new DynamicMember(operand, name);
	}

	protected final Expression[] argExpressions;

	final String name;

	protected Member(Expression operand, String name, Expression[] args) {
		super(operand);
		this.name = name;
		this.argExpressions = args;
	}

	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor))
			for (int idx = 0; idx < argExpressions.length; ++idx)
				if (!argExpressions[idx].accept(visitor))
					return false;
		return true;
	}

	public int compareTo(Expression e) {
		int cmp = super.compareTo(e);
		if (cmp == 0) {
			cmp = name.compareTo(((Member) e).name);
			if (cmp == 0)
				cmp = compare(argExpressions, ((Member) e).argExpressions);
		}
		return cmp;
	}

	public boolean equals(Object o) {
		if (super.equals(o)) {
			Member mo = (Member) o;
			return name.equals(mo.name) && equals(argExpressions, mo.argExpressions);
		}
		return false;
	}

	public int getExpressionType() {
		return TYPE_MEMBER;
	}

	public String getName() {
		return name;
	}

	public String getOperator() {
		return OPERATOR_MEMBER;
	}

	public int getPriority() {
		return PRIORITY_MEMBER;
	}

	public int hashCode() {
		int result = 31 + name.hashCode();
		result = 31 * result + operand.hashCode();
		return 31 * result + hashCode(argExpressions);
	}

	public void toString(StringBuffer bld, Variable rootVariable) {
		if (operand != rootVariable) {
			appendOperand(bld, rootVariable, operand, getPriority());
			bld.append('.');
		}
		bld.append(name);
		if (argExpressions.length > 0) {
			bld.append('(');
			elementsToString(bld, rootVariable, argExpressions);
			bld.append(')');
		}
	}
}
