/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.query.IQuery;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An object that adds queryable support to the profile registry.
 */
public class QueryableProfileRegistry implements IQueryable {

	private ProvisioningUI ui;

	public QueryableProfileRegistry(ProvisioningUI ui) {
		this.ui = ui;
	}

	public Collector query(IQuery query, IProgressMonitor monitor) {
		Collector result = new Collector();
		IProfile[] profiles = ui.getSession().getProfileRegistry().getProfiles();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableProfileRegistry_QueryProfileProgress, profiles.length);
		try {
			query.perform(Arrays.asList(profiles).iterator(), result);
		} finally {
			sub.done();
		}
		return result;
	}
}
