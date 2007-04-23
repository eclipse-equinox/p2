package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.equinox.internal.simpleconfigurator.console.ConfiguratorCommandProvider;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class EquinoxUtils {

	public static URL getDefaultConfigURL(BundleContext context) {
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
			if (baseURL == null)
				return null;
			
			try {
				URL configURL = new URL(baseURL, SimpleConfiguratorConstants.CONFIGURATOR_FOLDER + "/" + SimpleConfiguratorConstants.CONFIG_LIST);
				File configFile = new File(configURL.getFile());
				if (configFile.exists())
					return configURL;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return null;
		} finally {
			configLocationTracker.close();
		}
	}

	public static ServiceRegistration registerConsoleCommands(BundleContext context) {
		return context.registerService(CommandProvider.class.getName(), new ConfiguratorCommandProvider(context), null);	}
}
