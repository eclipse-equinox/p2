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
import org.eclipse.equinox.p2.ui.model.ProfileElement;
import org.eclipse.equinox.p2.ui.model.QueriedElementCollector;

/**
 * Collector that accepts the matched Profiles and
 * wraps them in a ProfileElement.
 * 
 * @since 3.4
 */
public class ProfileElementCollector extends QueriedElementCollector {

	public ProfileElementCollector(IProvElementQueryProvider queryProvider, Profile profile) {
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
		if (!(match instanceof Profile))
			return true;
		return super.accept(new ProfileElement(((Profile) match).getProfileId()));
	}

}
