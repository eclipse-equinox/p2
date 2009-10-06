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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that adds queryable support to the profile registry.
 */
public class QueryableProfileRegistry implements IQueryable {

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound), StatusManager.SHOW | StatusManager.LOG);
			return result;
		}
		IProfile[] profiles = profileRegistry.getProfiles();
		SubMonitor sub = SubMonitor.convert(monitor, ProvUIMessages.QueryableProfileRegistry_QueryProfileProgress, profiles.length);
		try {
			query.perform(Arrays.asList(profiles).iterator(), result);
		} finally {
			sub.done();
		}
		return result;
	}
}
