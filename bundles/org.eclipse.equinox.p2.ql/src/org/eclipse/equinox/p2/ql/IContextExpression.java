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
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * This is an expression that will need access to the global variable
 * <code>everything</code>.
 */
public interface IContextExpression<T> extends org.eclipse.equinox.p2.metadata.expression.IContextExpression<T> {
	/**
	 * <p>Creates a new context to be passed to a subsequent evaluation. The context
	 * will have the variable 'everything' set to an expression that represents
	 * the <code>everything</code> iterator filtered for instances of <code>elementClass</code>.</p>
	 * <p>The values of the iterator will be copied if necessary (when everything is referenced
	 * more then once).</p>
	 * @param everything The iterator that represents all queried material.
	 * @param translations A translation support object to be assigned to the variable 'translations'
	 * @return A new evaluation context.
	 */
	IEvaluationContext createContext(Class<T> elementClass, Iterator<T> everything, ITranslationSupport translations);
}
