/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Ericsson AB (Hamdan Msheik) - Bug 396420 - Control Install dialog through preference customization
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceInitializer;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Activator class for the p2 UI.
 */
public class ProvSDKUIActivator extends AbstractUIPlugin {

	private static ProvSDKUIActivator plugin;
	private static BundleContext context;
	private ScopedPreferenceStore preferenceStore;

	private IPropertyChangeListener preferenceListener;

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

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		ProvSDKUIActivator.context = bundleContext;
		PreferenceInitializer.migratePreferences();
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = event -> updateWithPreferences(getPolicy());
		}
		return preferenceListener;
	}

	public ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	public IProvisioningAgent getProvisioningAgent() {
		return getProvisioningUI().getSession().getProvisioningAgent();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	void updateWithPreferences(Policy policy) {

		IPreferenceStore store = getPreferenceStore();

		String value = store.getString(PreferenceConstants.PREF_SHOW_LATEST_VERSION);
		policy.setShowLatestVersionsOnly(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value) ? store.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION) : policy.getShowLatestVersionsOnly());

		value = store.getString(PreferenceConstants.PREF_HIDE_INSTALLED);
		policy.setHideAlreadyInstalled(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value) ? store.getBoolean(PreferenceConstants.PREF_HIDE_INSTALLED) : policy.getHideAlreadyInstalled());

		value = store.getString(PreferenceConstants.PREF_FILTER_ON_ENV);
		policy.setFilterOnEnv(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value) ? store.getBoolean(PreferenceConstants.PREF_FILTER_ON_ENV) : policy.getFilterOnEnv());

		value = store.getString(PreferenceConstants.PREF_CONTACT_ALL_SITES);
		policy.setContactAllSites(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value) ? store.getBoolean(PreferenceConstants.PREF_CONTACT_ALL_SITES) : policy.getContactAllSites());

		value = store.getString(PreferenceConstants.PREF_GROUP_BY_CATEGORY);
		policy.setGroupByCategory(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value) ? store.getBoolean(PreferenceConstants.PREF_GROUP_BY_CATEGORY) : policy.getGroupByCategory());

		value = store.getString(PreferenceConstants.PREF_CHECK_AGAINST_CURRENT_JRE);
		policy.setCheckAgainstCurrentExecutionEnvironment(!IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value)
				? store.getBoolean(PreferenceConstants.PREF_CHECK_AGAINST_CURRENT_JRE)
				: policy.getCheckAgainstCurrentExecutionEnvironment());
	}

	/*
	 * Overridden to use a profile scoped preference store.
	 */
	@Override
	public IPreferenceStore getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			final IAgentLocation agentLocation = getAgentLocation();
			if (agentLocation == null) {
				return super.getPreferenceStore();
			}
			preferenceStore = new ScopedPreferenceStore(new ProfileScope(agentLocation, IProfileRegistry.SELF), PLUGIN_ID);
		}
		return preferenceStore;
	}

	private IAgentLocation getAgentLocation() {
		ServiceReference<IAgentLocation> ref = getContext().getServiceReference(IAgentLocation.class);
		if (ref == null) {
			return null;
		}
		IAgentLocation location = getContext().getService(ref);
		getContext().ungetService(ref);
		return location;
	}

	public void savePreferences() {
		if (preferenceStore != null) {
			try {
				preferenceStore.save();
			} catch (IOException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_ErrorSavingPrefs, e), StatusManager.LOG | StatusManager.SHOW);
			}
		}
	}
}
