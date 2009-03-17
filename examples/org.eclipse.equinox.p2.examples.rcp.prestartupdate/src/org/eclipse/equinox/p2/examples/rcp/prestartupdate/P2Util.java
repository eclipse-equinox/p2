/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.examples.rcp.prestartupdate;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.DefaultPhaseSet;
import org.eclipse.equinox.internal.provisional.p2.engine.IEngine;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

public class P2Util {
	// XXX Check for updates to this application and return true if
	// we have installed updates and need a restart.
	// This method is intentionally long and ugly in order to provide
	// "one-stop-shopping" for how to check for and perform an update.
	static boolean checkForUpdates() {
		// Before we show a progress dialog, at least find out that we have
		// installed content and repos to check.
		final IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper
				.getService(Activator.bundleContext, IProfileRegistry.class
						.getName());
		if (profileRegistry == null)
			return false;
		final IProfile profile = profileRegistry
				.getProfile(IProfileRegistry.SELF);
		if (profile == null)
			return false;

		// We are going to look for updates to all IU's in the profile. A
		// different query could be used if we are looking for updates to
		// a subset. For example, the p2 UI only looks for updates to those
		// IU's marked with a special property.
		final Collector collector = profile.query(InstallableUnitQuery.ANY,
				new Collector(), null);
		if (collector.isEmpty())
			return false;
		final IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper
				.getService(Activator.bundleContext,
						IMetadataRepositoryManager.class.getName());
		if (manager == null)
			return false;
		final URI[] reposToSearch = manager
				.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
		if (reposToSearch.length == 0)
			return false;
		final IPlanner planner = (IPlanner) ServiceHelper.getService(
				Activator.bundleContext, IPlanner.class.getName());
		if (planner == null)
			return false;
		// Looking in all known repositories for updates for each IU in the profile
		final boolean[] didWeUpdate = new boolean[1];
		didWeUpdate[0] = false;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				// We'll break progress up into 4 steps.
				// 1.  Load repos - it is not strictly necessary to do this.
				//     The planner will do it for us.  However, burying this
				//     in the planner's progress reporting will not 
				//     show enough progress initially, so we do it manually.
                // 2.  Get update list
				// 3.  Build a profile change request and get a provisioning plan
				// 4.  Perform the provisioning plan.
				SubMonitor sub = SubMonitor.convert(monitor,
						"Checking for application updates...", 400);
				// 1.  Load repos
				SubMonitor loadMonitor = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
				for (int i=0; i<reposToSearch.length; i++)
					try {
						if (loadMonitor.isCanceled())
							throw new InterruptedException();
						manager.loadRepository(reposToSearch[i], loadMonitor.newChild(100/reposToSearch.length));
					} catch (ProvisionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				loadMonitor.done();
				
				// 2.  Get update list.
				// First we look for replacement IU's for each IU
				ArrayList iusWithUpdates = new ArrayList();
				ArrayList replacementIUs = new ArrayList();
				Iterator iter = collector.iterator();
				ProvisioningContext pc = new ProvisioningContext(reposToSearch);
				SubMonitor updateSearchMonitor = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
				while (iter.hasNext()) {
					if (updateSearchMonitor.isCanceled())
						throw new InterruptedException();
					IInstallableUnit iu = (IInstallableUnit) iter.next();
					IInstallableUnit[] replacements = planner.updatesFor(iu,
							pc, updateSearchMonitor.newChild(100/collector.size()));
					if (replacements.length > 0) {
						iusWithUpdates.add(iu);
						if (replacements.length == 1)
							replacementIUs.add(replacements[0]);
						else {
							IInstallableUnit repl = replacements[0];
							for (int i = 1; i < replacements.length; i++)
								if (replacements[i].getVersion().compareTo(
										repl.getVersion()) > 0)
									repl = replacements[i];
							replacementIUs.add(repl);
						}
					}
				}
				// Did we find any updates?
				if (iusWithUpdates.size() == 0) {
					sub.done();
				} else {
					if (sub.isCanceled())
						throw new InterruptedException();
					// 3.  Build a profile change request and get a provisioning plan
					ProfileChangeRequest changeRequest = new ProfileChangeRequest(
							profile);
					changeRequest
							.removeInstallableUnits((IInstallableUnit[]) iusWithUpdates
									.toArray(new IInstallableUnit[iusWithUpdates
											.size()]));
					changeRequest
							.addInstallableUnits((IInstallableUnit[]) iusWithUpdates
									.toArray(new IInstallableUnit[iusWithUpdates
											.size()]));
					ProvisioningPlan plan = planner.getProvisioningPlan(
							changeRequest, pc, sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS));
					if (plan.getStatus().getSeverity() == IStatus.CANCEL)
						throw new InterruptedException();
					if (plan.getStatus().getSeverity() != IStatus.ERROR) {
						IEngine engine = (IEngine) ServiceHelper.getService(
								Activator.bundleContext, IEngine.class
										.getName());
						IArtifactRepositoryManager artifactMgr = (IArtifactRepositoryManager) ServiceHelper
								.getService(Activator.bundleContext,
										IArtifactRepositoryManager.class
												.getName());
						if (engine != null && artifactMgr != null) {
							// 4.  Perform the provisioning plan
							pc
									.setArtifactRepositories(artifactMgr
											.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));
							IStatus status = engine.perform(profile,
									new DefaultPhaseSet(), plan.getOperands(),
									pc, sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS));
							if (status.getSeverity() == IStatus.CANCEL)
								throw new InterruptedException();
							if (status.getSeverity() != IStatus.ERROR) {
								didWeUpdate[0] = true;
							}
						}
					}
				}
			}
		};
		try {
			new ProgressMonitorDialog(null).run(true, true, runnable);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return didWeUpdate[0];
	}
}
