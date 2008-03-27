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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.reconciler.dropins"; //$NON-NLS-1$
	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String OSGI_CONFIGURATION_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$
	//	private static final String PROFILE_EXTENSION = "profile.extension"; //$NON-NLS-1$
	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;
	private ServiceReference packageAdminRef;
	private List watchers = new ArrayList();
	private static IMetadataRepository[] dropinRepositories;
	private static IMetadataRepository[] configurationRepositories;
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
			throw new IllegalStateException("MetadataRepositoryManager not registered.");
		try {
			return manager.loadRepository(repoURL, null);
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
		if (false)
			watchConfiguration();

		synchronize(new ArrayList(0), null);

		// we should probably be  holding on to these repos by URL
		// see Bug 223422
		// for now explicitly nulling out these repos to allow GC to occur
		dropinRepositories = null;
		configurationRepositories = null;
		linksRepositories = null;
		eclipseProductRepository = null;
	}

	private void watchEclipseProduct() {

		URL baseURL;
		try {
			baseURL = new URL(bundleContext.getProperty(OSGI_CONFIGURATION_AREA));
			URL pooledURL = new URL(baseURL, "../.pooled"); //$NON-NLS-1$
			loadArtifactRepository(pooledURL);
			eclipseProductRepository = loadMetadataRepository(pooledURL);
		} catch (MalformedURLException e) {
			// TODO proper logging
			e.printStackTrace();
		} catch (ProvisionException e) {
			// TODO proper logging
			e.printStackTrace();
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
	public static synchronized void synchronize(List extraRepositories, IProgressMonitor monitor) {
		IProfile profile = getCurrentProfile(bundleContext);
		if (profile == null)
			return;
		// create the profile synchronizer on all available repositories
		Set repositories = new HashSet(extraRepositories);
		if (dropinRepositories != null)
			repositories.addAll(Arrays.asList(dropinRepositories));

		if (configurationRepositories != null)
			repositories.addAll(Arrays.asList(configurationRepositories));

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
		File configFile = new File("configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		DirectoryWatcher watcher = new DirectoryWatcher(configFile.getParentFile());
		try {
			PlatformXmlListener listener = new PlatformXmlListener(configFile);
			watcher.addListener(listener);
			watcher.poll();
			List repositories = listener.getMetadataRepositories();
			if (repositories != null)
				configurationRepositories = (IMetadataRepository[]) repositories.toArray(new IMetadataRepository[0]);
		} catch (ProvisionException e) {
			// TODO proper logging
			e.printStackTrace();
		}
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

	private static File getLinksDirectory() {
		try {
			//TODO: a proper install area would be better. osgi.install.area is relative to the framework jar
			URL baseURL = new URL(bundleContext.getProperty(OSGI_CONFIGURATION_AREA));
			URL folderURL = new URL(baseURL, "../links"); //$NON-NLS-1$
			return new File(folderURL.getPath());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static File getDropinsDirectory() {
		String watchedDirectoryProperty = bundleContext.getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null) {
			File folder = new File(watchedDirectoryProperty);
			return folder;
		}
		try {
			//TODO: a proper install area would be better. osgi.install.area is relative to the framework jar
			URL baseURL = new URL(bundleContext.getProperty(OSGI_CONFIGURATION_AREA));
			URL folderURL = new URL(baseURL, "../" + DROPINS); //$NON-NLS-1$
			File folder = new File(folderURL.getPath());
			return folder;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Disabled for now

	//	private void removeUnwatchedRepositories(BundleContext context, Profile profile, File watchedFolder) {
	//		removeUnwatchedMetadataRepositories(context, profile, watchedFolder);
	//		removeUnwatchedArtifactRepositories(context, profile, watchedFolder);
	//	}
	//
	//	private void removeUnwatchedArtifactRepositories(BundleContext context, Profile profile, File watchedFolder) {
	//		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
	//		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) context.getService(reference);
	//		try {
	//			IArtifactRepository[] repositories = manager.getKnownRepositories();
	//			for (int i = 0; i < repositories.length; i++) {
	//				Map properties = repositories[i].getProperties();
	//				String profileId = (String) properties.get("profileId");
	//				String folderName = (String) properties.get("folder");
	//
	//				if (profile.getProfileId().equals(profileId) && !watchedFolder.getAbsolutePath().equals(folderName)) {
	//					manager.removeRepository(repositories[i]);
	//				}
	//			}
	//		} finally {
	//			context.ungetService(reference);
	//		}
	//	}
	//
	//	private void removeUnwatchedMetadataRepositories(BundleContext context, Profile profile, File watchedFolder) {
	//		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
	//		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) context.getService(reference);
	//		try {
	//			IMetadataRepository[] repositories = manager.getKnownRepositories();
	//			for (int i = 0; i < repositories.length; i++) {
	//				Map properties = repositories[i].getProperties();
	//				String profileId = (String) properties.get("profileId");
	//				if (profile.getProfileId().equals(profileId)) {
	//					String folderName = (String) properties.get("folder");
	//					if ((folderName != null) && !watchedFolder.getAbsolutePath().equals(folderName)) {
	//						manager.removeRepository(repositories[i].getLocation());
	//					}
	//				}
	//			}
	//		} finally {
	//			context.ungetService(reference);
	//		}
	//	}

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
