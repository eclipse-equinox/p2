/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElementWrapper;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * ElementWrapper that accepts the matched IU's and
 * wraps them in an InstalledIUElement.
 * 
 * @since 3.4
 */
public class InstalledIUElementWrapper extends QueriedElementWrapper {

	public InstalledIUElementWrapper(IQueryable<?> queryable, Object parent) {
		super(queryable, parent);
	}

	/**
	 * Accepts a result that matches the query criteria.
	 * 
	 * @param match an object matching the query
	 * @return <code>true</code> if the query should continue,
	 * or <code>false</code> to indicate the query should stop.
	 */
	protected boolean shouldWrap(Object match) {
		if (match instanceof IInstallableUnit)
			return true;
		return false;
	}

	/**
	 * Transforms the item to a UI element
	 */
	protected Object wrap(Object item) {
		if (queryable instanceof IProfile)
			return super.wrap(new InstalledIUElement(parent, ((IProfile) queryable).getProfileId(), (IInstallableUnit) item));
		// Shouldn't happen, the queryable should typically be a profile
		return super.wrap(item);
	}

}
