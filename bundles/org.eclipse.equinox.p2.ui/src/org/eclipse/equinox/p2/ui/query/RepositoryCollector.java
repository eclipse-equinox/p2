/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.p2.ui.model.RepositoryElement;

/**
 * Collector that accepts the matched Profiles and
 * wraps them in a ProfileElement.
 * 
 * @since 3.4
 */
public class RepositoryCollector extends QueriedElementCollector {

	public RepositoryCollector(IProvElementQueryProvider queryProvider, IQueryable queryable) {
		super(queryProvider, queryable);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		// Circumvent the code that gets/sets the queryable of this element,
		// as that will cause loading of the repository.
		if (match instanceof RepositoryElement) {
			if (match instanceof MetadataRepositoryElement)
				((MetadataRepositoryElement) match).setQueryProvider(queryProvider);
			getList().add(match);
			return true;
		}
		return super.accept(match);
	}

}
