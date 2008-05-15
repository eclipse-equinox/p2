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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.extensionlocation.*;
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
	private static final String PLATFORM_CFG = "org.eclipse.update/platform.xml"; //$NON-NLS-1$
	private static final String CACHE_FILENAME = "cache.timestamps"; //$NON-NLS-1$
	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;
	private ServiceReference packageAdminRef;
	private List watchers = new ArrayList();
	private static Collection dropinRepositories;
	private static Collection configurationRepositories;

	/**
	 * Helper method to create an extension location metadata repository at the given URL. 
	 * If one already exists at that location then an exception will be thrown.
	 * 
	 * This method never returns <code>null</code>.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException 
	 */
	public static IMetadataRepository createExtensionLocationMetadataRepository(URL location, String name, Map properties) throws ProvisionException {
		BundleContext context = getContext();
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(context, IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered."); //$NON-NLS-1$
		ExtensionLocationMetadataRepositoryFactory factory = new ExtensionLocationMetadataRepositoryFactory();
		IMetadataRepository repository = factory.create(location, name, ExtensionLocationMetadataRepository.TYPE, properties);
		//we need to add the concrete repository to the repository manager, or its properties will not be correct
		((MetadataRepositoryManager) manager).addRepository(repository);
		return repository;
	}

	/**
	 * Helper method to load an extension location metadata repository from the given URL.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException
	 */
	public static IMetadataRepository loadMetadataRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		BundleContext context = getContext();
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(context, IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered."); //$NON-NLS-1$
		return manager.loadRepository(location, monitor);
	}

	/**
	 * Helper method to create an extension location artifact repository at the given URL. 
	 * If one already exists at that location then an exception will be thrown.
	 * 
	 * This method never returns <code>null</code>.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException 
	 */
	public static IArtifactRepository createExtensionLocationArtifactRepository(URL location, String name, Map properties) throws ProvisionException {
		BundleContext context = getContext();
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(context, IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered."); //$NON-NLS-1$
		ExtensionLocationArtifactRepositoryFactory factory = new ExtensionLocationArtifactRepositoryFactory();
		IArtifactRepository repository = factory.create(location, name, ExtensionLocationArtifactRepository.TYPE, properties);
		//we need to add the concrete repository to the repository manager, or its properties will not be correct
		((ArtifactRepositoryManager) manager).addRepository(repository);
		return repository;
	}

	/**
	 * Helper method to load an extension location metadata repository from the given URL.
	 * 
	 * @throws IllegalStateException
	 * @throws ProvisionException
	 */
	public static IArtifactRepository loadArtifactRepository(URL location, IProgressMonitor monitor) throws ProvisionException {
		BundleContext context = getContext();
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(context, IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered."); //$NON-NLS-1$
		return manager.loadRepository(location, monitor);
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

		if (isUpToDate())
			return;
		// TODO i-build to i-build backwards compatibility code to remove the
		// old .pooled repositories. Remove this call soon.
		removeOldRepos();
		// create the watcher for the "drop-ins" folder
		watchDropins(profile);
		// keep an eye on the platform.xml
		watchConfiguration();

		synchronize(null);
		writeTimestamps();

		// we should probably be holding on to these repos by URL
		// see Bug 223422
		// for now explicitly nulling out these repos to allow GC to occur
		dropinRepositories = null;
		configurationRepositories = null;
	}

	/*
	 * Return a boolean value indicating whether or not we need to run
	 * the reconciler due to changes in the file-system.
	 */
	private boolean isUpToDate() {
		// the user might want to force a reconciliation
		if ("true".equals(getContext().getProperty("osgi.checkConfiguration")))
			return false;
		// read timestamps
		Properties timestamps = readTimestamps();
		if (timestamps.isEmpty())
			return false;
		// check platform.xml
		File configuration = getConfigurationLocation();
		if (configuration != null) {
			configuration = new File(configuration, PLATFORM_CFG);
			if (!Long.toString(configuration.lastModified()).equals(timestamps.getProperty(configuration.getAbsolutePath())))
				return false;
			// the plugins and features directories are always siblings to the configuration directory
			File parent = configuration.getParentFile();
			if (parent != null) {
				File plugins = new File(parent, "plugins");
				if (!Long.toString(plugins.lastModified()).equals(timestamps.getProperty(plugins.getAbsolutePath())))
					return false;
				File features = new File(parent, "features");
				if (!Long.toString(features.lastModified()).equals(timestamps.getProperty(features.getAbsolutePath())))
					return false;
			}
		}
		// check dropins folder
		File dropins = getDropinsDirectory();
		if (dropins != null) {
			if (!Long.toString(dropins.lastModified()).equals(timestamps.getProperty(dropins.getAbsolutePath())))
				return false;
		}
		// check links folder
		File links = getLinksDirectory();
		if (links != null) {
			if (!Long.toString(links.lastModified()).equals(timestamps.getProperty(links.getAbsolutePath())))
				return false;
		}
		return true;
	}

	/*
	 * Restore the cached timestamp values.
	 */
	private Properties readTimestamps() {
		Properties result = new Properties();
		File file = Activator.getContext().getDataFile(CACHE_FILENAME);
		if (!file.exists())
			return result;
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			result.load(input);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error occurred while reading cached timestamps for reconciliation.", e)); //$NON-NLS-1$
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return result;
	}

	/*
	 * Persist the cache timestamp values.
	 */
	private void writeTimestamps() {
		Properties timestamps = new Properties();
		// cache the platform.xml file timestamp
		File configuration = getConfigurationLocation();
		if (configuration != null) {
			configuration = new File(configuration, PLATFORM_CFG);
			// always write out the timestamp even if it doesn't exist so we can detect addition/removal
			timestamps.put(configuration.getAbsolutePath(), Long.toString(configuration.lastModified()));
			File parent = configuration.getParentFile();
			if (parent != null) {
				File plugins = new File(parent, "plugins");
				timestamps.put(plugins.getAbsolutePath(), Long.toString(plugins.lastModified()));
				File features = new File(parent, "features");
				timestamps.put(features.getAbsolutePath(), Long.toString(features.lastModified()));
			}
		}
		// cache the dropins folder timestamp
		// always write out the timestamp even if it doesn't exist so we can detect addition/removal
		File dropins = getDropinsDirectory();
		if (dropins != null)
			timestamps.put(dropins.getAbsolutePath(), Long.toString(dropins.lastModified()));
		// cache links folder timestamp
		// always write out the timestamp even if it doesn't exist so we can detect addition/removal
		File links = getLinksDirectory();
		if (links != null)
			timestamps.put(links.getAbsolutePath(), Long.toString(links.lastModified()));

		// write out the file
		File file = Activator.getContext().getDataFile(CACHE_FILENAME);
		OutputStream output = null;
		try {
			file.delete();
			output = new BufferedOutputStream(new FileOutputStream(file));
			timestamps.store(output, null);
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Error occurred while writing cache timestamps for reconciliation.", e)); //$NON-NLS-1$
		} finally {
			if (output != null)
				try {
					output.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	/*
	 * TODO Backwards compatibility code to remove the
	 * old .pooled repositories from the saved list. Remove
	 * this method soon.
	 */
	private void removeOldRepos() {
		URL osgiInstallArea = getOSGiInstallArea();
		if (osgiInstallArea == null)
			return;
		URL location = null;
		try {
			location = new URL(getOSGiInstallArea(), ".pooled"); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.ERROR, ID, "Error occurred while removing old repositories.", e)); //$NON-NLS-1$
			return;
		}
		BundleContext context = getContext();
		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(context, IArtifactRepositoryManager.class.getName());
		if (artifactManager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered."); //$NON-NLS-1$
		artifactManager.removeRepository(location);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(context, IMetadataRepositoryManager.class.getName());
		if (metadataManager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered."); //$NON-NLS-1$
		metadataManager.removeRepository(location);
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
		configFile = new File(configFile, PLATFORM_CFG);
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

		DropinsRepositoryListener listener = new DropinsRepositoryListener(Activator.getContext(), dropinsDirectory.getAbsolutePath());
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
	 * Do a look-up and return the OSGi install area if it is set.
	 */
	public static URL getOSGiInstallArea() {
		Location location = (Location) ServiceHelper.getService(Activator.getContext(), Location.class.getName(), Location.INSTALL_FILTER);
		if (location == null)
			return null;
		if (!location.isSet())
			return null;
		return location.getURL();
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

	/*
	 * Return the location of the links directory, or null if it is not available.
	 */
	private static File getLinksDirectory() {
		File root = getEclipseHome();
		return root == null ? null : new File(root, LINKS);
	}

	/*
	 * Return the location of the dropins directory, or null if it is not available.
	 */
	private static File getDropinsDirectory() {
		String watchedDirectoryProperty = bundleContext.getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null)
			return new File(watchedDirectoryProperty);
		File root = getEclipseHome();
		return root == null ? null : new File(root, DROPINS);
	}

	/*
	 * Return the current profile or null if it cannot be retrieved.
	 */
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

	/*
	 * Return the bundle with the given symbolic name, or null if it cannot be found.
	 */
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
