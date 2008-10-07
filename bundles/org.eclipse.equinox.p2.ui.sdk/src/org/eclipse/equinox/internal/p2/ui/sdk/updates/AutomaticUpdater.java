/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.EventObject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.sdk.*;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Updates;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.PlanValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	/**
	 * Overridden so that we can use the profile change request computations,
	 * but we hide the resolution from the user and optionally suppress the
	 * wizard if we are resolving for reasons other than user request.
	 * 
	 * @since 3.4
	 *
	 */
	final class AutomaticUpdateAction extends UpdateAction {

		private boolean suppressWizard = false;

		AutomaticUpdateAction(ISelectionProvider selectionProvider, String profileId) {
			super(Policy.getDefault(), selectionProvider, profileId, false);
		}

		void suppressWizard(boolean suppress) {
			suppressWizard = suppress;
		}

		protected int performAction(IInstallableUnit[] ius, String targetProfileId, ProvisioningPlan plan) {
			if (suppressWizard) {
				setUpdateAffordanceState(plan != null && plan.getStatus().isOK());
				return Window.OK;
			}
			return super.performAction(ius, targetProfileId, plan);
		}

		protected PlanValidator getPlanValidator() {
			return new PlanValidator() {
				public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
					if (alreadyValidated)
						return true;
					// In all other cases we return false, because clicking the popup will actually run the action.
					// We are just checking prefs to determine whether to to show the popup or not.
					String openPlan = prefs.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN);
					if (plan != null) {
						// If the user cancelled the operation, don't continue
						if (plan.getStatus().getSeverity() == IStatus.CANCEL)
							return false;
						boolean noError = plan.getStatus().getSeverity() != IStatus.ERROR;
						if (noError || !(MessageDialogWithToggle.NEVER.equals(openPlan))) {
							// Show the affordance if user prefers always opening a currentPlan or being prompted
							// In this context, the affordance is the prompt.
							if (updateAffordance == null)
								createUpdateAffordance();
							setUpdateAffordanceState(noError);
							// If the user always wants to open invalid plans, or there is no error then go ahead and show
							// the popup.
							if ((noError || MessageDialogWithToggle.ALWAYS.equals(openPlan)) && popup == null)
								createUpdatePopup();
						} else {
							// There is an error and the pref is NEVER, the user doesn't want to know about it
							ProvUI.reportStatus(plan.getStatus(), StatusManager.LOG);
						}
					}
					return false;
				}
			};
		}
	}

	Preferences prefs;
	StatusLineCLabelContribution updateAffordance;
	AutomaticUpdateAction updateAction;
	IStatusLineManager statusLineManager;
	IInstallableUnit[] iusWithUpdates;
	String profileId;
	AutomaticUpdatesPopup popup;
	ProvisioningListener profileChangeListener;
	IJobChangeListener provisioningJobListener;
	boolean alreadyValidated = false;
	boolean alreadyDownloaded = false;
	private static final String AUTO_UPDATE_STATUS_ITEM = "AutoUpdatesStatus"; //$NON-NLS-1$

	public AutomaticUpdater() {
		prefs = ProvSDKUIActivator.getDefault().getPluginPreferences();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener#updatesAvailable(org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent)
	 */
	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = prefs.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		profileId = event.getProfileId();
		iusWithUpdates = event.getIUs();
		validateUpdates(null, true);
		alreadyDownloaded = false;

		if (iusWithUpdates.length <= 0) {
			clearUpdatesAvailable();
			return;
		}
		registerProfileChangeListener();
		registerProvisioningJobListener();

		// Download the items if the preference dictates before
		// showing the user that updates are available.
		try {
			if (download) {
				ElementQueryDescriptor descriptor = Policy.getDefault().getQueryProvider().getQueryDescriptor(new Updates(event.getProfileId(), event.getIUs()));
				IInstallableUnit[] replacements = (IInstallableUnit[]) descriptor.queryable.query(descriptor.query, descriptor.collector, null).toArray(IInstallableUnit.class);
				if (replacements.length > 0) {
					ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(event.getProfileId());
					request.removeInstallableUnits(iusWithUpdates);
					request.addInstallableUnits(replacements);
					final ProvisioningPlan plan = ProvisioningUtil.getPlanner().getProvisioningPlan(request, new ProvisioningContext(), null);
					Job job = ProvisioningOperationRunner.schedule(new ProfileModificationOperation(ProvSDKMessages.AutomaticUpdater_AutomaticDownloadOperationName, event.getProfileId(), plan, new DownloadPhaseSet(), false), null, StatusManager.LOG);
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent jobEvent) {
							alreadyDownloaded = true;
							IStatus status = jobEvent.getResult();
							if (status.isOK()) {
								createUpdateAction();
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										updateAction.suppressWizard(true);
										updateAction.performAction(iusWithUpdates, event.getProfileId(), plan);
									}
								});
							} else if (status.getSeverity() != IStatus.CANCEL) {
								ProvUI.reportStatus(status, StatusManager.LOG);
							}
						}
					});
				}
			} else {
				createUpdateAction();
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						updateAction.suppressWizard(true);
						updateAction.run();
					}
				});
			}

		} catch (ProvisionException e) {
			ProvUI.handleException(e, ProvSDKMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
		}

	}

	/*
	 * Validate that iusToBeUpdated is valid, and reset the cache.  
	 * If isKnownToBeAvailable is false, then recheck that the update is
	 * available.  isKnownToBeAvailable should be false when the update list 
	 * might be stale (Reminding the user of updates may happen long
	 * after the update check.  This reduces the risk of notifying the user
	 * of updates and then not finding them .)
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
				ProvUI.handleException(e, ProvSDKMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
				continue;
			}
		}
		iusWithUpdates = (IInstallableUnit[]) list.toArray(new IInstallableUnit[list.size()]);
	}

	// A proposed update is valid if it is still visible to the user as an installed item (it is a root)
	// and if it is not locked for updating.
	private boolean validToUpdate(IInstallableUnit iu) {
		int lock = IInstallableUnit.LOCK_NONE;
		boolean isRoot = false;
		try {
			IProfile profile = ProvisioningUtil.getProfile(profileId);
			String value = profile.getInstallableUnitProperty(iu, IInstallableUnit.PROP_PROFILE_LOCKED_IU);
			if (value != null)
				lock = Integer.parseInt(value);
			value = profile.getInstallableUnitProperty(iu, IInstallableUnit.PROP_PROFILE_ROOT_IU);
			isRoot = value == null ? false : Boolean.valueOf(value).booleanValue();
		} catch (ProvisionException e) {
			// ignore
		} catch (NumberFormatException e) {
			// ignore and assume no lock
		}
		return isRoot && (lock & IInstallableUnit.LOCK_UPDATE) == 0;
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
			updateAffordance.setTooltip(ProvSDKMessages.AutomaticUpdater_ClickToReviewUpdates);
			updateAffordance.setImage(ProvUIImages.getImage(ProvUIImages.IMG_TOOL_UPDATE));
		} else {
			updateAffordance.setTooltip(ProvSDKMessages.AutomaticUpdater_ClickToReviewUpdatesWithProblems);
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
		popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, prefs);
		popup.open();

	}

	void createUpdateAction() {
		if (updateAction == null)
			updateAction = new AutomaticUpdateAction(getSelectionProvider(), profileId);
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

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// Ignore because the selection won't change 
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
			 */
			public ISelection getSelection() {
				return new StructuredSelection(iusWithUpdates);
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// ignore because the selection is static
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
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
			IProvisioningEventBus bus = ProvSDKUIActivator.getDefault().getProvisioningEventBus();
			if (bus != null)
				bus.addListener(profileChangeListener);
		}
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
	 * The profile has changed.  Make sure our toUpdate list is
	 * still valid and if there is nothing to update, get rid
	 * of the update popup and affordance.
	 */
	void validateUpdates() {
		Job validateJob = new WorkbenchJob("Update validate job") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				validateUpdates(monitor, false);
				if (iusWithUpdates.length == 0)
					clearUpdatesAvailable();
				else {
					createUpdateAction();
					updateAction.suppressWizard(true);
					updateAction.run();
				}
				return Status.OK_STATUS;
			}
		};
		validateJob.setSystem(true);
		validateJob.setPriority(Job.SHORT);
		validateJob.schedule();
	}

	public void shutdown() {
		if (provisioningJobListener != null) {
			ProvisioningOperationRunner.removeJobChangeListener(provisioningJobListener);
			provisioningJobListener = null;
		}
		if (profileChangeListener == null)
			return;
		IProvisioningEventBus bus = ProvSDKUIActivator.getDefault().getProvisioningEventBus();
		if (bus != null)
			bus.removeListener(profileChangeListener);
		profileChangeListener = null;
		statusLineManager = null;
	}

}
