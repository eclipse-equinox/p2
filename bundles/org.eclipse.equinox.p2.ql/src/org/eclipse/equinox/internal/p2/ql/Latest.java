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
package org.eclipse.equinox.internal.p2.ql;

import org.eclipse.equinox.p2.metadata.IVersionedId;

import java.util.*;

/**
 * An expression that is especially targeted towards {@link IVersionedId} instances. It will
 * reject any objects that is not an <code>IVersionedId</code> and it will ensure that the
 * resulting iterator only iterates over the latest version of any found id.
 */
public final class Latest extends UnaryCollectionFilter {

	static final String OPERATOR = "latest"; //$NON-NLS-1$

	public Latest(Expression collection) {
		super(collection);
	}

	public Object evaluate(ExpressionContext context, VariableScope scope) {
		Class instanceClass = context.getInstanceClass();
		if (!IVersionedId.class.isAssignableFrom(instanceClass))
			return Collections.EMPTY_SET.iterator();

		HashMap greatestIUVersion;
		if (operand instanceof Select) {
			// Inline element evaluation here so that we don't build a map that is
			// larger then it has to be
			Select select = (Select) operand;
			Iterator iterator = select.operand.evaluateAsIterator(context, scope);
			if (!iterator.hasNext())
				return Collections.EMPTY_SET.iterator();

			greatestIUVersion = new HashMap();
			scope = select.lambda.prolog(context, scope);
			while (iterator.hasNext()) {
				Object next = iterator.next();
				if (!instanceClass.isInstance(next))
					continue;

				select.variable.setValue(scope, next);
				if (select.lambda.evaluate(context, scope) != Boolean.TRUE)
					continue;

				IVersionedId versionedID = (IVersionedId) next;
				String id = versionedID.getId();
				IVersionedId prev = (IVersionedId) greatestIUVersion.put(id, versionedID);
				if (prev == null)
					continue;
				if (prev.getVersion().compareTo(versionedID.getVersion()) > 0)
					greatestIUVersion.put(id, prev);
			}
		} else {
			Iterator iterator = operand.evaluateAsIterator(context, scope);
			if (iterator == null)
				return null;
			if (!iterator.hasNext())
				return Collections.EMPTY_SET.iterator();

			greatestIUVersion = new HashMap();
			while (iterator.hasNext()) {
				Object next = iterator.next();
				if (!instanceClass.isInstance(next))
					continue;

				IVersionedId versionedID = (IVersionedId) next;
				String id = versionedID.getId();

				IVersionedId prev = (IVersionedId) greatestIUVersion.put(id, versionedID);
				if (prev == null)
					continue;

				if (prev.getVersion().compareTo(versionedID.getVersion()) > 0)
					greatestIUVersion.put(id, prev);
			}
		}
		return greatestIUVersion.values().iterator();
	}

	String getOperator() {
		return OPERATOR;
	}
}
