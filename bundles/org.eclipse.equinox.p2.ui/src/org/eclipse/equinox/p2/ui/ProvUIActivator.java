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
package org.eclipse.equinox.p2.ui;

import java.net.URL;
import java.util.EventObject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.ui.ProvisioningEventManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Controls the lifecycle of the provisioning UI bundle
 * 
 * @since 3.4
 */
public class ProvUIActivator extends AbstractUIPlugin {
	private static BundleContext context;
	private static PackageAdmin packageAdmin = null;
	private static ServiceReference packageAdminRef = null;
	private static ProvUIActivator plugin;
	private ProvisioningListener profileChangeListener;
	private ProvisioningEventManager eventManager = new ProvisioningEventManager();

	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static ProvUIActivator getDefault() {
		return plugin;
	}

	public static Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		// Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	public ProvUIActivator() {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);

		plugin = this;
		ProvUIActivator.context = bundleContext;
		packageAdminRef = bundleContext.getServiceReference(PackageAdmin.class.getName());
		packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminRef);

		// TODO for now we need to manually start up the provisioning infrastructure
		// because the Eclipse Application launch config won't let me specify bundles to start.
		getBundle("org.eclipse.equinox.p2.exemplarysetup").start(Bundle.START_ACTIVATION_POLICY); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.frameworkadmin.equinox").start(Bundle.START_ACTIVATION_POLICY); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.simpleconfigurator.manipulator").start(Bundle.START_ACTIVATION_POLICY); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.p2.updatechecker").start(Bundle.START_ACTIVATION_POLICY); //$NON-NLS-1$

		initializeImages();
		addProfileChangeListener();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		try {
			removeProfileChangeListener();
			plugin = null;
			ProvUIActivator.context = null;
		} finally {
			super.stop(bundleContext);
		}
	}

	private void addProfileChangeListener() {
		if (profileChangeListener == null) {
			profileChangeListener = new ProvisioningListener() {
				public void notify(EventObject o) {
					if (o instanceof ProfileEvent) {
						ProfileEvent event = (ProfileEvent) o;
						try {
							Profile selfProfile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
							if (selfProfile != null && (selfProfile.getProfileId().equals(event.getProfile().getProfileId()))) {
								if (event.getReason() == ProfileEvent.CHANGED)
									PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
										public void run() {
											ProvUI.requestRestart(false, null);
										}
									});
							}
						} catch (ProvisionException e) {
							ProvUI.handleException(e, null);
						}
					}
				}
			};
		}
		ProvisioningEventBus bus = getProvisioningEventBus();
		if (bus != null)
			bus.addListener(profileChangeListener);
	}

	private void removeProfileChangeListener() {
		if (profileChangeListener != null) {
			ProvisioningEventBus bus = getProvisioningEventBus();
			if (bus != null)
				bus.removeListener(profileChangeListener);
		}
	}

	public void addProvisioningListener(StructuredViewerProvisioningListener listener) {
		// TODO hack for unsupported repository events.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197052
		if ((listener.getEventTypes() & StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) == StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) {
			eventManager.addListener(listener);
		} else {
			getProvisioningEventBus().addListener(listener);
		}
	}

	private ProvisioningEventBus getProvisioningEventBus() {
		ServiceReference busReference = context.getServiceReference(ProvisioningEventBus.class.getName());
		if (busReference == null)
			return null;
		return (ProvisioningEventBus) context.getService(busReference);
	}

	// TODO hack for triggering events from the UI.  
	public void notifyListeners(EventObject event) {
		eventManager.notifyListeners(event);
	}

	public void removeProvisioningListener(StructuredViewerProvisioningListener listener) {
		if ((listener.getEventTypes() & StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) == StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) {
			eventManager.removeListener(listener);
		} else {
			ServiceReference busReference = context.getServiceReference(ProvisioningEventBus.class.getName());
			ProvisioningEventBus bus = (ProvisioningEventBus) context.getService(busReference);
			bus.removeListener(listener);
		}
	}

	private void initializeImages() {
		createImageDescriptor(ProvUIImages.IMG_METADATA_REPOSITORY);
		createImageDescriptor(ProvUIImages.IMG_ARTIFACT_REPOSITORY);
		createImageDescriptor(ProvUIImages.IMG_IU);
		createImageDescriptor(ProvUIImages.IMG_UNINSTALLED_IU);
		createImageDescriptor(ProvUIImages.IMG_CATEGORY);
		createImageDescriptor(ProvUIImages.IMG_PROFILE);
		createImageDescriptor(ProvUIImages.IMG_TOOL_UPDATE);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_INSTALL);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_REVERT);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UNINSTALL);
		createImageDescriptor(ProvUIImages.WIZARD_BANNER_UPDATE);
	}

	/**
	 * Creates an image and places it in the image registry.
	 */
	private void createImageDescriptor(String id) {
		URL url = FileLocator.find(getBundle(), new Path(ProvUIImages.ICON_PATH + id), null);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		getImageRegistry().put(id, desc);
	}
}
