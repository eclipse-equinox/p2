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
package org.eclipse.equinox.p2.engine.query;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.MatchQuery;

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property associated with the specified profile, whose value matches the provided value.
 * @since 2.0
 */
public class IUProfilePropertyQuery extends MatchQuery<IInstallableUnit> {
	public static final String ANY = "*"; //$NON-NLS-1$

	private final String propertyName;
	private final String propertyValue;
	private IProfile profile;

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void setProfile(IProfile profile) {
		this.profile = profile;
	}

	/**
	 * Creates a new query on the given property name and value.
	 * Because the queryable for this query is typically the profile
	 * instance, we use a reference to the profile rather than the
	 * profile id for performance reasons.
	 * @param propertyName The name of the property to match
	 * @param propertyValue The value to compare to. A value of &quot;*&quot; means any value.
	 */
	public IUProfilePropertyQuery(String propertyName, String propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	@Override
	public boolean isMatch(IInstallableUnit candidate) {
		if (profile == null)
			return false;
		String foundValue = profile.getInstallableUnitProperty(candidate, propertyName);
		return foundValue == null ? propertyValue == null : (ANY.equals(propertyValue) || foundValue.equals(propertyValue));
	}
}
