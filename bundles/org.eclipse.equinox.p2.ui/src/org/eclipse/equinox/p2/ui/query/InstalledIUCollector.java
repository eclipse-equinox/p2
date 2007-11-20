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

import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.model.InstalledIUElement;

/**
 * Collectors that accepts the matched IU's and
 * wraps them in an InstalledIUElement.
 * 
 * @since 3.4
 */
public class InstalledIUCollector extends QueriedElementCollector {

	public InstalledIUCollector(IProvElementQueryProvider queryProvider, Profile profile) {
		super(queryProvider, profile);
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
		if (queryable instanceof Profile)
			return super.accept(new InstalledIUElement((Profile) queryable, (IInstallableUnit) match));
		// shouldn't happen, but is possible if a client reset the queryable to a non-profile.
		return super.accept(match);
	}

}
