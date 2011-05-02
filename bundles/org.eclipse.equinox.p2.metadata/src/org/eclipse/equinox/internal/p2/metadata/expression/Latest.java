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

import java.util.*;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.expression.IEvaluationContext;

/**
 * An expression that is especially targeted towards {@link IVersionedId} instances. It will
 * reject any objects that is not an <code>IVersionedId</code> and it will ensure that the
 * resulting iterator only iterates over the latest version of any found id.
 */
final class Latest extends UnaryCollectionFilter {

	Latest(Expression collection) {
		super(collection);
	}

	public Iterator<?> evaluateAsIterator(IEvaluationContext context) {
		HashMap<String, IVersionedId> greatestIUVersion;
		if (operand instanceof Select) {
			// Inline element evaluation here so that we don't build a map that is
			// larger then it has to be
			Select select = (Select) operand;
			Iterator<?> iterator = select.operand.evaluateAsIterator(context);
			if (!iterator.hasNext())
				return Collections.EMPTY_SET.iterator();

			greatestIUVersion = new HashMap<String, IVersionedId>();
			LambdaExpression lambda = select.lambda;
			context = lambda.prolog(context);
			Variable variable = lambda.getItemVariable();
			while (iterator.hasNext()) {
				Object next = iterator.next();
				if (!(next instanceof IVersionedId))
					continue;

				variable.setValue(context, next);
				if (lambda.evaluate(context) != Boolean.TRUE)
					continue;

				IVersionedId versionedID = (IVersionedId) next;
				String id = versionedID.getId();
				IVersionedId prev = greatestIUVersion.put(id, versionedID);
				if (prev == null)
					continue;
				if (prev.getVersion().compareTo(versionedID.getVersion()) > 0)
					greatestIUVersion.put(id, prev);
			}
		} else {
			Iterator<?> iterator = operand.evaluateAsIterator(context);
			if (iterator == null)
				return null;
			if (!iterator.hasNext())
				return Collections.EMPTY_SET.iterator();

			greatestIUVersion = new HashMap<String, IVersionedId>();
			while (iterator.hasNext()) {
				Object next = iterator.next();
				if (!(next instanceof IVersionedId))
					continue;

				IVersionedId versionedID = (IVersionedId) next;
				String id = versionedID.getId();

				IVersionedId prev = greatestIUVersion.put(id, versionedID);
				if (prev == null)
					continue;

				if (prev.getVersion().compareTo(versionedID.getVersion()) > 0)
					greatestIUVersion.put(id, prev);
			}
		}
		return greatestIUVersion.values().iterator();
	}

	public int getExpressionType() {
		return TYPE_LATEST;
	}

	public String getOperator() {
		return IExpressionConstants.KEYWORD_LATEST;
	}
}
