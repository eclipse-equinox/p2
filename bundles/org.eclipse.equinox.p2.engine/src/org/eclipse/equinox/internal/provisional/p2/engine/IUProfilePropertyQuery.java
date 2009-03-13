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
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IUPropertyQuery;

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property associated with the specified profile, whose value matches the provided value.
 */
public class IUProfilePropertyQuery extends IUPropertyQuery {
	private IProfile profile;

	/**
	 * Creates a new query on the given property name and value.
	 * Because the queryable for this query is typically the profile
	 * instance, we use a reference to the profile rather than the
	 * profile id for performance reasons.
	 */
	public IUProfilePropertyQuery(IProfile profile, String propertyName, String propertyValue) {
		super(propertyName, propertyValue);
		this.profile = profile;
	}

	protected String getProperty(IInstallableUnit iu, String name) {
		return profile.getInstallableUnitProperty(iu, name);
	}
}
