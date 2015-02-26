/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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

	private IPropertyChangeListener preferenceListener;

	Policy policy;

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
		initializePolicy();
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
		policy = null;
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					updateForPreferences();
				}
			};
		}
		return preferenceListener;
	}

	void updateForPreferences() {

		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY))
			policy.setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
		else
			policy.setVisibleAvailableIUQuery(QueryUtil.createIUAnyQuery());
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY))
			policy.setVisibleInstalledIUQuery(new UserVisibleRootQuery());
		else
			policy.setVisibleInstalledIUQuery(QueryUtil.createIUAnyQuery());

		RepositoryTracker tracker = getRepositoryTracker();
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS)) {
			tracker.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			tracker.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		} else {
			tracker.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
			tracker.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		}
		// store in ui prefs
		policy.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS));
		policy.setGroupByCategory(getPreferenceStore().getBoolean(PreferenceConstants.PREF_USE_CATEGORIES));
	}

	private RepositoryTracker getRepositoryTracker() {
		return ProvisioningUI.getDefaultUI().getRepositoryTracker();
	}

	void initializePolicy() {
		policy = new Policy();
		// Manipulate the default query context according to our preferences
		updateForPreferences();
	}

	public Policy getPolicy() {
		return policy;
	}

	public IProfileRegistry getProfileRegistry() {
		IProvisioningAgent agent = ServiceHelper.getService(context, IProvisioningAgent.class);
		return (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	public ProvisioningUI getProvisioningUI(String profileId) {
		return new ProvisioningUI(ProvisioningUI.getDefaultUI().getSession(), profileId, policy);
	}
}
