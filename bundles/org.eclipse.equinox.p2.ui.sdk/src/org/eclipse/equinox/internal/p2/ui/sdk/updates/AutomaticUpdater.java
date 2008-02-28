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
import org.eclipse.equinox.internal.provisional.p2.engine.ProfileEvent;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.UpdateAction;
import org.eclipse.equinox.internal.provisional.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.equinox.internal.provisional.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.*;
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
	 * @since 3.4
	 *
	 */
	final class AutomaticUpdateAction extends UpdateAction {
		ProvisioningPlan plan;

		AutomaticUpdateAction(ISelectionProvider selectionProvider, String profileId, IProfileChooser chooser, Policies policies, Shell shell) {
			super(selectionProvider, profileId, chooser, policies, shell);
		}

		public void initializePlan() {
			try {
				plan = getProvisioningPlan(toUpdate, profileId, new NullProgressMonitor());
			} catch (ProvisionException e) {
				// ignore
			}
		}

		protected ProvisioningPlan getProvisioningPlan() {
			if (plan != null)
				return plan;
			return super.getProvisioningPlan();
		}
	}

	Preferences prefs;
	StatusLineCLabelContribution updateAffordance;
	AutomaticUpdateAction updateAction;
	IStatusLineManager statusLineManager;
	IInstallableUnit[] updatesFound;
	IInstallableUnit[] toUpdate;
	String profileId;
	AutomaticUpdatesPopup popup;
	ProvisioningListener profileChangeListener;
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
		updatesFound = event.getIUs();
		alreadyDownloaded = false;
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
							alreadyDownloaded = true;
							IStatus status = jobEvent.getResult();
							if (status.isOK()) {
								createUpdateAction();
								updateAction.initializePlan();
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										updateAction.run();
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
				updateAction.initializePlan();
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						updateAction.run();
					}
				});
			}

		} catch (ProvisionException e) {
			ProvUI.handleException(e, ProvSDKMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
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
								ProvUI.handleException(e, ProvSDKMessages.AutomaticUpdater_ErrorCheckingUpdates, StatusManager.LOG);
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

	void createUpdatePopup() {
		popup = new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, prefs);
		popup.open();

	}

	void createUpdateAction() {
		if (updateAction == null)
			updateAction = new AutomaticUpdateAction(getSelectionProvider(), profileId, null, ProvPolicies.getDefault(), null);
	}

	IPlanValidator getPlanValidator() {
		return new IPlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				if (alreadyValidated)
					return true;
				// In all other cases we return false, because the clicking the popup will actually run the action.
				String openPlan = prefs.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN);
				if (plan != null) {
					if (plan.getStatus().isOK() || !(MessageDialogWithToggle.NEVER.equals(openPlan))) {
						// Show the affordance if user prefers always opening a plan or being prompted
						// In this context, the affordance is the prompt.
						if (updateAffordance == null)
							createUpdateAffordance();
						setUpdateAffordanceState(plan.getStatus().isOK());
						// If the user always wants to open invalid plans, then go ahead and show
						// the popup.
						if (MessageDialogWithToggle.ALWAYS.equals(openPlan) && popup == null)
							createUpdatePopup();
					} else {
						// The pref is NEVER, the user doesn't want to know about it
						ProvUI.reportStatus(plan.getStatus(), StatusManager.LOG);
					}
				}
				return false;
			}
		};
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
				return new StructuredSelection(toUpdate);
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
				else {
					createUpdateAction();
					updateAction.initializePlan();
					setUpdateAffordanceState(updateAction.getProvisioningPlan().getStatus().isOK());
				}
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
		IProvisioningEventBus bus = ProvSDKUIActivator.getDefault().getProvisioningEventBus();
		if (bus != null)
			bus.removeListener(profileChangeListener);
		profileChangeListener = null;
		statusLineManager = null;
	}

}
