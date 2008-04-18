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

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUVersionsElement;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * Collector that only accepts categories or the latest version of each
 * IU, and wraps that version in an IUVersionsElement.
 * 
 * @since 3.4
 */
public class LatestIUVersionElementCollector extends LatestIUVersionCollector {
	public LatestIUVersionElementCollector(IQueryProvider queryProvider, IQueryable queryable, QueryContext queryContext, boolean makeCategories) {
		super(queryProvider, queryable, queryContext, makeCategories);
	}

	protected Object makeDefaultElement(IInstallableUnit iu) {
		return new IUVersionsElement(iu);
	}

	protected IInstallableUnit getIU(Object matchElement) {
		if (matchElement instanceof IUVersionsElement)
			return ((IUVersionsElement) matchElement).getIU();
		return super.getIU(matchElement);
	}

}
