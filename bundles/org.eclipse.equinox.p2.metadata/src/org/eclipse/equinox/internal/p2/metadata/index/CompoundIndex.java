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
package org.eclipse.equinox.internal.p2.metadata.index;

import java.util.*;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.metadata.index.IIndex;

public class CompoundIndex<T> implements IIndex<T> {

	private final Collection<IIndex<T>> indexes;

	public CompoundIndex(Collection<IIndex<T>> indexes) {
		this.indexes = indexes;
	}

	public Iterator<T> getCandidates(IEvaluationContext ctx, IExpression variable, IExpression booleanExpr) {
		Set<T> result = null;
		for (IIndex<T> index : indexes) {
			Iterator<T> indexResult = index.getCandidates(ctx, variable, booleanExpr);
			if (indexResult == null)
				return null;
			if (indexResult.hasNext()) {
				if (result == null)
					result = new HashSet<T>();
				do {
					result.add(indexResult.next());
				} while (indexResult.hasNext());
			}
		}
		if (result == null)
			result = Collections.emptySet();
		return result.iterator();
	}
}
