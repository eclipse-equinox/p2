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
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Activator class for the p2 UI.
 */
public class ProvSDKUIActivator extends AbstractUIPlugin {

	private static final String LICENSE_STORAGE = "licenses.xml"; //$NON-NLS-1$
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
		getPreferenceStore().addPropertyChangeListener(getPreferenceListener());
	}

	private IPropertyChangeListener getPreferenceListener() {
		if (preferenceListener == null) {
			preferenceListener = new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					updateWithPreferences(getPolicy());
				}
			};
		}
		return preferenceListener;
	}

	public ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}

	private Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	private LicenseManager getLicenseManager() {
		return getProvisioningUI().getPolicy().getLicenseManager();
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
				StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_LicenseManagerReadError, e), StatusManager.LOG);
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
			StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_ErrorWritingLicenseRegistry, e), StatusManager.LOG);
		}
	}

	public void stop(BundleContext bundleContext) throws Exception {
		writeLicenseRegistry();
		plugin = null;
		getPreferenceStore().removePropertyChangeListener(preferenceListener);
		super.stop(bundleContext);
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		ServiceReference busReference = context.getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		if (busReference == null)
			return null;
		return (IProvisioningEventBus) context.getService(busReference);
	}

	static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}

	void updateWithPreferences(Policy policy) {
		IUViewQueryContext queryContext = policy.getQueryContext();
		RepositoryManipulator manipulator = policy.getRepositoryManipulator();
		queryContext.setShowLatestVersionsOnly(getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		queryContext.setVisibleAvailableIUProperty(IInstallableUnit.PROP_TYPE_GROUP);
		// If this ever changes, we must change AutomaticUpdateSchedule.getProfileQuery()
		queryContext.setVisibleInstalledIUProperty(IProfile.PROP_PROFILE_ROOT_IU);
		manipulator.setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		manipulator.setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
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
				StatusManager.getManager().handle(new Status(IStatus.ERROR, PLUGIN_ID, 0, ProvSDKMessages.ProvSDKUIActivator_ErrorSavingPrefs, e), StatusManager.LOG | StatusManager.SHOW);
			}
	}
}
