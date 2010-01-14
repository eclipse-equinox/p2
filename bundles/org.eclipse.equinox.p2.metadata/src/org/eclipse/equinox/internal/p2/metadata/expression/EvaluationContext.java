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

import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * Highly specialized evaluation contexts optimized for misc purposes
 */
public class EvaluationContext implements IEvaluationContext {
	public static class Parameters extends EvaluationContext {
		private static final Object[] noParameters = new Object[0];

		private final Object[] parameters;

		public Parameters(IEvaluationContext parentContext, Object[] parameters) {
			super(parentContext);
			this.parameters = parameters == null ? noParameters : parameters;
		}

		public Object getParameter(int position) {
			return parameters[position];
		}
	}

	public static class SingleVariableContext implements IEvaluationContext {
		private final IEvaluationContext parentContext;

		private Object value;

		private final IExpression variable;

		public SingleVariableContext(IEvaluationContext parentContext, IExpression variable) {
			this.parentContext = parentContext;
			this.variable = variable;
		}

		public Object getParameter(int position) {
			return parentContext.getParameter(position);
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

	static class MultiVariableContext implements IEvaluationContext {
		private final IEvaluationContext parentContext;

		private final Object[] values;

		public MultiVariableContext(IEvaluationContext parentContext, IExpression[] variables) {
			this.parentContext = parentContext;
			values = new Object[variables.length * 2];
			for (int idx = 0, ndx = 0; ndx < variables.length; ++ndx, idx += 2)
				values[idx] = variables[ndx];
		}

		public Object getParameter(int position) {
			return parentContext.getParameter(position);
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

	public static final EvaluationContext INSTANCE = new EvaluationContext(null);

	public static IEvaluationContext create() {
		return INSTANCE;
	}

	public static IEvaluationContext create(IEvaluationContext parent, IExpression variable) {
		return new SingleVariableContext(parent, variable);
	}

	public static IEvaluationContext create(IEvaluationContext parent, IExpression[] variables) {
		return variables.length == 1 ? new SingleVariableContext(parent, variables[0]) : new MultiVariableContext(parent, variables);
	}

	public static IEvaluationContext create(IEvaluationContext parent, Object[] parameters) {
		return new Parameters(parent, parameters);
	}

	public static IEvaluationContext create(IExpression variable) {
		return new SingleVariableContext(null, variable);
	}

	public static IEvaluationContext create(IExpression[] variables) {
		if (variables == null || variables.length == 0)
			return INSTANCE;
		return variables.length == 1 ? create(variables[0]) : new MultiVariableContext(INSTANCE, variables);
	}

	public static IEvaluationContext create(Object[] parameters, IExpression variable) {
		return parameters == null || parameters.length == 0 ? create(variable) : new SingleVariableContext(new Parameters(null, parameters), variable);
	}

	public static IEvaluationContext create(Object[] parameters, IExpression[] variables) {
		if (parameters == null || parameters.length == 0)
			return create(variables);

		Parameters pctx = new Parameters(null, parameters);
		if (variables == null || variables.length == 0)
			return pctx;
		return create(pctx, variables);
	}

	protected EvaluationContext(IEvaluationContext parentContext) {
		this.parentContext = parentContext;
	}

	private final IEvaluationContext parentContext;

	public Object getParameter(int position) {
		if (parentContext == null)
			throw new IllegalArgumentException("No such parameter: $" + position); //$NON-NLS-1$
		return parentContext.getParameter(position);
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
}
