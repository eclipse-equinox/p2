/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.query;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An object that implements a query for available updates
 */
public class QueryableUpdates implements IQueryable<IInstallableUnit> {

	private IInstallableUnit[] iusToUpdate;
	ProvisioningUI ui;

	public QueryableUpdates(ProvisioningUI ui, IInstallableUnit[] iusToUpdate) {
		this.ui = ui;
		this.iusToUpdate = iusToUpdate;
	}

	@Override
	public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		int totalWork = 2000;
		monitor.beginTask(ProvUIMessages.QueryableUpdates_UpdateListProgress, totalWork);
		IPlanner planner = (IPlanner) ui.getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
		try {
			Set<IInstallableUnit> allUpdates = new HashSet<>();
			for (IInstallableUnit unit : iusToUpdate) {
				if (monitor.isCanceled())
					return Collector.emptyCollector();
				IQueryResult<IInstallableUnit> updates = planner.updatesFor(unit, new ProvisioningContext(ui.getSession().getProvisioningAgent()), SubMonitor.convert(monitor, totalWork / 2 / iusToUpdate.length));
				allUpdates.addAll(updates.toUnmodifiableSet());
			}
			return query.perform(allUpdates.iterator());
		} catch (OperationCanceledException e) {
			// Nothing more to do, return result
			return Collector.emptyCollector();
		} finally {
			monitor.done();
		}
	}
}
