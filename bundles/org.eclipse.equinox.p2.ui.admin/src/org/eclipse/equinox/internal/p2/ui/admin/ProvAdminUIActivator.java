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

import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddProfileDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ValidationDialogServiceUI;
import org.eclipse.equinox.internal.provisional.p2.ui.model.Profiles;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.ProvElementLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
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
					updateForPreferences(getPolicy().getQueryContext());
				}
			};
		}
		return preferenceListener;
	}

	void updateForPreferences(IUViewQueryContext queryContext) {
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_GROUPS_ONLY))
			queryContext.setVisibleAvailableIUProperty(IInstallableUnit.PROP_TYPE_GROUP);
		else
			queryContext.setVisibleAvailableIUProperty(null);
		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY))
			queryContext.setVisibleInstalledIUProperty(IInstallableUnit.PROP_PROFILE_ROOT_IU);
		else
			queryContext.setVisibleInstalledIUProperty(null);

		if (getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS)) {
			queryContext.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			queryContext.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		} else {
			queryContext.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
			queryContext.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_ALL);
		}
		queryContext.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS));
		queryContext.setUseCategories(getPreferenceStore().getBoolean(PreferenceConstants.PREF_USE_CATEGORIES));
	}

	void initializePolicy() {
		policy = new Policy();
		// Manipulate the default query context according to our preferences
		IUViewQueryContext queryContext = new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO);
		policy.setQueryContext(queryContext);
		updateForPreferences(queryContext);
		policy.setPlanValidator(new PlanValidator() {
			public boolean continueWorkingWithPlan(ProvisioningPlan plan, Shell shell) {
				if (plan == null)
					return false;
				return true;
			}
		});
		policy.setProfileChooser(new ProfileChooser() {
			public String getProfileId(Shell shell) {
				// TODO would be nice if the profile chooser dialog let you
				// create a new profile
				ProvElementContentProvider provider = new ProvElementContentProvider();
				if (provider.getElements(new Profiles(getPolicy())).length == 0) {
					AddProfileDialog dialog = new AddProfileDialog(shell, new String[0]);
					if (dialog.open() == Window.OK) {
						return dialog.getAddedProfileId();
					}
					return null;
				}

				ListDialog dialog = new ListDialog(shell);
				dialog.setTitle(ProvAdminUIMessages.MetadataRepositoriesView_ChooseProfileDialogTitle);
				dialog.setLabelProvider(new ProvElementLabelProvider());
				dialog.setInput(new Profiles(getPolicy()));
				dialog.setContentProvider(provider);
				dialog.open();
				Object[] result = dialog.getResult();
				if (result != null && result.length > 0) {
					IProfile profile = (IProfile) ProvUI.getAdapter(result[0], IProfile.class);
					if (profile != null)
						return profile.getProfileId();
				}
				return null;
			}
		});
	}

	public Policy getPolicy() {
		return policy;
	}
}
