/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * RepositoryLocationQuery is a query that gathers repository
 * locations rather than repositories.  It is used when composing
 * queries against a QueryableRepositoryManager to indicate that the
 * repository need not be loaded to run the query.  
 * 
 * @since 3.5
 */
public class RepositoryLocationQuery implements IQuery<URI> {

	public IQueryResult<URI> perform(Iterator<URI> iterator) {
		Collector<URI> result = new Collector<URI>();
		while (iterator.hasNext()) {
			Object candidate = iterator.next();
			URI location = getLocation(candidate);
			if (location != null)
				if (!result.accept(location))
					break;
		}
		return result;
	}

	private URI getLocation(Object o) {
		if (o instanceof URI)
			return (URI) o;
		if (o instanceof IRepository<?>)
			return ((IRepository<?>) o).getLocation();
		return null;
	}

	public IExpression getExpression() {
		return null;
	}
}
