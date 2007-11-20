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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIActivator;

/**
 * An object that adds queryable support to an artifact repository 
 * manager.
 */
public class QueryableProfileRegistry implements IQueryable {

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(ProvUIActivator.getContext(), IProfileRegistry.class.getName());
		if (profileRegistry == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoProfileRegistryFound));
			return result;
		}
		Profile[] profiles = profileRegistry.getProfiles();
		for (int i = 0; i < profiles.length; i++) {
			if (query.isMatch(profiles[i]))
				result.accept(profiles[i]);
		}
		return result;
	}
}
