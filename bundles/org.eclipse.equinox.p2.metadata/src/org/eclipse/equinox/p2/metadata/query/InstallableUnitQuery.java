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
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.Query;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

/**
 * A query that matches on the id and version of an {@link IInstallableUnit}.
 */
public class InstallableUnitQuery extends Query {
	private String id;
	private final VersionRange range;

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, and whose version falls in the provided range.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param range The version range to match
	 */
	public InstallableUnitQuery(String id, VersionRange range) {
		this.id = id;
		this.range = range;
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 * @param version The precise version that a matching unit must have
	 */
	public InstallableUnitQuery(String id, Version version) {
		this.id = id;
		this.range = new VersionRange(version, true, version, true);
	}

	/**
	 * Returns the id that this query will match against.
	 * @return The installable unit it
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the version range that this query will match against.
	 * @return The installable unit version range.
	 */
	public VersionRange getRange() {
		return range;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.query2.Query#isMatch(java.lang.Object)
	 */
	public boolean isMatch(Object object) {
		if (!(object instanceof IInstallableUnit))
			return false;
		IInstallableUnit candidate = (IInstallableUnit) object;
		if (id != null && !id.equals(candidate.getId()))
			return false;
		if (range != null && !range.isIncluded(candidate.getVersion()))
			return false;
		return true;
	}

}
