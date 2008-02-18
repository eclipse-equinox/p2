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
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.ui.SimpleLicenseManager;
import org.eclipse.equinox.internal.provisional.p2.ui.UpdateManagerCompatibility;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Activator class for the admin UI.
 */
public class ProvAdminUIActivator extends AbstractUIPlugin {

	private static ProvAdminUIActivator plugin;
	private static BundleContext context;

	public static final String PLUGIN_ID = "org.eclipse.equinox.internal.provisional.p2.ui.admin"; //$NON-NLS-1$
	public static final String PERSPECTIVE_ID = "org.eclipse.equinox.internal.provisional.p2.ui.admin.ProvisioningPerspective"; //$NON-NLS-1$

	private IQueryProvider queryProvider;
	private LicenseManager licenseManager;
	private IPlanValidator planValidator;

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static ProvAdminUIActivator getDefault() {
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

	public ProvAdminUIActivator() {
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
		ProvAdminUIActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		super.stop(bundleContext);
	}

	public IQueryProvider getQueryProvider() {
		if (queryProvider == null)
			queryProvider = new ProvAdminQueryProvider();
		return queryProvider;
	}

	public LicenseManager getLicenseManager() {
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
						MessageDialog dialog = new MessageDialog(shell, ProvAdminUIMessages.ProvAdminUIActivator_UnsupportedInstallHandler, null, ProvAdminUIMessages.ProvAdminUIActivator_UnsupportedInstallHandlerMessage, MessageDialog.WARNING, new String[] {ProvAdminUIMessages.ProvAdminUIActivator_LaunchUpdateManager, ProvAdminUIMessages.ProvAdminUIActivator_ContinueAnyway, IDialogConstants.CANCEL_LABEL}, 0);
						int ret = dialog.open();
						if (ret == 1) // continue anyway
							return true;
						if (ret == 0)
							UpdateManagerCompatibility.openInstaller();
						return false;
					}
					return true;
				}
			};
		return planValidator;
	}
}
