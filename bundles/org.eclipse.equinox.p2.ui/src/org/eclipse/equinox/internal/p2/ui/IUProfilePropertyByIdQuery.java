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
package org.eclipse.equinox.internal.p2.ui;

import java.util.Iterator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property associated with the specified profile, whose value matches the provided value.
 * Uses the profile id instead of the profile to reference the profile.
 * The profile instance is cached only during the duration of the query.
 * This query is used instead of IUProfilePropertyQuery because we pass
 * this query to the automatic update checker and it will be referenced during
 * the life of the platform.  
 */
public class IUProfilePropertyByIdQuery extends IUPropertyQuery {
	private String profileId;
	private IProfile cachedProfile;

	/**
	 * Creates a new query on the given property name and value.
	 */
	public IUProfilePropertyByIdQuery(String profileId, String propertyName, String propertyValue) {
		super(propertyName, propertyValue);
		this.profileId = profileId;
	}

	protected String getProperty(IInstallableUnit iu, String name) {
		IProfile profile = getProfile();
		if (profile == null)
			return null;
		return profile.getInstallableUnitProperty(iu, name);
	}

	private IProfile getProfile() {
		if (cachedProfile == null)
			try {
				cachedProfile = ProvisioningUtil.getProfile(profileId);
			} catch (ProvisionException e) {
				// ignore, this will return null
			}
		return cachedProfile;
	}

	public Collector perform(Iterator iterator, Collector result) {
		Collector collector = super.perform(iterator, result);
		cachedProfile = null;
		return collector;
	}
}
