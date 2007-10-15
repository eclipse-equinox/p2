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
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.resource.ImageDescriptor;
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

		// TODO for now we need to manually start up the provisioning
		// infrastructure
		// because the Eclipse Application launch config won't let me specify
		// bundles to start.
		getBundle("org.eclipse.equinox.p2.exemplarysetup").start(); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.frameworkadmin.equinox").start(); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.simpleconfigurator.manipulator").start(); //$NON-NLS-1$

		initializeImages();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		try {
			plugin = null;
			ProvUIActivator.context = null;
		} finally {
			super.stop(bundleContext);
		}
	}

	public void addProvisioningListener(StructuredViewerProvisioningListener listener) {
		// TODO hack for unsupported repository events.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=197052
		if ((listener.getEventTypes() & StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) == StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY) {
			eventManager.addListener(listener);
		} else {
			ServiceReference busReference = context.getServiceReference(ProvisioningEventBus.class.getName());
			ProvisioningEventBus bus = (ProvisioningEventBus) context.getService(busReference);
			bus.addListener(listener);
		}
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
		createImageDescriptor(ProvUIImages.IMG_PROFILE);
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
