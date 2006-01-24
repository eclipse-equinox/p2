/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.simpleConfigurator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.internal.simpleconfigurator.BundleInfo;
import org.eclipse.core.internal.simpleconfigurator.ConfigurationReader;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

public class Installer {
	
	private void install(BundleInfo[] expectedState, BundleContext manipulatingContext, PackageAdmin adminService, StartLevel startLevelService) {
		Collection toRefresh = new ArrayList();
		Collection toStart = new ArrayList();
		toRefresh.addAll(installBundles(expectedState, toStart, manipulatingContext, adminService, startLevelService));
		toRefresh.addAll(uninstallBundles(expectedState, manipulatingContext, adminService));
		refreshPackages((Bundle[])toRefresh.toArray(new Bundle[toRefresh.size()]), manipulatingContext);
		startBundles((Bundle[]) toStart.toArray(new Bundle[toStart.size()]));
		//if time stamps are the same
		//  do nothing
		//  return
		//if list exists
		//  force the list in the fwk
		//else
		//  discover bundles in folders and force the list in the fwk
	}
	
	private ArrayList installBundles(BundleInfo[] finalList, Collection toStart, BundleContext manipulatingContext, PackageAdmin adminService, StartLevel startLevelService) {
		ArrayList installed = new ArrayList(); 
		for (int i = 0; i < finalList.length; i++) {
			//TODO here we do not deal with bundles that don't have a symbolic id
			//TODO Need to handle the case where getBundles return multiple value
			Bundle[] matches = adminService.getBundles(finalList[i].getSymbolicName(), finalList[i].getVersion());
			Bundle current = matches == null ? null : (matches.length == 0 ? null : matches[0]); 
			if (current == null) {
				try {
					if (finalList[i].getLocation() == null) {
						System.out.println("Bundle " + finalList[i].getSymbolicName() + " " + finalList[i].getVersion());
						continue;
					}
					//TODO Need to try the install with reference:
					current = manipulatingContext.installBundle(finalList[i].getLocation());
					installed.add(current);
				} catch (BundleException e) {
					System.err.println("Can't install " + finalList[i].getSymbolicName() + " / " +  finalList[i].getVersion() +  " from location "+ finalList[i].getLocation());
					continue;
				}
			}
			int startLevel = finalList[i].getStartLevel();
			//TODO Need to find a way to not have the system bundle in this list
			if (startLevel != -1) {
				String name = current.getSymbolicName();
				if (! "org.eclipse.core.simpleConfigurator".equals(name))
					startLevelService.setBundleStartLevel(current, startLevel);
			}
			if (finalList[i].getExpectedState() == Bundle.ACTIVE)
				toStart.add(current);
		}
		return installed;
	}
	
	private Collection uninstallBundles(BundleInfo[] finalList, BundleContext manipulatingContext, PackageAdmin adminService) {
		Bundle[] allBundles = manipulatingContext.getBundles();
		
		//Build a set with all the bundles from the system
		Set installedBundles = new HashSet(allBundles.length); 
		for (int i = 0; i < allBundles.length; i++) {
			installedBundles.add(allBundles[i]);
		}
		
		//Remove all the bundles appearing in the final list from the set of installed bundles
		for (int i = 0; i < finalList.length; i++) {
			Bundle[] toAdd = adminService.getBundles(finalList[i].getSymbolicName(), finalList[i].getVersion());
			for (int j = 0; j < toAdd.length; j++) {
				installedBundles.remove(toAdd[j]);
			}
		}
		
		for (Iterator iter = installedBundles.iterator(); iter.hasNext();) {
			try {
				((Bundle) iter.next()).uninstall();
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return installedBundles;
	}
	
	private void refreshPackages(Bundle[] bundles, BundleContext manipulatingContext) {
		if (bundles.length == 0)
			return;
		ServiceReference packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null) {
			packageAdmin = (PackageAdmin) manipulatingContext.getService(packageAdminRef);
			if (packageAdmin == null)
				return;
		}
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED)
					synchronized (flag) {
						flag[0] = true;
						flag.notifyAll();
					}
			}
		};
		manipulatingContext.addFrameworkListener(listener);
		packageAdmin.refreshPackages(bundles);
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		manipulatingContext.removeFrameworkListener(listener);
		manipulatingContext.ungetService(packageAdminRef);
	}
	
	private void startBundles(Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (bundle.getState() == Bundle.INSTALLED) {
				//throw new IllegalStateException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED, bundle.getLocation()));
				System.err.println("Bundle " + bundle + " can't be started because it is not resolved");
				continue;
			}
			if (bundle.getState() == Bundle.STARTING)
				continue;
		
			try {
				bundle.start();
			} catch (BundleException e) {
				e.printStackTrace();
//				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_START, bundle.getLocation()), 0, e, null);
//				log.log(entry);
			}
		}
	}
	
	//TODO What should be the behavior of these methods if the configuration file is missing?
	//For now it just does nothing which allows for bootstrap with missing files
	public void applyConfiguration(BundleContext manipulatingContext) {
		ServiceReference locationRef = null;
		try {
			locationRef = manipulatingContext.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)")[0];
		} catch (InvalidSyntaxException e1) {
			//ignore, they are no syntax error here
		}
		Location configLocation = (Location) manipulatingContext.getService(locationRef);
		URL configLocationURL = null;
		try {
			String specifiedURL = System.getProperty("eclipse.simpleConfigurator.configURL");
			if (specifiedURL != null)
				configLocationURL = new URL(specifiedURL);
		} catch (MalformedURLException e) {
			//ignore and keep going
		}finally {
			if (configLocationURL == null)
				configLocationURL = configLocation.getURL();
		}
		applyConfiguration(manipulatingContext, configLocationURL);
	}
	
	public void applyConfiguration(BundleContext manipulatingContext, URL configLocation) {
		ServiceReference packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin adminService = (PackageAdmin) manipulatingContext.getService(packageAdminRef);

		ServiceReference startLevelRef = manipulatingContext.getServiceReference(StartLevel.class.getName());
		StartLevel startLevelService = (StartLevel) manipulatingContext.getService(startLevelRef);

		ConfigurationReader reader = new ConfigurationReader(configLocation);
		BundleInfo[] expectedBundles = reader.getExpectedState();
		if (expectedBundles != null)
			install(expectedBundles, manipulatingContext, adminService, startLevelService);
	}
	
}
