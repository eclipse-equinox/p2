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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * Highly specialized evaluation contexts optimized for misc purposes
 */
public class EvaluationContext implements IEvaluationContext {
	public static class SingleVariableContext extends EvaluationContext {
		private Object value;

		private final IExpression variable;

		public SingleVariableContext(EvaluationContext parentContext, IExpression variable, Object[] parameters) {
			super(parentContext, parameters);
			this.variable = variable;
		}

		public Object getValue(IExpression var) {
			return variable == var ? value : parentContext.getValue(var);
		}

		public void setValue(IExpression var, Object val) {
			if (variable == var)
				value = val;
			else
				parentContext.setValue(var, val);
		}
	}

	public static class MultiVariableContext extends EvaluationContext {
		private final Object[] values;

		public MultiVariableContext(EvaluationContext parentContext, IExpression[] variables, Object[] parameters) {
			super(parentContext, parameters);
			values = new Object[variables.length * 2];
			for (int idx = 0, ndx = 0; ndx < variables.length; ++ndx, idx += 2)
				values[idx] = variables[ndx];
		}

		public Object getValue(IExpression variable) {
			for (int idx = 0; idx < values.length; ++idx)
				if (values[idx++] == variable)
					return values[idx];
			return parentContext.getValue(variable);
		}

		public void setValue(IExpression variable, Object value) {
			for (int idx = 0; idx < values.length; ++idx)
				if (values[idx++] == variable) {
					values[idx] = value;
					return;
				}
			parentContext.setValue(variable, value);
		}
	}

	private static final Object[] noParameters = new Object[0];

	private static final EvaluationContext INSTANCE = new EvaluationContext(null, noParameters);

	public static IEvaluationContext create() {
		return INSTANCE;
	}

	public static IEvaluationContext create(IEvaluationContext parent, IExpression variable) {
		return new SingleVariableContext((EvaluationContext) parent, variable, ((EvaluationContext) parent).parameters);
	}

	public static IEvaluationContext create(IEvaluationContext parent, IExpression[] variables) {
		return create(parent, ((EvaluationContext) parent).parameters, variables);
	}

	public static IEvaluationContext create(IEvaluationContext parent, Object[] parameters, IExpression[] variables) {
		if (variables == null || variables.length == 0)
			return create(parent, parameters);
		if (parameters == null)
			parameters = noParameters;
		return variables.length == 1 ? new SingleVariableContext((EvaluationContext) parent, variables[0], parameters) : new MultiVariableContext((EvaluationContext) parent, variables, parameters);
	}

	public static IEvaluationContext create(IEvaluationContext parent, Object[] parameters) {
		if (parameters == null)
			parameters = noParameters;
		return new EvaluationContext((EvaluationContext) parent, parameters);
	}

	public static IEvaluationContext create(IExpression variable) {
		return new SingleVariableContext(INSTANCE, variable, noParameters);
	}

	public static IEvaluationContext create(IExpression[] variables) {
		return create(INSTANCE, noParameters, variables);
	}

	public static IEvaluationContext create(Object[] parameters, IExpression variable) {
		if (parameters == null)
			parameters = noParameters;
		return new SingleVariableContext(INSTANCE, variable, parameters);
	}

	public static IEvaluationContext create(Object[] parameters, IExpression[] variables) {
		return create(INSTANCE, parameters, variables);
	}

	final EvaluationContext parentContext;

	private final Object[] parameters;

	private IIndexProvider<?> indexProvider;

	EvaluationContext(EvaluationContext parentContext, Object[] parameters) {
		this.parentContext = parentContext;
		this.parameters = parameters;
	}

	public final Object getParameter(int position) {
		return parameters[position];
	}

	public Object getValue(IExpression variable) {
		if (parentContext == null)
			throw new IllegalArgumentException("No such variable: " + variable); //$NON-NLS-1$
		return parentContext.getValue(variable);
	}

	public void setValue(IExpression variable, Object value) {
		if (parentContext == null)
			throw new IllegalArgumentException("No such variable: " + variable); //$NON-NLS-1$
		parentContext.setValue(variable, value);
	}

	public IIndexProvider<?> getIndexProvider() {
		if (indexProvider == null) {
			if (parentContext == null)
				return null;
			return parentContext.getIndexProvider();
		}
		return indexProvider;
	}

	public void setIndexProvider(IIndexProvider<?> indexProvider) {
		this.indexProvider = indexProvider;
	}
}
