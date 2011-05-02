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
package org.eclipse.equinox.p2.metadata.expression;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.index.IIndexProvider;

/**
 * This is an expression that will need access to the global variable
 * <code>everything</code>.
 * @since 2.0
 */
public interface IContextExpression<T> extends IExpression {

	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will have the variable 'everything' set to an expression that represents
	 * the <code>everything</code> iterator filtered for instances of <code>elementClass</code>.</p>
	 * <p>The values of the iterator will be copied if necessary (when everything is referenced
	 * more then once).</p>
	 * @param elementClass the class of the iterator elements
	 * @param indexProvider The index provider that represents all queried material.
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Class<? extends T> elementClass, IIndexProvider<T> indexProvider);

	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will have the variable 'everything' set to an expression that represents
	 * the <code>everything</code> iterator filtered for instances of <code>elementClass</code>.</p>
	 * <p>The values of the iterator will be copied if necessary (when everything is referenced
	 * more then once).</p>
	 * @param elementClass the class of the iterator elements
	 * @param everything The iterator that represents all queried material.
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Class<? extends T> elementClass, Iterator<T> everything);

	/**
	 * Returns the parameters that this context expression was created with.
	 * @return An array of parameters, possibly empty but never <code>null</code>.
	 */
	Object[] getParameters();

	/**
	 * Evaluate the expression and return the expected collection result as an iterator
	 * @param context The evaluation context
	 * @return The result of the evaluation.
	 */
	Iterator<T> iterator(IEvaluationContext context);
}
