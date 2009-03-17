package org.eclipse.equinox.p2.examples.rcp.prestartupdate;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.
	 * IApplicationContext)
	 */
	public Object start(IApplicationContext context) {
		Display display = PlatformUI.createDisplay();
		try {
			// XXX check for updates before ever running a workbench
			if (checkForUpdates(display))
				// An update was done, we need to restart
				return IApplication.EXIT_RESTART;
			else {
				int returnCode = PlatformUI.createAndRunWorkbench(display,
						new ApplicationWorkbenchAdvisor());
				if (returnCode == PlatformUI.RETURN_RESTART) {
					return IApplication.EXIT_RESTART;
				}
			}
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}

	// XXX Check for updates to this application and return true if
	// we have installed updates and need a restart.
	// This method is intentionally long and ugly in order to provide
	// "one-stop-shopping" for how to check for and perform an update.
	private boolean checkForUpdates(Display display) {
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
				SubMonitor sub = SubMonitor.convert(monitor,
						"Checking for application updates...", 200 + (collector
								.size() * 100));
				// First we look for replacement IU's for each IU
				ArrayList iusWithUpdates = new ArrayList();
				ArrayList replacementIUs = new ArrayList();
				Iterator iter = collector.iterator();
				ProvisioningContext pc = new ProvisioningContext(reposToSearch);
				while (iter.hasNext()) {
					if (sub.isCanceled())
						throw new InterruptedException();
					IInstallableUnit iu = (IInstallableUnit) iter.next();
					IInstallableUnit[] replacements = planner.updatesFor(iu,
							pc, sub.newChild(100));
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
					// Build a profile change request
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
							changeRequest, pc, sub.newChild(100));
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
							pc
									.setArtifactRepositories(artifactMgr
											.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL));
							IStatus status = engine.perform(profile,
									new DefaultPhaseSet(), plan.getOperands(),
									pc, sub.newChild(100));
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
