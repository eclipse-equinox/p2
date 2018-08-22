/*******************************************************************************
 * Copyright (c) 2009 - 2010 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.expression;

import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * The evaluation context. Contexts can be nested and new contexts are pushed for each closure
 * during an evaluation of an expression.
 * @since 2.0
 */
public interface IEvaluationContext {
	IIndexProvider<?> getIndexProvider();

	void setIndexProvider(IIndexProvider<?> indexProvider);

	/**
	 * Retrieve the value of the given <code>variable</code> from this context
	 * @param variable The variable who's value should be retrieved
	 * @return The current value for the variable
	 */
	Object getValue(IExpression variable);

	/**
	 * Set the current value for the given <code>variable</code> to <code>value</code>
	 * @param variable The variable who's value should be set
	 * @param value The new value for the variable.
	 */
	void setValue(IExpression variable, Object value);

	/**
	 * Returns the value of the parameter at the given <code>position</code>
	 * @param position The zero based position for the parameter
	 * @return The parameter value
	 */
	Object getParameter(int position);
}
