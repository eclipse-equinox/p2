/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Activator class for the automatic updates plugin. The automatic updates plugin
 * is responsible for scheduling background update checks, based on update settings
 * specified by end user settings. The implementation of update checking is provided
 * by another plugin that provides an IUpdateChecker implementation.
 */
public class AutomaticUpdatePlugin extends AbstractUIPlugin {

	// bundle-relative icon path
	public final static String ICON_PATH = "$nl$/icons/"; //$NON-NLS-1$

	// tool icons
	public final static String IMG_TOOL_UPDATE = "tool/update.gif"; //$NON-NLS-1$
	public final static String IMG_TOOL_UPDATE_PROBLEMS = "tool/update_problems.gif"; //$NON-NLS-1$
	public final static String IMG_TOOL_CLOSE = "tool/close.gif"; //$NON-NLS-1$
	public final static String IMG_TOOL_CLOSE_HOT = "tool/close_hot.gif"; //$NON-NLS-1$

	private static AutomaticUpdatePlugin plugin;
	private static BundleContext context;

	private AutomaticUpdateScheduler scheduler;
	private AutomaticUpdater updater;
	private ScopedPreferenceStore preferenceStore;

	private ProvisioningSession session;

	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui.sdk.scheduler"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static AutomaticUpdatePlugin getDefault() {
		return plugin;
	}

	public AutomaticUpdatePlugin() {
		// constructor
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		context = bundleContext;
		IProvisioningAgent agent = ServiceHelper.getService(getContext(), IProvisioningAgent.class);
		session = new ProvisioningSession(agent);

		PreferenceInitializer.migratePreferences();
	}

	public void stop(BundleContext bundleContext) throws Exception {
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
		context = null;
	}

	public AutomaticUpdateScheduler getScheduler() {
		// If the scheduler was disabled, it does not get initialized
		if (scheduler == null)
			scheduler = new AutomaticUpdateScheduler();
		return scheduler;
	}

	public AutomaticUpdater getAutomaticUpdater() {
		if (updater == null)
			updater = new AutomaticUpdater();
		return updater;
	}

	void setScheduler(AutomaticUpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		ServiceReference<IProvisioningEventBus> busReference = context.getServiceReference(IProvisioningEventBus.class);
		if (busReference == null)
			return null;
		return context.getService(busReference);
	}

	/*
	 * Overridden to use a profile scoped preference store. (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			final IAgentLocation agentLocation = getAgentLocation();
			if (agentLocation == null)
				return super.getPreferenceStore();
			preferenceStore = new ScopedPreferenceStore(new ProfileScope(agentLocation, IProfileRegistry.SELF), PLUGIN_ID);
		}
		return preferenceStore;
	}

	public IAgentLocation getAgentLocation() {
		ServiceReference<IAgentLocation> ref = getContext().getServiceReference(IAgentLocation.class);
		if (ref == null)
			return null;
		IAgentLocation location = getContext().getService(ref);
		getContext().ungetService(ref);
		return location;
	}

	public void savePreferences() {
		if (preferenceStore != null)
			try {
				preferenceStore.save();
			} catch (IOException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, AutomaticUpdatePlugin.PLUGIN_ID, 0, AutomaticUpdateMessages.ErrorSavingPreferences, e), StatusManager.LOG | StatusManager.SHOW);
			}
	}

	protected void initializeImageRegistry(ImageRegistry reg) {
		createImageDescriptor(IMG_TOOL_UPDATE, reg);
		createImageDescriptor(IMG_TOOL_UPDATE_PROBLEMS, reg);
		createImageDescriptor(IMG_TOOL_CLOSE, reg);
		createImageDescriptor(IMG_TOOL_CLOSE_HOT, reg);
	}

	/**
	 * Creates the specified image descriptor and registers it
	 */
	private void createImageDescriptor(String id, ImageRegistry reg) {
		URL url = FileLocator.find(getBundle(), new Path(ICON_PATH).append(id), null);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		reg.put(id, desc);
	}

	public ProvisioningSession getSession() {
		return session;
	}

	public IProvisioningAgentProvider getAgentProvider() {
		ServiceReference<IProvisioningAgentProvider> ref = getContext().getServiceReference(IProvisioningAgentProvider.class);
		if (ref == null)
			return null;
		IProvisioningAgentProvider agentProvider = getContext().getService(ref);
		getContext().ungetService(ref);
		return agentProvider;
	}
}
