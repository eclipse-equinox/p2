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

/**
 * A query that searches for {@link IInstallableUnit} instances that have
 * a property associated with the specified profile, whose value matches the provided value.
 */
public class IUProfilePropertyQuery extends IUPropertyQuery {
	private Profile profile;

	/**
	 * Creates a new query on the given property name and value.
	 */
	public IUProfilePropertyQuery(Profile profile, String propertyName, String propertyValue) {
		super(propertyName, propertyValue);
		this.profile = profile;
	}

	protected String getProperty(IInstallableUnit iu, String name) {
		return profile.getInstallableUnitProfileProperty(iu, name);
	}
}
