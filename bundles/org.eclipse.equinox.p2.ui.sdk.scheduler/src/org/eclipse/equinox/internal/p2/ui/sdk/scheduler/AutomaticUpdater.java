/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.5
 */
public class AutomaticUpdater implements IUpdateListener {

	StatusLineCLabelContribution updateAffordance;
	AutomaticUpdateAction updateAction;
	IStatusLineManager statusLineManager;
	IInstallableUnit[] iusWithUpdates;
	String profileId;
	AutomaticUpdatesPopup popup;
	IJobChangeListener provisioningJobListener;
	boolean alreadyValidated = false;
	boolean alreadyDownloaded = false;
	private static final String AUTO_UPDATE_STATUS_ITEM = "AutoUpdatesStatus"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener
	 * #
	 * updatesAvailable(org.eclipse.equinox.internal.provisional.p2.updatechecker
	 * .UpdateEvent)
	 */
	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = getPreferenceStore().getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		profileId = event.getProfileId();
		iusWithUpdates = event.getIUs();
		validateUpdates(null, true);
		alreadyDownloaded = false;

		if (iusWithUpdates.length <= 0) {
			clearUpdatesAvailable();
			return;
		}
		registerProvisioningJobListener();

		// Always get a profile change request and provisioning plan.
		// A side-effect of making the change request is producing the model
		// elements necessary for a wizard, so initialize the data structures
		// for getting these.
		final ArrayList initialSelections = new ArrayList();
		final IUElementListRoot root = new IUElementListRoot();
		try {
			ProfileChangeRequest request = UpdateWizard.createProfileChangeRequest(event.getIUs(), event.getProfileId(), root, initialSelections, null);
			if (request == null) {
				clearUpdatesAvailable();
				return;
			}
			final PlannerResolutionOperation operation = new PlannerResolutionOperation(AutomaticUpdateMessages.AutomaticUpdater_ResolutionOperationLabel, event.getProfileId(), request, null, new MultiStatus(AutomaticUpdatePlugin.PLUGIN_ID, 0, null, null), false);
			if ((operation.execute(new NullProgressMonitor())).isOK()) {
				// Download the items before notifying user if the
				// preference dictates.

				if (download) {
					Job job = ProvisioningOperationRunner.schedule(new ProfileModificationOperation(AutomaticUpdateMessages.AutomaticUpdater_AutomaticDownloadOperationName, event.getProfileId(), operation.getProvisioningPlan(), new ProvisioningContext(), new DownloadPhaseSet(), false), StatusManager.LOG);
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent jobEvent) {
							alreadyDownloaded = true;
							IStatus status = jobEvent.getResult();
							if (status.isOK()) {
								createUpdateAction(operation, root, initialSelections);
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										updateAction.suppressWizard(true);
										updateAction.run();
									}
								});
							} else if (status.getSeverity() != IStatus.CANCEL) {
								ProvUI.reportStatus(status, StatusManager.LOG);
							}
						}
					});
				} else {
					createUpdateAction(operation, root, initialSelections);
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							updateAction.suppressWizard(true);
							updateAction.run();
						}
					});
				}
			}
		} catch (ProvisionException e) {
			ProvUI.handleException(e, AutomaticUpdateMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
		}

	}

	/*
	 * Validate that iusToBeUpdated is valid, and reset the cache. If
	 * isKnownToBeAvailable is false, then recheck that the update is available.
	 * isKnownToBeAvailable should be false when the update list might be stale
	 * (Reminding the user of updates may happen long after the update check.
	 * This reduces the risk of notifying the user of updates and then not
	 * finding them .)
	 */

	void validateUpdates(IProgressMonitor monitor, boolean isKnownToBeAvailable) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < iusWithUpdates.length; i++) {
			try {
				if (isKnownToBeAvailable || ProvisioningUtil.getPlanner().updatesFor(iusWithUpdates[i], new ProvisioningContext(), monitor).length > 0) {
					if (validToUpdate(iusWithUpdates[i]))
						list.add(iusWithUpdates[i]);
				}
			} catch (ProvisionException e) {
				ProvUI.handleException(e, AutomaticUpdateMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
				continue;
			} catch (OperationCanceledException e) {
				// Nothing to report
			}
		}
		iusWithUpdates = (IInstallableUnit[]) list.toArray(new IInstallableUnit[list.size()]);
	}

	// A proposed update is valid if it is still visible to the user as an
	// installed item (it is a root)
	// and if it is not locked for updating.
	private boolean validToUpdate(IInstallableUnit iu) {
		int lock = IProfile.LOCK_NONE;
		boolean isRoot = false;
		try {
			IProfile profile = ProvisioningUtil.getProfile(profileId);
			String value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				lock = Integer.parseInt(value);
			value = profile.getInstallableUnitProperty(iu, IProfile.PROP_PROFILE_ROOT_IU);
			isRoot = value == null ? false : Boolean.valueOf(value).booleanValue();
		} catch (ProvisionException e) {
			// ignore
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

	void createUpdateAffordance() {
		updateAffordance = new StatusLineCLabelContribution(AUTO_UPDATE_STATUS_ITEM, 5);
		updateAffordance.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				launchUpdate();
			}
		});
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.add(updateAffordance);
			manager.update(true);
		}
	}

	void setUpdateAffordanceState(boolean isValid) {
		if (updateAffordance == null)
			return;
		if (isValid) {
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdates);
			updateAffordance.setImage(ProvUIImages.getImage(ProvUIImages.IMG_TOOL_UPDATE));
		} else {
			updateAffordance.setTooltip(AutomaticUpdateMessages.AutomaticUpdater_ClickToReviewUpdatesWithProblems);
			updateAffordance.setImage(ProvUIImages.getImage(ProvUIImages.IMG_TOOL_UPDATE_PROBLEMS));
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
		boolean shouldBeVisible = !ProvisioningOperationRunner.hasScheduledOperations();
		if (updateAffordance.isVisible() != shouldBeVisible) {
			IStatusLineManager manager = getStatusLineManager();
			if (manager != null) {
				updateAffordance.setVisible(shouldBeVisible);
				manager.update(true);
			}
		}
	}

	void createUpdatePopup() {
		popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, getPreferenceStore());
		popup.open();

	}

	void createUpdateAction(PlannerResolutionOperation operation, IUElementListRoot root, ArrayList initialSelections) {
		if (updateAction != null) {
			updateAction.dispose();
		}
		updateAction = new AutomaticUpdateAction(this, getSelectionProvider(), profileId, operation, root, initialSelections);
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
		alreadyValidated = false;
	}

	ISelectionProvider getSelectionProvider() {
		return new ISelectionProvider() {

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.jface.viewers.ISelectionProvider#
			 * addSelectionChangedListener
			 * (org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// Ignore because the selection won't change
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
			 */
			public ISelection getSelection() {
				return new StructuredSelection(iusWithUpdates);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @seeorg.eclipse.jface.viewers.ISelectionProvider#
			 * removeSelectionChangedListener
			 * (org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// ignore because the selection is static
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ISelectionProvider#setSelection(org
			 * .eclipse.jface.viewers.ISelection)
			 */
			public void setSelection(ISelection sel) {
				throw new UnsupportedOperationException("This ISelectionProvider is static, and cannot be modified."); //$NON-NLS-1$
			}
		};
	}

	public void launchUpdate() {
		alreadyValidated = true;
		updateAction.suppressWizard(false);
		updateAction.run();
	}

	private void registerProvisioningJobListener() {
		if (provisioningJobListener == null) {
			provisioningJobListener = new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					if (workbench == null || workbench.isClosing())
						return;
					if (workbench.getDisplay() == null)
						return;
					workbench.getDisplay().asyncExec(new Runnable() {
						public void run() {
							checkUpdateAffordanceEnablement();
						}
					});
				}

				public void scheduled(final IJobChangeEvent event) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					if (workbench == null || workbench.isClosing())
						return;
					if (workbench.getDisplay() == null)
						return;
					workbench.getDisplay().asyncExec(new Runnable() {
						public void run() {
							checkUpdateAffordanceEnablement();
						}
					});
				}
			};
			ProvisioningOperationRunner.addJobChangeListener(provisioningJobListener);
		}
	}

	/*
	 * The profile has changed. Make sure our toUpdate list is still valid and
	 * if there is nothing to update, get rid of the update popup and
	 * affordance.
	 */
	void validateUpdates() {
		Job validateJob = new Job("Update validate job") { //$NON-NLS-1$
			public IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				validateUpdates(monitor, false);
				// If there are no more updates, clear the indicators
				if (iusWithUpdates.length == 0) {
					if (PlatformUI.isWorkbenchRunning())
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								clearUpdatesAvailable();
							}
						});
				} else {
					// Run through the same update notification logic as before,
					// which will cause a new plan to be created against the
					// changed profile.
					updatesAvailable(new UpdateEvent(profileId, iusWithUpdates));
				}
				return Status.OK_STATUS;
			}
		};
		validateJob.setSystem(true);
		validateJob.setPriority(Job.LONG);
		validateJob.schedule();
	}

	public void shutdown() {
		if (updateAction != null)
			updateAction.dispose();
		if (provisioningJobListener != null) {
			ProvisioningOperationRunner.removeJobChangeListener(provisioningJobListener);
			provisioningJobListener = null;
		}
		statusLineManager = null;
		updateAction = null;
	}

	IPreferenceStore getPreferenceStore() {
		return AutomaticUpdatePlugin.getDefault().getPreferenceStore();
	}

}
