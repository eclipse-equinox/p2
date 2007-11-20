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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProfileFactory;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.query.IProvElementQueryProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * Activator class for the admin UI.
 */
public class ProvSDKUIActivator extends AbstractUIPlugin {

	private static final String DEFAULT_PROFILE_ID = "DefaultProfile"; //$NON-NLS-1$
	private static ProvSDKUIActivator plugin;
	private static BundleContext context;
	private AutomaticUpdateScheduler scheduler;
	private IProvElementQueryProvider queryProvider;

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
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		if (scheduler != null) {
			scheduler.shutdown();
			scheduler = null;
		}
		super.stop(bundleContext);
	}

	public AutomaticUpdateScheduler getScheduler() {
		// If the scheduler was disabled, it does not get initialized
		if (scheduler == null)
			scheduler = new AutomaticUpdateScheduler();
		return scheduler;
	}

	/**
	 * Get a profile for the running system.  If not available, get
	 * any available profile.  Getting any profile allows testing of the
	 * UI even when the system is not self hosting.  Error reporting is
	 * left to the client, who must check for a null return.
	 */
	public static Profile getAnyProfile() throws ProvisionException {
		Profile profile = null;
		// Get the profile of the running system.
		profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
		if (profile == null) {
			StatusManager.getManager().handle(getNoSelfProfileStatus(), StatusManager.LOG);
			Profile[] profiles = ProvisioningUtil.getProfiles();
			if (profiles.length > 0)
				return profiles[0];
			return ProfileFactory.makeProfile(DEFAULT_PROFILE_ID);

		}
		return profile;
	}

	void setScheduler(AutomaticUpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	public IProvElementQueryProvider getQueryProvider() {
		if (queryProvider == null)
			queryProvider = new ProvSDKQueryProvider();
		return queryProvider;
	}
}
