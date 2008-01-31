/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.query;

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.director.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;

/**
 * An object that adds queryable support to the profile registry.
 */
public class QueryableUpdates implements IQueryable {

	private IInstallableUnit[] iusToUpdate;

	public QueryableUpdates(IInstallableUnit[] iusToUpdate) {
		this.iusToUpdate = iusToUpdate;
	}

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		int totalWork = 2000;
		monitor.beginTask(ProvUIMessages.QueryableUpdates_UpdateListProgress, totalWork);
		IPlanner planner = (IPlanner) ServiceHelper.getService(ProvUIActivator.getContext(), IPlanner.class.getName());
		if (planner == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoPlannerFound));
			return result;
		}
		ArrayList allUpdates = new ArrayList();
		for (int i = 0; i < iusToUpdate.length; i++) {
			IInstallableUnit[] updates = planner.updatesFor(iusToUpdate[i], new ProvisioningContext(), new SubProgressMonitor(monitor, totalWork / 2 / iusToUpdate.length));
			for (int j = 0; j < updates.length; j++)
				allUpdates.add(updates[j]);
		}
		for (int i = 0; i < allUpdates.size(); i++) {
			if (query.isMatch(allUpdates.get(i)))
				result.accept(allUpdates.get(i));
			monitor.worked(totalWork / 2 / allUpdates.size());
		}
		monitor.done();
		return result;
	}
}
