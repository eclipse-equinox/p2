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
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.sdk.updates.AutomaticUpdater;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IPlanValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Activator class for the p2 UI.
 */
public class ProvSDKUIActivator extends AbstractUIPlugin {

	private static final String DEFAULT_PROFILE_ID = "DefaultProfile"; //$NON-NLS-1$
	private static final String LICENSE_STORAGE = "licenses.xml"; //$NON-NLS-1$
	private static ProvSDKUIActivator plugin;
	private static BundleContext context;
	private AutomaticUpdateScheduler scheduler;
	private AutomaticUpdater updater;
	private IQueryProvider queryProvider;
	private SimpleLicenseManager licenseManager;
	private IPlanValidator planValidator;

	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui.sdk"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static ProvSDKUIActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public ProvSDKUIActivator() {
		// constructor
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		ProvSDKUIActivator.context = bundleContext;
		readLicenseRegistry();
	}

	private void readLicenseRegistry() {
		IPath location = getStateLocation().append(LICENSE_STORAGE);
		File f = location.toFile();
		BufferedInputStream stream = null;
		if (f.exists()) {
			try {
				stream = new BufferedInputStream(new FileInputStream(f));
				getLicenseManager().read(stream);
				stream.close();
			} catch (IOException e) {
				ProvUI.reportStatus(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_LicenseManagerReadError, e), StatusManager.LOG);
			}
		}
	}

	private void writeLicenseRegistry() {
		if (!getLicenseManager().hasAcceptedLicenses())
			return;
		IPath location = getStateLocation().append(LICENSE_STORAGE);
		File f = location.toFile();
		BufferedOutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(f, false));
			getLicenseManager().write(stream);
			stream.close();
		} catch (IOException e) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_ErrorWritingLicenseRegistry, e), StatusManager.LOG);
		}
	}

	public void stop(BundleContext bundleContext) throws Exception {
		writeLicenseRegistry();
		if (scheduler != null) {
			scheduler.shutdown();
			scheduler = null;
		}
		if (updater != null) {
			updater.shutdown();
			updater = null;
		}
		plugin = null;
		super.stop(bundleContext);
	}

	public AutomaticUpdateScheduler getScheduler() {
		// If the scheduler was disabled, it does not get initialized
		if (scheduler == null)
			scheduler = new AutomaticUpdateScheduler();
		return scheduler;
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		ServiceReference busReference = context.getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		if (busReference == null)
			return null;
		return (IProvisioningEventBus) context.getService(busReference);
	}

	public AutomaticUpdater getAutomaticUpdater() {
		if (updater == null)
			updater = new AutomaticUpdater();
		return updater;
	}

	/**
	 * Get the id of the profile for the running system.  If not available, get
	 * any available profile.  Getting any profile allows testing of the
	 * UI even when the system is not self hosting.  Error reporting is
	 * left to the client, who must check for a null return.
	 */
	public static String getProfileId() throws ProvisionException {
		// Get the profile of the running system.
		IProfile profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
		if (profile == null) {
			ProvUI.reportStatus(getNoSelfProfileStatus(), StatusManager.LOG);
			IProfile[] profiles = ProvisioningUtil.getProfiles();
			if (profiles.length > 0)
				return profiles[0].getProfileId();
			return ProfileFactory.makeProfile(DEFAULT_PROFILE_ID).getProfileId();
		}
		return profile.getProfileId();
	}

	void setScheduler(AutomaticUpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	public IQueryProvider getQueryProvider() {
		if (queryProvider == null)
			queryProvider = new ProvSDKQueryProvider();
		return queryProvider;
	}

	public SimpleLicenseManager getLicenseManager() {
		if (licenseManager == null)
			licenseManager = new SimpleLicenseManager();
		return licenseManager;
	}

	public IPlanValidator getPlanValidator() {
		if (planValidator == null)
			planValidator = new IPlanValidator() {
				public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
					if (plan == null)
						return false;
					// If the plan requires install handler support, we want to open the old update UI
					if (UpdateManagerCompatibility.requiresInstallHandlerSupport(plan)) {
						MessageDialog dialog = new MessageDialog(shell, ProvSDKMessages.ProvSDKUIActivator_UnsupportedFeatureTitle, null, ProvSDKMessages.ProvSDKUIActivator_UnsupportedFeatureMessage, MessageDialog.WARNING, new String[] {ProvSDKMessages.ProvSDKUIActivator_LaunchUpdateManager, IDialogConstants.CANCEL_LABEL}, 0);
						if (dialog.open() == 0)
							BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
								public void run() {
									UpdateManagerCompatibility.openInstaller();
								}
							});
						return false;
					}
					if (plan.getStatus().isOK())
						return true;

					// Special case those statuses where we would never want to open a wizard
					if (plan.getStatus().getCode() == IStatusCodes.NOTHING_TO_UPDATE) {
						ProvUI.reportStatus(plan.getStatus(), StatusManager.BLOCK);
						return false;
					}

					String openPlan = getPreferenceStore().getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN);
					if (MessageDialogWithToggle.ALWAYS.equals(openPlan)) {
						return true;
					}
					if (MessageDialogWithToggle.NEVER.equals(openPlan)) {
						ProvUI.reportStatus(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
						return false;
					}
					MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, ProvSDKMessages.ProvSDKUIActivator_OpenWizardAnyway, null, false, getPreferenceStore(), PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN);
					// Any answer but yes will stop the performance of the plan, but NO is interpreted to mean, show me the error.
					if (dialog.getReturnCode() == IDialogConstants.NO_ID)
						ProvUI.reportStatus(plan.getStatus(), StatusManager.SHOW | StatusManager.LOG);
					return dialog.getReturnCode() == IDialogConstants.YES_ID;
				}

			};
		return planValidator;
	}
}
