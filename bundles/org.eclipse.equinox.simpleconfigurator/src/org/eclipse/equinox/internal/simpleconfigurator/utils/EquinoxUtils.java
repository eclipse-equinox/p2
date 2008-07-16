/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.net.URL;
import org.eclipse.equinox.internal.simpleconfigurator.console.ConfiguratorCommandProvider;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxUtils {

	public static URL[] getConfigAreaURL(BundleContext context) {
		Filter filter = null;
		try {
			filter = context.createFilter(Location.CONFIGURATION_FILTER);
		} catch (InvalidSyntaxException e) {
			// should not happen
		}
		ServiceTracker configLocationTracker = new ServiceTracker(context, filter, null);
		configLocationTracker.open();
		try {
			Location configLocation = (Location) configLocationTracker.getService();
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
		} finally {
			configLocationTracker.close();
		}
	}

	public static ServiceRegistration registerConsoleCommands(BundleContext context) {
		return context.registerService(CommandProvider.class.getName(), new ConfiguratorCommandProvider(context), null);
	}
}
