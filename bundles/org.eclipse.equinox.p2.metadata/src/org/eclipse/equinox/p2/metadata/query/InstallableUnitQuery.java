/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata.query;

import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * A query that matches on the id and version of an {@link IInstallableUnit}.
 * @since 2.0
 */
public class InstallableUnitQuery extends MatchQuery<IInstallableUnit> {
	/**
	 * A convenience query that will match any {@link IInstallableUnit}
	 * it encounters.
	 */
	public static final InstallableUnitQuery ANY = new InstallableUnitQuery((String) null);

	private String id;
	private final VersionRange range;

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id, regardless of version.
	 * 
	 * @param id The installable unit id to match, or <code>null</code> to match any id
	 */
	public InstallableUnitQuery(String id) {
		this.id = id;
		this.range = null;
	}

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
		this.range = (version == null || Version.emptyVersion.equals(version)) ? null : new VersionRange(version, true, version, true);
	}

	/**
	 * Creates a query that will match any {@link IInstallableUnit} with the given
	 * id and version.
	 * 
	 * @param versionedId The precise id/version combination that a matching unit must have
	 */
	public InstallableUnitQuery(IVersionedId versionedId) {
		this(versionedId.getId(), versionedId.getVersion());
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
	public boolean isMatch(IInstallableUnit candidate) {
		if (id != null && !id.equals(candidate.getId()))
			return false;
		if (range != null && !range.isIncluded(candidate.getVersion()))
			return false;
		return true;
	}

}
