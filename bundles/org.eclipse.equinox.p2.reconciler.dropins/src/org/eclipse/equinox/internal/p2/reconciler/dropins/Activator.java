/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
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
import org.eclipse.equinox.internal.p2.update.PlatformXmlListener;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.reconciler.dropins.ProfileSynchronizer;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class Activator implements BundleActivator {

	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String OSGI_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
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
		Profile profile = getCurrentProfile(context);
		if (profile == null)
			return;

		// create the watcher for the "drop-ins" folder
		watchDropins(profile);

		// create watchers for the sites specified in the platform.xml
		// TODO
		if (false)
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
		Profile profile = getCurrentProfile(bundleContext);
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
	private void watchConfiguration() {
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
	}

	/*
	 * Create a new directory watcher with a repository listener on the drop-ins folder. 
	 */
	private void watchDropins(Profile profile) {
		File folder = getWatchedDirectory(bundleContext);
		if (folder == null)
			return;
		DirectoryWatcher watcher = new DirectoryWatcher(folder);
		RepositoryListener listener = new RepositoryListener(Activator.getContext(), Integer.toString(folder.hashCode()));
		listener.getArtifactRepository().getModifiableProperties().put(PROFILE_EXTENSION, profile.getProfileId());
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
			if (folder.isDirectory())
				return folder;
			return null;
		}
		try {
			URL baseURL = new URL(context.getProperty(OSGI_INSTALL_AREA));
			URL folderURL = new URL(baseURL, DROPINS);
			File folder = new File(folderURL.getPath());
			if (folder.isDirectory())
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

	private static Profile getCurrentProfile(BundleContext context) {
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
