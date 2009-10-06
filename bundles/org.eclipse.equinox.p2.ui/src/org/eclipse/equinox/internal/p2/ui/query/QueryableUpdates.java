/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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

import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * An object that implements a query for available updates
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
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoPlannerFound), StatusManager.SHOW | StatusManager.LOG);
			return result;
		}
		try {
			ArrayList allUpdates = new ArrayList();
			for (int i = 0; i < iusToUpdate.length; i++) {
				if (monitor.isCanceled())
					return result;
				IInstallableUnit[] updates = planner.updatesFor(iusToUpdate[i], new ProvisioningContext(), new SubProgressMonitor(monitor, totalWork / 2 / iusToUpdate.length));
				for (int j = 0; j < updates.length; j++)
					allUpdates.add(updates[j]);
			}
			query.perform(allUpdates.iterator(), result);
		} catch (OperationCanceledException e) {
			// Nothing more to do, return result
		} finally {
			monitor.done();
		}
		return result;
	}
}
