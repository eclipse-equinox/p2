/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceInitializer;
import org.eclipse.equinox.internal.provisional.p2.core.IServiceUI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.*;

/**
 * Activator class for the p2 UI.
 */
public class ProvSDKUIActivator extends AbstractUIPlugin {

	public static final boolean ANY_PROFILE = false;
	private static final String DEFAULT_PROFILE_ID = "DefaultProfile"; //$NON-NLS-1$
	private static final String LICENSE_STORAGE = "licenses.xml"; //$NON-NLS-1$
	private static ProvSDKUIActivator plugin;
	private static BundleContext context;
	private ServiceRegistration certificateUIRegistration;
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
		PreferenceInitializer.migratePreferences();
		certificateUIRegistration = context.registerService(IServiceUI.class.getName(), new ValidationDialogServiceUI(), null);
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					updateWithPreferences(Policy.getDefault().getQueryContext());
				}
			};
		}
		return preferenceListener;
	}

	private void readLicenseRegistry() {
		IPath location = getStateLocation().append(LICENSE_STORAGE);
		File f = location.toFile();
		BufferedInputStream stream = null;
		if (f.exists()) {
			try {
				stream = new BufferedInputStream(new FileInputStream(f));
				Policy.getDefault().getLicenseManager().read(stream);
				stream.close();
			} catch (IOException e) {
				ProvUI.reportStatus(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_LicenseManagerReadError, e), StatusManager.LOG);
			}
		}
	}

	private void writeLicenseRegistry() {
		if (!Policy.getDefault().getLicenseManager().hasAcceptedLicenses())
			return;
		IPath location = getStateLocation().append(LICENSE_STORAGE);
		File f = location.toFile();
		BufferedOutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(f, false));
			Policy.getDefault().getLicenseManager().write(stream);
			stream.close();
		} catch (IOException e) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_ErrorWritingLicenseRegistry, e), StatusManager.LOG);
		}
	}

	public void stop(BundleContext bundleContext) throws Exception {
		writeLicenseRegistry();
		plugin = null;
		certificateUIRegistration.unregister();
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		ServiceReference busReference = context.getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		if (busReference == null)
			return null;
		return (IProvisioningEventBus) context.getService(busReference);
	}

	/**
	 * Get the id of the profile for the running system.  Throw a ProvisionException
	 * if no self profile is available, unless configured to answer any
	 * profile.  Getting any profile allows testing of the
	 * UI even when the system is not self hosting.  
	 */
	public static String getSelfProfileId() throws ProvisionException {
		// Get the profile of the running system.
		IProfile profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
		if (profile == null) {
			if (ANY_PROFILE) {
				ProvUI.reportStatus(getNoSelfProfileStatus(), StatusManager.LOG);
				IProfile[] profiles = ProvisioningUtil.getProfiles();
				if (profiles.length > 0)
					return profiles[0].getProfileId();
				return ProfileFactory.makeProfile(DEFAULT_PROFILE_ID).getProfileId();
			}
			throw new ProvisionException(getNoSelfProfileStatus());
		}
		return profile.getProfileId();
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	void updateWithPreferences(IUViewQueryContext queryContext) {
		queryContext.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		queryContext.setVisibleAvailableIUProperty(IInstallableUnit.PROP_TYPE_GROUP);
		// If this ever changes, we must change AutomaticUpdateSchedule.getProfileQuery()
		queryContext.setVisibleInstalledIUProperty(IInstallableUnit.PROP_PROFILE_ROOT_IU);
		queryContext.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		queryContext.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
	}

	/*
	 * Overridden to use a profile scoped preference store.
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
	 */
	public IPreferenceStore getPreferenceStore() {
		// Create the preference store lazily.
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore(new ProfileScope(IProfileRegistry.SELF), PLUGIN_ID);
		}
		return preferenceStore;
	}

	public void savePreferences() {
		if (preferenceStore != null)
			try {
				preferenceStore.save();
			} catch (IOException e) {
				ProvUI.handleException(e, ProvSDKMessages.ProvSDKUIActivator_ErrorSavingPrefs, StatusManager.LOG | StatusManager.SHOW);
			}
	}
}
