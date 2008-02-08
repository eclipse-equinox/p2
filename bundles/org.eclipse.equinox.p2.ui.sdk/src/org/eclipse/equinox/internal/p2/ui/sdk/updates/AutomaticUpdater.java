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
package org.eclipse.equinox.internal.p2.ui.sdk.updates;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EventObject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.sdk.*;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.ProfileEvent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.p2.ui.model.ProfileElement;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	Preferences prefs;
	StatusLineCLabelContribution updateAffordance;
	IStatusLineManager statusLineManager;
	IInstallableUnit[] updatesFound;
	IInstallableUnit[] toUpdate;
	String profileId;
	AutomaticUpdatesPopup popup;
	ProvisioningListener profileChangeListener;
	private static final String AUTO_UPDATE_STATUS_ITEM = "AutoUpdatesStatus"; //$NON-NLS-1$

	public AutomaticUpdater() {
		prefs = ProvSDKUIActivator.getDefault().getPluginPreferences();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.updatechecker.IUpdateListener#updatesAvailable(org.eclipse.equinox.p2.updatechecker.UpdateEvent)
	 */
	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = prefs.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		profileId = event.getProfileId();
		updatesFound = event.getIUs();
		// Recompute the updates that we want to make available to the user.
		toUpdate = getUpdatesToShow(updatesFound, new NullProgressMonitor());

		if (toUpdate.length <= 0) {
			clearUpdatesAvailable();
			return;
		}
		registerProfileChangeListener();

		// Download the items if the preference dictates before
		// showing the user that updates are available.
		try {
			if (download) {
				UpdateEvent eventWithOnlyRoots = new UpdateEvent(event.getProfileId(), toUpdate);
				ElementQueryDescriptor descriptor = ProvSDKUIActivator.getDefault().getQueryProvider().getQueryDescriptor(eventWithOnlyRoots, IQueryProvider.AVAILABLE_UPDATES);
				IInstallableUnit[] replacements = (IInstallableUnit[]) descriptor.queryable.query(descriptor.query, descriptor.collector, null).toArray(IInstallableUnit.class);
				if (replacements.length > 0) {
					ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(event.getProfileId());
					request.removeInstallableUnits(toUpdate);
					request.addInstallableUnits(replacements);
					final ProvisioningPlan plan = ProvisioningUtil.getPlanner().getProvisioningPlan(request, new ProvisioningContext(), null);
					Job job = ProvisioningOperationRunner.schedule(new ProfileModificationOperation(ProvSDKMessages.AutomaticUpdater_AutomaticDownloadOperationName, event.getProfileId(), plan, new DownloadPhaseSet(), false), null);
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent jobEvent) {
							IStatus status = jobEvent.getResult();
							if (status.isOK()) {
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										showUpdatesAvailable(true);
									}
								});
							} else if (status.getSeverity() != IStatus.CANCEL) {
								ProvUI.reportStatus(status);
							}
						}
					});
				}
			} else {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						showUpdatesAvailable(false);
					}
				});
			}

		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}

	}

	// Figure out which updates we want to expose to the user.
	// Updates of IU's below the user's visiblity will not be shown.
	IInstallableUnit[] getUpdatesToShow(final IInstallableUnit[] iusWithUpdates, IProgressMonitor monitor) {
		// We could simply collect the install roots ourselves, but implementing
		// this in terms of a normal "what's installed" query allows the policy to be defined only
		// in one place.
		IQueryable rootQueryable = new IQueryable() {
			public Collector query(Query query, Collector result, IProgressMonitor pm) {
				for (int i = 0; i < iusWithUpdates.length; i++)
					if (query.isMatch(iusWithUpdates[i])) {
						IInstallableUnit iu = (IInstallableUnit) ProvUI.getAdapter(iusWithUpdates[i], IInstallableUnit.class);
						if (iu != null) {
							// It's possible that the update list is stale, so for install roots that had updates available,
							// we do one more check here to ensure that an update is still available for it.  Otherwise
							// we risk notifying the user of updates and then not finding them (which can still happen, but
							// we are trying to reduce the window in which it can happen.
							try {
								if (ProvisioningUtil.getPlanner().updatesFor(iu, new ProvisioningContext(), pm).length > 0)
									result.accept(iusWithUpdates[i]);
							} catch (ProvisionException e) {
								ProvUI.handleException(e, null);
								continue;
							}
						}
					}
				return result;
			}
		};
		ProfileElement element = new ProfileElement(profileId);
		ElementQueryDescriptor descriptor = ProvSDKUIActivator.getDefault().getQueryProvider().getQueryDescriptor(element, IQueryProvider.INSTALLED_IUS);
		Object[] elements = rootQueryable.query(descriptor.query, descriptor.collector, null).toArray(Object.class);
		IInstallableUnit[] result = new IInstallableUnit[elements.length];
		for (int i = 0; i < result.length; i++)
			result[i] = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		return result;
	}

	Shell getWorkbenchWindowShell() {
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return activeWindow != null ? activeWindow.getShell() : null;

	}

	IStatusLineManager getStatusLineManager() {
		if (statusLineManager != null)
			return statusLineManager;
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow == null)
			return null;
		// YUCK!  YUCK!  YUCK!
		// IWorkbenchWindow does not define getStatusLineManager(), yet WorkbenchWindow does
		try {
			Method method = activeWindow.getClass().getDeclaredMethod("getStatusLineManager", new Class[0]); //$NON-NLS-1$
			try {
				Object statusLine = method.invoke(activeWindow, new Object[0]);
				if (statusLine instanceof IStatusLineManager) {
					statusLineManager = (IStatusLineManager) statusLine;
					return statusLineManager;
				}
			} catch (InvocationTargetException e) {
				// oh well
			} catch (IllegalAccessException e) {
				// I tried
			}
		} catch (NoSuchMethodException e) {
			// can't blame us for trying.
		}

		IWorkbenchPartSite site = activeWindow.getActivePage().getActivePart().getSite();
		if (site instanceof IViewSite) {
			statusLineManager = ((IViewSite) site).getActionBars().getStatusLineManager();
		} else if (site instanceof IEditorSite) {
			statusLineManager = ((IEditorSite) site).getActionBars().getStatusLineManager();
		}
		return statusLineManager;
	}

	void updateStatusLine() {
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null)
			manager.update(true);
	}

	private void createUpdateAffordance() {
		updateAffordance = new StatusLineCLabelContribution(AUTO_UPDATE_STATUS_ITEM, 5);
		updateAffordance.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				launchUpdate();
			}
		});
		updateAffordance.setTooltip(ProvSDKMessages.AutomaticUpdatesDialog_UpdatesAvailableTitle);
		updateAffordance.setImage(ProvUIImages.getImage(ProvUIImages.IMG_TOOL_UPDATE));
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.add(updateAffordance);
			manager.update(true);
		}
	}

	private void createUpdatePopup(boolean alreadyDownloaded) {
		popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, prefs);
		popup.open();

	}

	void showUpdatesAvailable(boolean alreadyDownloaded) {
		if (updateAffordance == null)
			createUpdateAffordance();
		if (popup == null)
			createUpdatePopup(alreadyDownloaded);
	}

	void clearUpdatesAvailable() {
		if (updateAffordance != null) {
			IStatusLineManager manager = getStatusLineManager();
			if (manager != null) {
				manager.remove(updateAffordance);
				manager.update(true);
			}
			updateAffordance.dispose();
			updateAffordance = null;
		}
		if (popup != null) {
			popup.close(false);
			popup = null;
		}
	}

	public void launchUpdate() {
		UpdateWizard wizard = new UpdateWizard(profileId, toUpdate, ProvSDKUIActivator.getDefault().getLicenseManager(), ProvSDKUIActivator.getDefault().getQueryProvider());
		WizardDialog dialog = new WizardDialog(getWorkbenchWindowShell(), wizard);
		if (dialog.open() == Window.OK)
			clearUpdatesAvailable();
	}

	private void registerProfileChangeListener() {
		if (profileChangeListener == null) {
			profileChangeListener = new ProvisioningListener() {
				public void notify(EventObject o) {
					if (o instanceof ProfileEvent) {
						ProfileEvent event = (ProfileEvent) o;
						if (event.getReason() == ProfileEvent.CHANGED && profileId.equals(event.getProfileId())) {
							validateUpdates();
						}
					}
				}
			};
			ProvisioningEventBus bus = ProvSDKUIActivator.getDefault().getProvisioningEventBus();
			if (bus != null)
				bus.addListener(profileChangeListener);
		}
	}

	/*
	 * The profile has changed.  Make sure our toUpdate list is
	 * still valid and if there is nothing to update, get rid
	 * of the update popup and affordance.
	 */
	void validateUpdates() {
		Job validateJob = new WorkbenchJob("Update validate job") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				toUpdate = getUpdatesToShow(updatesFound, monitor);
				if (toUpdate.length == 0)
					clearUpdatesAvailable();
				return Status.OK_STATUS;
			}
		};
		validateJob.setSystem(true);
		validateJob.setPriority(Job.SHORT);
		validateJob.schedule();
	}

	public void shutdown() {
		if (profileChangeListener == null)
			return;
		ProvisioningEventBus bus = ProvSDKUIActivator.getDefault().getProvisioningEventBus();
		if (bus != null)
			bus.removeListener(profileChangeListener);
		profileChangeListener = null;
		statusLineManager = null;
	}

}
