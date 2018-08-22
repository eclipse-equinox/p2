/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;

/*
 * This class is used to keep pretty print a normal query using the information passed in.
 */
public class PrettyQuery<T> implements IQuery<T> {
	private IQuery<T> decorated;
	private String userString;

	public PrettyQuery(IQuery<T> toDecorate, String userReadable) {
		decorated = toDecorate;
		userString = userReadable;
	}

	@Override
	public IQueryResult<T> perform(Iterator<T> iterator) {
		return decorated.perform(iterator);
	}

	@Override
	public IExpression getExpression() {
		return decorated.getExpression();
	}

	@Override
	public String toString() {
		if (userString != null)
			return userString;
		return decorated.toString();
	}
}
