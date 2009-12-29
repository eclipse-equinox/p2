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

import java.lang.reflect.*;
import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.ql.*;

/**
 * <p>An expression that performs member calls to obtain some value
 * from some object instance. It uses standard bean semantics so
 * that an attempt to obtain &quot;value&quot; will cause an
 * attempt to call <code>getValue()</code> and if no such method
 * exists, <code>isValue()</code> and if that doesn't work either,
 * <code>value()</code>.</p>
 */
abstract class Member extends Unary {

	static Member createDynamicMember(Expression operand, String name) {
		return new DynamicMember(operand, name);
	}

	static final Object[] NO_ARGS = new Object[0];

	final String name;
	final Expression[] argExpressions;

	static abstract class CapabilityIndexMethod extends Member {
		public CapabilityIndexMethod(Expression operand, String name, Expression[] args) {
			super(operand, name, args);
		}

		final ICapabilityIndex getSelf(IEvaluationContext context) {
			Object self = operand.evaluate(context);
			if (self instanceof ICapabilityIndex)
				return (ICapabilityIndex) self;
			throw new IllegalArgumentException("lhs of member expected to be an ICapabilityIndex implementation"); //$NON-NLS-1$
		}

		public final Object evaluate(IEvaluationContext context) {
			return evaluateAsIterator(context);
		}

		boolean isCollection() {
			return true;
		}
	}

	static final class CapabilityIndex_satisfiesAny extends CapabilityIndexMethod {

		public CapabilityIndex_satisfiesAny(Expression operand, Expression[] argExpressions) {
			super(operand, KEYWORD_SATISFIES_ANY, NAry.assertLength(argExpressions, 1, 1, KEYWORD_SATISFIES_ANY));
		}

		@SuppressWarnings("unchecked")
		public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
			return getSelf(context).satisfiesAny((Iterator<IRequirement>) argExpressions[0].evaluateAsIterator(context));
		}
	}

	static final class CapabilityIndex_satisfiesAll extends CapabilityIndexMethod {

		public CapabilityIndex_satisfiesAll(Expression operand, Expression[] argExpressions) {
			super(operand, KEYWORD_SATISFIES_ALL, NAry.assertLength(argExpressions, 1, 1, KEYWORD_SATISFIES_ALL));
		}

		@SuppressWarnings("unchecked")
		public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
			return getSelf(context).satisfiesAll((Iterator<IRequirement>) argExpressions[0].evaluateAsIterator(context));
		}
	}

	public boolean accept(IExpressionVisitor visitor) {
		if (super.accept(visitor))
			for (int idx = 0; idx < argExpressions.length; ++idx)
				if (!argExpressions[idx].accept(visitor))
					return false;
		return true;
	}

	Member(Expression operand, String name, Expression[] args) {
		super(operand);
		this.name = name;
		this.argExpressions = args;
	}

	public int getExpressionType() {
		return TYPE_MEMBER;
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
		return OPERATOR_MEMBER;
	}

	int getPriority() {
		return PRIORITY_MEMBER;
	}

	static final class DynamicMember extends Member {
		private static final Class<?>[] NO_ARG_TYPES = new Class[0];
		private static final String GET_PREFIX = "get"; //$NON-NLS-1$
		private static final String IS_PREFIX = "is"; //$NON-NLS-1$

		DynamicMember(Expression operand, String name) {
			super(operand, name, Expression.emptyArray);
			if (!(name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX)))
				name = GET_PREFIX + Character.toUpperCase(name.charAt(0)) + name.substring(1);
			this.methodName = name;
		}

		private Class<?> lastClass;
		private Method method;
		private String methodName;

		public Object evaluate(IEvaluationContext context) {
			return invoke(operand.evaluate(context));
		}

		boolean isReferencingTranslations() {
			return VARIABLE_TRANSLATIONS.equals(name);
		}

		Object invoke(Object self) {
			if (self == null)
				throw new IllegalArgumentException("Cannot access member " + name + " in null"); //$NON-NLS-1$//$NON-NLS-2$

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
							throw new IllegalArgumentException("Cannot find a public member " + name + " in a " + self.getClass().getName()); //$NON-NLS-1$//$NON-NLS-2$
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
}
