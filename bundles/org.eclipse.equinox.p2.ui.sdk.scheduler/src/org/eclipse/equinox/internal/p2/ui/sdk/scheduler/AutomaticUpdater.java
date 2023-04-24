/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
 *     Sonatype, Inc. - ongoing development
 *     Christian Georgi <christian.georgi@sap.com> - Bug 432887 - Setting to show update wizard w/o notification popup
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *     Vasili Gulevich (Spirent Communications) - Bug #254
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.5
 */
public class AutomaticUpdater implements IUpdateListener {

	StatusLineCLabelContribution updateAffordance;
	IStatusLineManager statusLineManager;
	Collection<IInstallableUnit> iusWithUpdates;
	String profileId;
	ProvisioningListener profileListener;
	AutomaticUpdatesPopup popup;
	UpdatesPopup failPopup;
	boolean alreadyDownloaded = false;
	UpdateOperation operation;
	private static final String AUTO_UPDATE_STATUS_ITEM = "AutoUpdatesStatus"; //$NON-NLS-1$
	private static final String WARN_LOCKED_PROPERTY = AutomaticUpdatePlugin.getContext().getBundle().getSymbolicName()
			+ ".warnLocked"; //$NON-NLS-1$
	/**
	 * https://github.com/eclipse-equinox/p2/issues/254 If all updatable IUs are
	 * preinstalled in a locked profile, warn user about outdated software
	 **/
	private static final boolean WARN_ABOUT_LOCKED_UPDATES = Boolean
			.parseBoolean(AutomaticUpdatePlugin.getContext().getProperty(WARN_LOCKED_PROPERTY));

	public AutomaticUpdater() {
		createProfileListener();
	}

	private void createProfileListener() {
		profileListener = o -> {
			if (o instanceof IProfileEvent) {
				IProfileEvent event = (IProfileEvent) o;
				if (event.getReason() == IProfileEvent.CHANGED && sameProfile(event.getProfileId())) {
					triggerNewUpdateNotification();
				}
			}
		};
		getProvisioningEventBus().addListener(profileListener);
	}

	boolean sameProfile(String another) {
		if (IProfileRegistry.SELF.equals(another)) {
			IProfile profile = getProfileRegistry().getProfile(another);
			if (profile != null) {
				another = profile.getProfileId();
			}
		}
		if (IProfileRegistry.SELF.equals(profileId)) {
			IProfile profile = getProfileRegistry().getProfile(profileId);
			if (profile != null) {
				profileId = profile.getProfileId();
			}
		}
		return (profileId == another) || (profileId != null && profileId.equals(another));
	}

	@Override
	public void updatesAvailable(UpdateEvent event) {
		updatesAvailable(event, true);
	}

	@Override
	public void checkingForUpdates() {
		new LastAutoCheckForUpdateMemo(AutomaticUpdatePlugin.getDefault().getAgentLocation())
				.store(Calendar.getInstance().getTime());
	}

	void updatesAvailable(final UpdateEvent event, final boolean notifyWithPopup) {
		final boolean download = getPreferenceStore().getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		profileId = event.getProfileId();
		iusWithUpdates = event.getIUs();
		validateIusToUpdate();
		alreadyDownloaded = false;

		final boolean showUpdateWizard = getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_UPDATE_WIZARD);

		// Create an update operation to reflect the new updates that are available.
		operation = new UpdateOperation(getSession(), iusWithUpdates);
		operation.setProfileId(event.getProfileId());
		IStatus status = operation.resolveModal(new NullProgressMonitor());

		if (!status.isOK() || operation.getPossibleUpdates() == null || operation.getPossibleUpdates().length == 0) {
			if (WARN_ABOUT_LOCKED_UPDATES) {
				operation = new UpdateOperation(getSession(), event.getIUs());
				operation.setProfileId(event.getProfileId());
				status = operation.resolveModal(new NullProgressMonitor());
				if (status.matches(IStatus.ERROR)) {
					StatusManager.getManager().handle(status, StatusManager.LOG);
					asyncExec(this::clearUpdateAffordances);
				} else {
					asyncExec(() -> notifyUserOfUpdates(false, notifyWithPopup, false));
				}
			} else {
				asyncExec(this::clearUpdateAffordances);
			}
			return;
		}
		// Download the items before notifying user if the
		// preference dictates.

		if (download) {
			ProfileModificationJob job = new ProfileModificationJob(
					AutomaticUpdateMessages.AutomaticUpdater_AutomaticDownloadOperationName, getSession(),
					event.getProfileId(), operation.getProvisioningPlan(),
					new ProvisioningContext(getSession().getProvisioningAgent()));
			job.setPhaseSet(PhaseSetFactory.createPhaseSetIncluding(new String[] { PhaseSetFactory.PHASE_COLLECT }));
			job.setUser(false);
			job.setSystem(true);
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent jobEvent) {
					IStatus jobStatus = jobEvent.getResult();
					if (jobStatus.isOK()) {
						alreadyDownloaded = true;
						asyncExec(() -> notifyUserOfUpdates(operation.getResolutionResult().isOK(), notifyWithPopup,
								showUpdateWizard));
					} else if (jobStatus.getSeverity() != IStatus.CANCEL) {
						StatusManager.getManager().handle(jobStatus, StatusManager.LOG);
					}
				}
			});
			job.schedule();
		} else {
			asyncExec(() -> notifyUserOfUpdates(operation.getResolutionResult().isOK(), notifyWithPopup,
					showUpdateWizard));
		}

	}

	private void asyncExec(Runnable runnable) {
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	ProvisioningSession getSession() {
		return AutomaticUpdatePlugin.getDefault().getSession();
	}

	/*
	 * Use with caution, as this still start the whole UI bundle. Shouldn't be used
	 * in any of the update checking code, only the code that presents updates when
	 * notified.
	 */
	ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	/*
	 * Filter out the ius that aren't visible to the user or are locked for
	 * updating.
	 */

	void validateIusToUpdate() {
		ArrayList<IInstallableUnit> list = new ArrayList<>(iusWithUpdates.size());
		IProfile profile = getProfileRegistry().getProfile(profileId);

		if (profile != null) {
			for (IInstallableUnit iuWithUpdate : iusWithUpdates) {
				try {
					if (validToUpdate(profile, iuWithUpdate))
						list.add(iuWithUpdate);
				} catch (OperationCanceledException e) {
					// Nothing to report
				}
			}
		}
		iusWithUpdates = list;
	}

	// A proposed update is valid if it is still visible to the user as an
	// installed item (it is a root)
	// and if it is not locked for updating.
	private boolean validToUpdate(IProfile profile, IInstallableUnit iu) {
		int lock = IProfile.LOCK_NONE;
		boolean isRoot = false;
		try {
			String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				lock = Integer.parseInt(value);
			value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
			isRoot = value == null ? false : Boolean.parseBoolean(value);
		} catch (NumberFormatException e) {
			// ignore and assume no lock
		}
		return isRoot && (lock & IProfile.LOCK_UPDATE) == 0;
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
		// YUCK! YUCK! YUCK!
		// IWorkbenchWindow does not define getStatusLineManager(), yet
		// WorkbenchWindow does
		try {
			Method method = activeWindow.getClass().getDeclaredMethod("getStatusLineManager"); //$NON-NLS-1$
			try {
				Object statusLine = method.invoke(activeWindow);
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

		IWorkbenchPage page = activeWindow.getActivePage();
		if (page == null)
			return null;
		IWorkbenchPart part = page.getActivePart();
		if (part == null)
			return null;
		IWorkbenchPartSite site = part.getSite();
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

	void createUpdateAffordance() {
		updateAffordance = new StatusLineCLabelContribution(AUTO_UPDATE_STATUS_ITEM, 5);
		updateAffordance.addListener(SWT.MouseDown, event -> launchUpdate());
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.add(updateAffordance);
			manager.update(true);
		}
	}

	void notifyUserOfUpdates(boolean isValid, boolean showPopup, boolean showUpdateWizard) {
		if (updateAffordance == null)
			createUpdateAffordance();
		if (isValid) {
			if (showPopup) {
				if (showUpdateWizard)
					launchUpdate();
				else
					openUpdatePopup();
			}
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdates);
			updateAffordance.setImage(
					AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_UPDATE)));
		} else {
			if (showPopup) {
				openUpdateFailPopup();
			}
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdatesWithProblems);
			updateAffordance.setImage(AutomaticUpdatePlugin.getDefault().getImageRegistry()
					.get((AutomaticUpdatePlugin.IMG_TOOL_UPDATE_PROBLEMS)));
		}
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.update(true);
		}
	}

	void checkUpdateAffordanceEnablement() {
		// We don't currently support enablement in the affordance,
		// so we hide it if it should not be enabled.
		if (updateAffordance == null)
			return;
		boolean shouldBeVisible = getProvisioningUI().hasScheduledOperations();
		if (updateAffordance.isVisible() != shouldBeVisible) {
			IStatusLineManager manager = getStatusLineManager();
			if (manager != null) {
				updateAffordance.setVisible(shouldBeVisible);
				manager.update(true);
			}
		}
	}

	void openUpdatePopup() {
		if (popup == null)
			popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, getPreferenceStore());
		popup.open();

	}

	void openUpdateFailPopup() {
		if (failPopup == null)
			failPopup = new AutomaticUpdatesFailPopup(getWorkbenchWindowShell());
		failPopup.open();
	}

	void clearUpdateAffordances() {
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
		if (failPopup != null) {
			failPopup.close();
			failPopup = null;
		}
	}

	public void launchUpdate() {
		getProvisioningUI().openUpdateWizard(false, operation, null);
	}

	/*
	 * The profile has changed. Make sure our toUpdate list is still valid and if
	 * there is nothing to update, get rid of the update popup and affordance.
	 */
	void triggerNewUpdateNotification() {
		Job notifyJob = new Job("Update validate job") { //$NON-NLS-1$
			@Override
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				// notify that updates are available for all roots. We don't know for sure that
				// there are any, but this will cause everything to be rechecked. Don't trigger
				// a popup, just update the affordance and internal state.
				updatesAvailable(new UpdateEvent(profileId, getInstalledIUs()));
				return Status.OK_STATUS;
			}
		};
		notifyJob.setSystem(true);
		notifyJob.setUser(false);
		notifyJob.setPriority(Job.LONG);
		notifyJob.schedule();
	}

	/*
	 * Get the IInstallable units for the specified profile
	 * 
	 * @param profileId the profile in question
	 * 
	 * @param all <code>true</code> if all IInstallableUnits in the profile should
	 * be returned, <code>false</code> only those IInstallableUnits marked as (user
	 * visible) roots should be returned.
	 * 
	 * @return an array of IInstallableUnits installed in the profile.
	 */
	public Collection<IInstallableUnit> getInstalledIUs() {
		IProfile profile = getProfileRegistry().getProfile(profileId);
		if (profile == null)
			return Collections.emptyList();
		IQuery<IInstallableUnit> query = new UserVisibleRootQuery();
		IQueryResult<IInstallableUnit> queryResult = profile.query(query, null);
		return queryResult.toUnmodifiableSet();
	}

	public void shutdown() {
		statusLineManager = null;
		if (profileListener != null) {
			getProvisioningEventBus().removeListener(profileListener);
			profileListener = null;
		}
	}

	IProfileRegistry getProfileRegistry() {
		return getSession().getProvisioningAgent().getService(IProfileRegistry.class);
	}

	IProvisioningEventBus getProvisioningEventBus() {
		return getSession().getProvisioningAgent().getService(IProvisioningEventBus.class);
	}

	IPreferenceStore getPreferenceStore() {
		return AutomaticUpdatePlugin.getDefault().getPreferenceStore();
	}

}
