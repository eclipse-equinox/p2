/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.query;

import java.net.URL;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * Collector that accepts the matched repo URLs and
 * wraps them in a MetadataRepositoryElement.
 * 
 * @since 3.4
 */
public class MetadataRepositoryElementCollector extends QueriedElementCollector {

	public MetadataRepositoryElementCollector(IQueryProvider queryProvider, QueryContext queryContext) {
		super(queryProvider, null, queryContext);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof URL))
			return true;
		return super.accept(new MetadataRepositoryElement((URL) match));
	}

}
