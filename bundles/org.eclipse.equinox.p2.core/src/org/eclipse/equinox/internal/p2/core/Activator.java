/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	public static final String AGENT_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	public static Location agentDataLocation = null;

	public static BundleContext context;
	public static Location downloadLocation = null;
	private static Activator instance;
	// Data mode constants for user, configuration and data locations.
	private static final String NO_DEFAULT = "@noDefault"; //$NON-NLS-1$
	private static final String NONE = "@none"; //$NON-NLS-1$

	private static final String PROP_CONFIG_DIR = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_TMPDIR = "java.io.tmpdir"; //$NON-NLS-1$
	public static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
	public static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$

	public static final String READ_ONLY_AREA_SUFFIX = ".readOnly"; //$NON-NLS-1$

	private static final String VAR_CONFIG_DIR = "@config.dir"; //$NON-NLS-1$
	private static final String VAR_USER_DIR = "@user.dir"; //$NON-NLS-1$
	private static final String VAR_USER_HOME = "@user.home"; //$NON-NLS-1$
	private ServiceRegistration agentLocationRegistration = null;
	ServiceTracker logTracker;

	/**
	 * NOTE: This method is copied from LocationHelper in org.eclipse.osgi
	 * due to access restrictions.
	 */
	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getFile();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URL(url.getProtocol(), url.getHost(), file);
	}

	/**
	 * Builds a URL with the given specification
	 * NOTE: This method is copied from LocationHelper in org.eclipse.osgi
	 * due to access restrictions.
	 * 
	 * @param spec the URL specification
	 * @param trailingSlash flag to indicate a trailing slash on the spec
	 * @return a URL
	 */
	private static URL buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		boolean isFile = spec.startsWith("file:"); //$NON-NLS-1$
		try {
			if (isFile)
				return adjustTrailingSlash(new File(spec.substring(5)).toURL(), trailingSlash);
			return new URL(spec);
		} catch (MalformedURLException e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				return adjustTrailingSlash(new File(spec).toURL(), trailingSlash);
			} catch (MalformedURLException e1) {
				return null;
			}
		}
	}

	/**
	 * Returns the framework log, or null if not available
	 */
	public static FrameworkLog getFrameworkLog() {
		//protect against concurrent shutdown
		Activator a = instance;
		if (a == null)
			return null;
		ServiceTracker tracker = a.getLogTracker();
		if (tracker == null)
			return null;
		return (FrameworkLog) tracker.getService();
	}

	private static String substituteVar(String source, String var, String prop) {
		String value = Activator.context.getProperty(prop);
		if (value == null)
			value = "";
		return value + source.substring(var.length());
	}

	private Location buildLocation(String property, URL defaultLocation, boolean readOnlyDefault, boolean addTrailingSlash) {
		String location = Activator.context.getProperty(property);
		// the user/product may specify a non-default readOnly setting   
		String userReadOnlySetting = Activator.context.getProperty(property + READ_ONLY_AREA_SUFFIX);
		boolean readOnly = (userReadOnlySetting == null ? readOnlyDefault : Boolean.valueOf(userReadOnlySetting).booleanValue());
		// if the instance location is not set, predict where the workspace will be and 
		// put the instance area inside the workspace meta area.
		if (location == null)
			return new BasicLocation(property, defaultLocation, readOnly);
		String trimmedLocation = location.trim();
		if (trimmedLocation.equalsIgnoreCase(NONE))
			return null;
		if (trimmedLocation.equalsIgnoreCase(NO_DEFAULT))
			return new BasicLocation(property, null, readOnly);
		if (trimmedLocation.startsWith(VAR_USER_HOME)) {
			String base = substituteVar(location, VAR_USER_HOME, PROP_USER_HOME);
			location = new File(base).getAbsolutePath();
		} else if (trimmedLocation.startsWith(VAR_USER_DIR)) {
			String base = substituteVar(location, VAR_USER_DIR, PROP_USER_DIR);
			location = new File(base).getAbsolutePath();
		} else if (trimmedLocation.startsWith(VAR_CONFIG_DIR)) {
			//note the config dir system property is already a URL
			location = substituteVar(location, VAR_CONFIG_DIR, PROP_CONFIG_DIR);
		}
		URL url = buildURL(location, addTrailingSlash);
		BasicLocation result = null;
		if (url != null) {
			result = new BasicLocation(property, null, readOnly);
			result.setURL(url, false);
		}
		return result;
	}

	private ServiceTracker getLogTracker() {
		if (logTracker != null)
			return logTracker;
		//lazy init if the bundle has been started
		if (context == null)
			return null;
		logTracker = new ServiceTracker(context, FrameworkLog.class.getName(), null);
		logTracker.open();
		return logTracker;
	}

	public void start(BundleContext context) throws Exception {
		instance = this;
		Activator.context = context;
		URL defaultLocation = new URL(context.getProperty("osgi.configuration.area") + "org.eclipse.equinox.p2.core/agentdata/");
		agentDataLocation = buildLocation(AGENT_DATA_AREA, defaultLocation, false, true);
		Dictionary locationProperties = new Hashtable();
		if (defaultLocation != null) {
			locationProperties.put("type", AGENT_DATA_AREA); //$NON-NLS-1$
			agentLocationRegistration = context.registerService(new String[] {Location.class.getName(), AgentLocation.class.getName()}, agentDataLocation, locationProperties);
		}
	}

	public void stop(BundleContext context) throws Exception {
		instance = null;
		if (agentLocationRegistration != null)
			agentLocationRegistration.unregister();
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		Activator.context = null;
	}
}
