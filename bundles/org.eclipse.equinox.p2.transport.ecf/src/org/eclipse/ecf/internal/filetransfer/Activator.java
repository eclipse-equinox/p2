/****************************************************************************
 * Copyright (c) 2004 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.internal.filetransfer;

import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.ecf.core.util.LogHelper;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.ecf.filetransfer"; //$NON-NLS-1$

	private static final String URLCONNECTION_FACTORY_EPOINT = PLUGIN_ID + "." //$NON-NLS-1$
			+ "urlStreamHandlerService"; //$NON-NLS-1$

	private static final String PROTOCOL_ATTRIBUTE = "protocol"; //$NON-NLS-1$

	private ServiceTracker extensionRegistryTracker = null;

	// The shared instance
	private static Activator plugin;

	private BundleContext context = null;

	private ServiceTracker logServiceTracker = null;

	/**
	 * The constructor
	 */
	public Activator() {
		// null constructor
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext ctxt) throws Exception {
		this.context = ctxt;
		plugin = this;
		setupProtocolHandlers(ctxt);
	}

	protected LogService getLogService() {
		if (logServiceTracker == null) {
			logServiceTracker = new ServiceTracker(this.context, LogService.class.getName(), null);
			logServiceTracker.open();
		}
		return (LogService) logServiceTracker.getService();
	}

	public void log(IStatus status) {
		LogService logService = getLogService();
		if (logService != null) {
			logService.log(LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
		}
	}

	public IExtensionRegistry getExtensionRegistry() {
		if (extensionRegistryTracker == null) {
			this.extensionRegistryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), null);
			this.extensionRegistryTracker.open();
		}
		return (IExtensionRegistry) extensionRegistryTracker.getService();
	}

	private void setupProtocolHandlers(BundleContext context) {
		IExtensionRegistry reg = getExtensionRegistry();
		if (reg != null) {
			IExtensionPoint extensionPoint = reg.getExtensionPoint(URLCONNECTION_FACTORY_EPOINT);
			if (extensionPoint == null) {
				return;
			}
			IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();

			for (IConfigurationElement configurationElement : configurationElements) {
				String protocol = configurationElement.getAttribute(PROTOCOL_ATTRIBUTE);
				if (protocol != null) {
					Hashtable properties = new Hashtable();
					properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {protocol});
					context.registerService(URLStreamHandlerService.class.getName(), new ProxyURLStreamHandlerService(configurationElement), properties);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext ctxt) throws Exception {
		if (extensionRegistryTracker != null) {
			extensionRegistryTracker.close();
			extensionRegistryTracker = null;
		}
		plugin = null;
		this.context = null;
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public synchronized static Activator getDefault() {
		if (plugin == null) {
			plugin = new Activator();
		}
		return plugin;
	}

}
