/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tools.mirror;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * A convenience query that will match any {@link IInstallableUnit}
 * it encounters.
 */
public class RangeQuery extends Query {
	private VersionRangedName[] targets;

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * VerionRangedName.
	 * 
	 * @param targets The installable unit names with versions to match, or <code>null</code> to match any id
	 */
	public RangeQuery(VersionRangedName[] targets) {
		this.targets = targets;
	}

	/**
	 * Returns true if the <code>IInstallableUnit</code> object is contained in the <code>VerionRangedName</code>'s or targets is null.
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;
		if (targets == null)
			return true;
		IInstallableUnit candidate = (IInstallableUnit) object;
		for (int i = 0; i < targets.length; i++) {
			VersionRangedName entry = targets[i];
			if (entry.getId().equalsIgnoreCase(candidate.getId()) && entry.getVersionRange().isIncluded(candidate.getVersion()))
				return true;
		}
		return false;
	}
}
