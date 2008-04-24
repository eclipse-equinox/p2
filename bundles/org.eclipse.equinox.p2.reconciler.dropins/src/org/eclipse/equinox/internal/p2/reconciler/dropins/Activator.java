/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.reconciler.dropins"; //$NON-NLS-1$
	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$
	private static final String LINKS = "links"; //$NON-NLS-1$
	//	private static final String PROFILE_EXTENSION = "profile.extension"; //$NON-NLS-1$
	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;
	private ServiceReference packageAdminRef;
	private List watchers = new ArrayList();
	private static Collection dropinRepositories;
	private static Collection configurationRepositories;
	private static IMetadataRepository[] linksRepositories;
	private static IMetadataRepository eclipseProductRepository;

	/**
	 * Helper method to load a metadata repository from the specified URL.
	 * This method never returns <code>null</code>.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException 
	 */
	public static IMetadataRepository loadMetadataRepository(URL repoURL) throws ProvisionException {
		BundleContext context = getContext();
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered."); //$NON-NLS-1$
		try {
			return manager.loadRepository(repoURL, null);
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Helper method to add the given metadata repository to the manager's list.
	 */
	public static void addRepository(IMetadataRepository repo) {
		BundleContext context = getContext();
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered."); //$NON-NLS-1$
		try {
			((MetadataRepositoryManager) manager).addRepository(repo);
		} finally {
			context.ungetService(reference);
		}
	}

	/**
	 * Helper method to load an artifact repository from the given URL.
	 * This method never returns <code>null</code>.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException
	 */
	public static IArtifactRepository loadArtifactRepository(URL repoURL) throws ProvisionException {
		BundleContext context = getContext();
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		IArtifactRepositoryManager manager = null;
		if (reference != null)
			manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");
		try {
			return manager.loadRepository(repoURL, null);
		} finally {
			context.ungetService(reference);
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		setPackageAdmin((PackageAdmin) context.getService(packageAdminRef));
		bundleContext = context;

		if (!startEarly("org.eclipse.equinox.p2.exemplarysetup")) //$NON-NLS-1$
			return;
		if (!startEarly("org.eclipse.equinox.simpleconfigurator.manipulator")) //$NON-NLS-1$
			return;
		if (!startEarly("org.eclipse.equinox.frameworkadmin.equinox")) //$NON-NLS-1$
			return;
		IProfile profile = getCurrentProfile(context);
		if (profile == null)
			return;

		// create a watcher for the main plugins and features directories
		watchEclipseProduct();
		// create the watcher for the "drop-ins" folder
		watchDropins(profile);
		// keep an eye on the platform.xml
		watchConfiguration();

		synchronize(null);

		// we should probably be holding on to these repos by URL
		// see Bug 223422
		// for now explicitly nulling out these repos to allow GC to occur
		dropinRepositories = null;
		configurationRepositories = null;
		linksRepositories = null;
		eclipseProductRepository = null;
	}

	private void watchEclipseProduct() {
		try {
			File configurationLocation = getConfigurationLocation();
			if (configurationLocation == null) {
				LogHelper.log(new Status(IStatus.ERROR, ID, "Unable to determine configuration location."));
				return;
			}
			File pool = new File(configurationLocation, "../.pooled").getCanonicalFile(); //$NON-NLS-1$
			URL pooledURL = pool.toURL();
			loadArtifactRepository(pooledURL);
			eclipseProductRepository = loadMetadataRepository(pooledURL);
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Error occurred while loading repository.", e));
		} catch (ProvisionException e) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Error occurred while loading repository.", e));
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Error occurred while loading repository.", e));
		}
	}

	private boolean startEarly(String bundleName) throws BundleException {
		Bundle bundle = getBundle(bundleName);
		if (bundle == null)
			return false;
		bundle.start(Bundle.START_TRANSIENT);
		return true;
	}

	/*
	 * Synchronize the profile.
	 */
	public static synchronized void synchronize(IProgressMonitor monitor) {
		IProfile profile = getCurrentProfile(bundleContext);
		if (profile == null)
			return;
		// create the profile synchronizer on all available repositories
		Set repositories = new HashSet();
		if (dropinRepositories != null)
			repositories.addAll(dropinRepositories);

		if (configurationRepositories != null)
			repositories.addAll(configurationRepositories);

		if (linksRepositories != null)
			repositories.addAll(Arrays.asList(linksRepositories));

		if (eclipseProductRepository != null)
			repositories.add(eclipseProductRepository);

		ProfileSynchronizer synchronizer = new ProfileSynchronizer(profile, repositories);
		IStatus result = synchronizer.synchronize(monitor);
		if (!result.isOK())
			LogHelper.log(result);

	}

	/*
	 * Watch the platform.xml file.
	 */
	private void watchConfiguration() {
		File configFile = getConfigurationLocation();
		if (configFile == null) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Unable to determine configuration location."));
			return;
		}
		configFile = new File(configFile, "org.eclipse.update/platform.xml"); //$NON-NLS-1$
		DirectoryWatcher watcher = new DirectoryWatcher(configFile.getParentFile());
		PlatformXmlListener listener = new PlatformXmlListener(configFile);
		watcher.addListener(listener);
		watcher.poll();
		configurationRepositories = listener.getMetadataRepositories();
	}

	/*
	 * Create a new directory watcher with a repository listener on the drop-ins folder. 
	 */
	private void watchDropins(IProfile profile) {
		List directories = new ArrayList();
		File dropinsDirectory = getDropinsDirectory();
		if (dropinsDirectory != null)
			directories.add(dropinsDirectory);
		File linksDirectory = getLinksDirectory();
		if (linksDirectory != null)
			directories.add(linksDirectory);
		if (directories.isEmpty())
			return;

		DropinsRepositoryListener listener = new DropinsRepositoryListener(Activator.getContext(), "dropins:" + dropinsDirectory.getAbsolutePath());
		//		listener.getArtifactRepository().setProperty(PROFILE_EXTENSION, profile.getProfileId());
		DirectoryWatcher watcher = new DirectoryWatcher((File[]) directories.toArray(new File[directories.size()]));
		watcher.addListener(listener);
		watcher.poll();

		dropinRepositories = listener.getMetadataRepositories();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		for (Iterator iter = watchers.iterator(); iter.hasNext();) {
			DirectoryWatcher watcher = (DirectoryWatcher) iter.next();
			watcher.stop();
		}
		bundleContext = null;
		setPackageAdmin(null);
		context.ungetService(packageAdminRef);
	}

	/*
	 * Return the bundle context for this bundle.
	 */
	public static BundleContext getContext() {
		return bundleContext;
	}

	/*
	 * Helper method to get the configuration location. Return null if
	 * it is unavailable.
	 */
	public static File getConfigurationLocation() {
		Location configurationLocation = (Location) ServiceHelper.getService(getContext(), Location.class.getName(), Location.CONFIGURATION_FILTER);
		if (configurationLocation == null || !configurationLocation.isSet())
			return null;
		URL url = configurationLocation.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	/*
	 * Helper method to return the eclipse.home location. Return
	 * null if it is unavailable.
	 */
	public static File getEclipseHome() {
		Location eclipseHome = (Location) ServiceHelper.getService(getContext(), Location.class.getName(), Location.ECLIPSE_HOME_FILTER);
		if (eclipseHome == null || !eclipseHome.isSet())
			return null;
		URL url = eclipseHome.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	private static File getLinksDirectory() {
		File root = getEclipseHome();
		return root == null ? null : new File(root, LINKS);
	}

	private static File getDropinsDirectory() {
		String watchedDirectoryProperty = bundleContext.getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null)
			return new File(watchedDirectoryProperty);
		File root = getEclipseHome();
		return root == null ? null : new File(root, DROPINS);
	}

	public static IProfile getCurrentProfile(BundleContext context) {
		ServiceReference reference = context.getServiceReference(IProfileRegistry.class.getName());
		if (reference == null)
			return null;
		IProfileRegistry profileRegistry = (IProfileRegistry) context.getService(reference);
		try {
			return profileRegistry.getProfile(IProfileRegistry.SELF);
		} finally {
			context.ungetService(reference);
		}
	}

	private static synchronized void setPackageAdmin(PackageAdmin service) {
		packageAdmin = service;
	}

	static synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

}
