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
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.sdk.*;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.*;
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

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	Preferences prefs;
	StatusLineCLabelContribution updateAffordance;
	IInstallableUnit[] toUpdate;
	String profileId;
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
		// Recompute the updates that we want to make available to the user.
		toUpdate = getUpdatesToShow(event);

		profileId = event.getProfileId();
		if (toUpdate.length <= 0)
			return;

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
	private IInstallableUnit[] getUpdatesToShow(final UpdateEvent event) {
		// We could simply collect the install roots ourselves, but implementing
		// this in terms of a normal "what's installed" query allows the policy to be defined only
		// in one place.
		IQueryable rootQueryable = new IQueryable() {
			public Collector query(Query query, Collector result, IProgressMonitor monitor) {
				IInstallableUnit[] ius = event.getIUs();
				for (int i = 0; i < ius.length; i++)
					if (query.isMatch(ius[i]))
						result.accept(ius[i]);
				return result;
			}
		};
		ProfileElement element = new ProfileElement(event.getProfileId());
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
		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow == null)
			return null;
		// YUCK!  YUCK!  YUCK!
		// IWorkbenchWindow does not define getStatusLineManager(), yet WorkbenchWindow does
		try {
			Method method = activeWindow.getClass().getDeclaredMethod("getStatusLineManager", new Class[0]); //$NON-NLS-1$
			try {
				Object statusLine = method.invoke(activeWindow, new Object[0]);
				if (statusLine instanceof IStatusLineManager)
					return (IStatusLineManager) statusLine;
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
			return ((IViewSite) site).getActionBars().getStatusLineManager();
		} else if (site instanceof IEditorSite) {
			return ((IEditorSite) site).getActionBars().getStatusLineManager();
		}
		return null;
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

	void showUpdatesAvailable(boolean alreadyDownloaded) {
		if (updateAffordance == null)
			createUpdateAffordance();
		new AutomaticUpdatesPopup(getWorkbenchWindowShell(), alreadyDownloaded, prefs).open();
	}

	void clearUpdatesAvailable() {
		IStatusLineManager manager = getStatusLineManager();
		if (manager != null) {
			manager.remove(updateAffordance);
			manager.update(true);
		}
		updateAffordance.dispose();
		updateAffordance = null;

	}

	public void launchUpdate() {
		UpdateWizard wizard = new UpdateWizard(profileId, toUpdate, ProvSDKUIActivator.getDefault().getLicenseManager(), ProvSDKUIActivator.getDefault().getQueryProvider());
		WizardDialog dialog = new WizardDialog(getWorkbenchWindowShell(), wizard);
		if (dialog.open() == Window.OK)
			clearUpdatesAvailable();
	}

}
