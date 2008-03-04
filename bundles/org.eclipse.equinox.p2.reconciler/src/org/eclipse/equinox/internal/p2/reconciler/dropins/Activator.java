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
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String OSGI_CONFIGURATION_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$
	private static final String PROFILE_EXTENSION = "profile.extension"; //$NON-NLS-1$
	private static PackageAdmin packageAdmin;
	private static BundleContext bundleContext;
	private ServiceReference packageAdminRef;
	private List watchers = new ArrayList();
	private static IMetadataRepository dropinRepository;

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

		// create the watcher for the "drop-ins" folder
		watchDropins(profile);

		// create watchers for the sites specified in the platform.xml
		// TODO
		if (Boolean.getBoolean("org.eclipse.p2.update.compatibility")) //$NON-NLS-1$
			watchConfiguration();

		synchronize(new ArrayList(0), null);
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
	public static void synchronize(List extraRepositories, IProgressMonitor monitor) {
		IProfile profile = getCurrentProfile(bundleContext);
		if (profile == null)
			return;
		// create the profile synchronizer on all available repositories
		List repositories = new ArrayList(extraRepositories);
		if (dropinRepository != null)
			repositories.add(dropinRepository);
		ProfileSynchronizer synchronizer = new ProfileSynchronizer(profile, repositories);
		synchronizer.synchronize(monitor);
	}

	/*
	 * Watch the platform.xml file.
	 */
	private void watchConfiguration() throws ProvisionException {
		File configFile = new File("configuration/org.eclipse.update/platform.xml"); //$NON-NLS-1$
		DirectoryWatcher watcher = new DirectoryWatcher(configFile.getParentFile());
		try {
			PlatformXmlListener listener = new PlatformXmlListener(configFile);
			watcher.addListener(listener);
		} catch (ProvisionException e) {
			// TODO proper logging
			e.printStackTrace();
		}
		watchers.add(watcher);
		watcher.start();

		// pay attention to the links/ folder too. this is only needed on startup though since
		// any other changes during execution will be reflected in the platform.xml file
		LinksManager manager = new LinksManager();
		manager.synchronize(configFile, new File("links"));
	}

	/*
	 * Create a new directory watcher with a repository listener on the drop-ins folder. 
	 */
	private void watchDropins(IProfile profile) {
		File folder = getWatchedDirectory(bundleContext);
		if (folder == null)
			return;

		RepositoryListener listener = new RepositoryListener(Activator.getContext(), Integer.toString(folder.hashCode()));
		listener.getArtifactRepository().setProperty(PROFILE_EXTENSION, profile.getProfileId());

		List folders = new ArrayList();
		folders.add(folder);
		File eclipseFeatures = new File(folder, "eclipse/features");
		if (eclipseFeatures.isDirectory())
			folders.add(eclipseFeatures);
		File eclipsePlugins = new File(folder, "eclipse/plugins");
		if (eclipsePlugins.isDirectory())
			folders.add(eclipsePlugins);

		DirectoryWatcher watcher = new DirectoryWatcher((File[]) folders.toArray(new File[folders.size()]));
		watcher.addListener(listener);
		watcher.poll();

		dropinRepository = listener.getMetadataRepository();
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

	public static File getWatchedDirectory(BundleContext context) {
		String watchedDirectoryProperty = context.getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null) {
			File folder = new File(watchedDirectoryProperty);
			return folder;
		}
		try {
			//TODO: a proper install area would be better. osgi.install.area is relative to the framework jar
			URL baseURL = new URL(context.getProperty(OSGI_CONFIGURATION_AREA));
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

	private static IProfile getCurrentProfile(BundleContext context) {
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
