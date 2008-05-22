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

import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.model.QueriedElementCollector;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;

/**
 * Collectors that accepts the matched IU's and
 * wraps them in an InstalledIUElement.
 * 
 * @since 3.4
 */
public class InstalledIUCollector extends QueriedElementCollector {

	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232413
	private boolean queryNameProperty = false;

	public InstalledIUCollector(IQueryProvider queryProvider, IProfile profile, QueryContext queryContext) {
		super(queryProvider, profile, queryContext);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	public boolean accept(Object match) {
		if (!(match instanceof IInstallableUnit))
			return true;
		if (queryable instanceof IProfile) {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232413
			// access the IU's name while collecting to prevent a later lockup in the UI thread.
			if (queryNameProperty)
				IUPropertyUtils.getIUProperty((IInstallableUnit) match, IInstallableUnit.PROP_NAME);
			return super.accept(new InstalledIUElement(((IProfile) queryable).getProfileId(), (IInstallableUnit) match));
		}
		// shouldn't happen, but is possible if a client reset the queryable to a non-profile.
		return super.accept(match);
	}

	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=232413
	public void fetchNamePropertyWhileCollecting() {
		this.queryNameProperty = true;
	}

}
