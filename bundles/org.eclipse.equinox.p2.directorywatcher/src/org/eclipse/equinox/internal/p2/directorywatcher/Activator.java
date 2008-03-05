/*******************************************************************************
 * Copyright (c) 2007, 2008 aQute, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * aQute - initial implementation and ideas 
 * IBM Corporation - initial adaptation to Equinox provisioning use
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.directorywatcher;

import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This clever little bundle watches a directory and will install any jar file
 * if finds in that directory 
 */

public class Activator implements BundleActivator, ManagedServiceFactory {
	static ServiceTracker packageAdminTracker;
	static ServiceTracker configAdminTracker;

	private static BundleContext context;
	Map watchers = null;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		watchers = new HashMap();
		Hashtable props = new Hashtable();
		props.put(Constants.SERVICE_PID, getName());
		aContext.registerService(ManagedServiceFactory.class.getName(), this, props);

		packageAdminTracker = new ServiceTracker(aContext, PackageAdmin.class.getName(), null);
		packageAdminTracker.open();
		configAdminTracker = new ServiceTracker(aContext, ConfigurationAdmin.class.getName(), null);
		configAdminTracker.open();

		// Created the initial configuration
		Hashtable properties = new Hashtable();
		set(properties, DirectoryWatcher.POLL);
		set(properties, DirectoryWatcher.DIR);
		updated("initial", properties); //$NON-NLS-1$
	}

	private void set(Hashtable properties, String key) {
		Object o = context.getProperty(key);
		if (o == null)
			return;
		properties.put(key, o);
	}

	public void stop(BundleContext aContext) throws Exception {
		if (watchers == null)
			return;
		for (Iterator i = watchers.values().iterator(); i.hasNext();)
			try {
				DirectoryWatcher watcher = (DirectoryWatcher) i.next();
				watcher.stop();
			} catch (Exception e) {
				// Ignore
			}
		watchers = null;
		configAdminTracker.close();
		packageAdminTracker.close();
	}

	public void deleted(String pid) {
		DirectoryWatcher watcher = (DirectoryWatcher) watchers.remove(pid);
		if (watcher != null)
			watcher.stop();
	}

	public String getName() {
		return "equinox.p2.directorywatcher"; //$NON-NLS-1$
	}

	public void updated(String pid, Dictionary properties) {
		deleted(pid);
		DirectoryWatcher watcher = new DirectoryWatcher(properties, context);
		watchers.put(pid, watcher);
		watcher.addListener(new JARFileListener());
		watcher.start();
	}

	public static ConfigurationAdmin getConfigAdmin() {
		return (ConfigurationAdmin) Activator.configAdminTracker.getService();
	}

	public static PackageAdmin getPackageAdmin(int timeout) {
		if (timeout == 0)
			return (PackageAdmin) packageAdminTracker.getService();
		try {
			return (PackageAdmin) packageAdminTracker.waitForService(timeout);
		} catch (InterruptedException e) {
			return null;
		}
	}

}
