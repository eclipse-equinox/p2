/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.p2.ui.ValidationDialogServiceUI;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Activator class for the admin UI.
 */
public class ProvAdminUIActivator extends AbstractUIPlugin {

	private static ProvAdminUIActivator plugin;
	private static BundleContext context;

	public static final String PLUGIN_ID = "org.eclipse.equinox.internal.provisional.p2.ui.admin"; //$NON-NLS-1$
	public static final String PERSPECTIVE_ID = "org.eclipse.equinox.internal.provisional.p2.ui.admin.ProvisioningPerspective"; //$NON-NLS-1$

	private ServiceRegistration certificateUIRegistration;
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
		certificateUIRegistration = context.registerService(IServiceUI.class.getName(), new ValidationDialogServiceUI(), null);
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		certificateUIRegistration.unregister();
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
		IUViewQueryContext queryContext = policy.getQueryContext();
		RepositoryManipulator manipulator = policy.getRepositoryManipulator();
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY))
			queryContext.setVisibleAvailableIUProperty(IInstallableUnit.PROP_TYPE_GROUP);
		else
			queryContext.setVisibleAvailableIUProperty(null);
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY))
			queryContext.setVisibleInstalledIUProperty(IProfile.PROP_PROFILE_ROOT_IU);
		else
			queryContext.setVisibleInstalledIUProperty(null);

		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS)) {
			manipulator.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			manipulator.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		} else {
			manipulator.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
			manipulator.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		}
		queryContext.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS));
		queryContext.setUseCategories(getPreferenceStore().getBoolean(PreferenceConstants.PREF_USE_CATEGORIES));
	}

	void initializePolicy() {
		policy = new Policy() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				if (plan == null)
					return false;
				return true;
			}
		};
		// Manipulate the default query context according to our preferences
		IUViewQueryContext queryContext = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
		policy.setQueryContext(queryContext);
		updateForPreferences();
		policy.setRepositoryManipulator(new ColocatedRepositoryManipulator(null));
	}

	public Policy getPolicy() {
		return policy;
	}

	public ProvisioningUI getProvisioningUI(String profileId) {
		return new ProvisioningUI(ProvisioningUI.getDefaultUI().getSession(), profileId, policy);
	}
}
