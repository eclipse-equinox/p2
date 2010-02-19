/*******************************************************************************
 * Copyright (c) 2010 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.index;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;

/**
 * Indexed access to the elements provided by an IQueryable
 * @since 2.0
 */
public interface IIndex<T> {
	/**
	 * Obtains the elements that are candidates for the given <code>booleanExpr</code> when applied
	 * using the given <code>variable</code> as <code>this</code>.
	 * The returned set of elements are the elements that must be present in order for the expression
	 * to evaluate to <code>true</code>. The set may contain false positives.
	 * 
	 * TODO: Write more about how the valid set of elements is determined.
	 * 
	 * @param ctx The evaluation context used when examining the <code>booleanExpr</code>.
	 * @param variable The variable used as <code>this</code>.
	 * @param booleanExpr The boolean expression.
	 * @return The candidate elements or <code>null</code> if this index cannot be used.
	 */
	Iterator<T> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr);
}
