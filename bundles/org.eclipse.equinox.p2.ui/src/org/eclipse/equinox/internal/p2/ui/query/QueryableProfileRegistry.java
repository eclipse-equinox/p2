/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
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

	private List<IProfile> getProfiles() {
		return Arrays.asList(ProvUI.getProfileRegistry(ui.getSession()).getProfiles());
	}

	@Override
	public IQueryResult<IProfile> query(IQuery<IProfile> query, IProgressMonitor monitor) {
		List<IProfile> profiles = getProfiles();
		monitor.beginTask(ProvUIMessages.QueryableProfileRegistry_QueryProfileProgress, profiles.size());
		try {
			return query.perform(profiles.iterator());
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean contains(IProfile profile) {
		return getProfiles().contains(profile);
	}
}
