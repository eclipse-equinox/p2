/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public IQueryResult<T> perform(Iterator<T> iterator) {
		return decorated.perform(iterator);
	}

	public IExpression getExpression() {
		return decorated.getExpression();
	}

	public String toString() {
		if (userString != null)
			return userString;
		return decorated.toString();
	}
}
