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

import java.util.Arrays;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An object that adds queryable support to the profile registry.
 */
public class QueryableProfileRegistry implements IQueryable<IProfile> {

	private ProvisioningUI ui;

	public QueryableProfileRegistry(ProvisioningUI ui) {
		this.ui = ui;
	}

	public IQueryResult<IProfile> query(IQuery<IProfile> query, IProgressMonitor monitor) {
		IProfile[] profiles = ProvUI.getProfileRegistry(ui.getSession()).getProfiles();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableProfileRegistry_QueryProfileProgress, profiles.length);
		try {
			return query.perform(Arrays.asList(profiles).iterator());
		} finally {
			sub.done();
		}
	}
}
