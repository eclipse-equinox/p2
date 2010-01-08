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
package org.eclipse.equinox.p2.ql;

import java.util.Iterator;

/**
 * This is an expression that will need access to the global variable
 * <code>everything</code>.
 */
public interface IContextExpression<T> extends IExpression {
	/**
	 * Returns the element class
	 * @return The element class
	 */
	Class<T> getElementClass();

	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will have the variable 'everything' set to an expression that represents
	 * the <code>everything</code> iterator filtered for instances of <code>elementClass</code>.</p>
	 * <p>The values of the iterator will be copied if necessary (when everything is referenced
	 * more then once).</p>
	 * @param everything The iterator that represents all queried material.
	 * @param params The parameters to use for the evaluation.
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Iterator<T> everything, Object[] params);

	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will have the variable 'everything' set to an expression that represents
	 * the <code>everything</code> iterator filtered for instances of <code>elementClass</code>.</p>
	 * <p>The values of the iterator will be copied if necessary (when everything is referenced
	 * more then once).</p>
	 * @param everything The iterator that represents all queried material.
	 * @param params The parameters to use for the evaluation.
	 * @param translations A translation support object to be assigned to the variable 'translations'
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Iterator<T> everything, Object[] params, ITranslationSupport translations);
}
