/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.net.*;
import org.eclipse.equinox.internal.simpleconfigurator.console.ConfiguratorCommandProvider;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxUtils {

	public static URL[] getConfigAreaURL(BundleContext context) {
		Location configLocation = getConfigLocation(context);
		if (configLocation == null)
			return null;

		URL baseURL = configLocation.getURL();
		if (configLocation.getParentLocation() != null && configLocation.getURL() != null) {
			if (baseURL == null)
				return new URL[] {configLocation.getParentLocation().getURL()};
			else
				return new URL[] {baseURL, configLocation.getParentLocation().getURL()};
		}
		if (baseURL != null)
			return new URL[] {baseURL};

		return null;
	}

	public static Location getConfigLocation(BundleContext context) {
		Filter filter = null;
		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// should not happen
		}
		@SuppressWarnings({"rawtypes", "unchecked"})
		ServiceTracker configLocationTracker = new ServiceTracker(context, filter, null);
		configLocationTracker.open();
		try {
			return (Location) configLocationTracker.getService();
		} finally {
			configLocationTracker.close();
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static URI getInstallLocationURI(BundleContext context) {
		try {
			ServiceReference[] references = context.getServiceReferences(Location.class.getName(), Location.INSTALL_FILTER);
			if (references != null && references.length > 0) {
				ServiceReference reference = references[0];
				Location installLocation = (Location) context.getService(reference);
				if (installLocation != null) {
					try {
						if (installLocation.isSet()) {
							URL location = installLocation.getURL();
							return URIUtil.toURI(location);
						}
					} catch (URISyntaxException e) {
						//TODO: log an error
					} finally {
						context.ungetService(reference);
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			//TODO: log an error
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static ServiceRegistration registerConsoleCommands(BundleContext context) {
		return context.registerService(CommandProvider.class.getName(), new ConfiguratorCommandProvider(context), null);
	}
}
