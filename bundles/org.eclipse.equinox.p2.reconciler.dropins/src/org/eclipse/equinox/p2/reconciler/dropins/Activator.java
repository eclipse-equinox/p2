package org.eclipse.equinox.p2.reconciler.dropins;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.configurator.Configurator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.director.IDirector;
import org.eclipse.equinox.p2.directorywatcher.DirectoryWatcher;
import org.eclipse.equinox.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$

	public void start(BundleContext context) throws Exception {

		Profile profile = getCurrentProfile(context);
		if (profile == null)
			return;

		File watchedFolder = getWatchedDirectory(context);
		if (watchedFolder == null)
			return;

		DirectoryWatcher watcher = new DirectoryWatcher(watchedFolder);
		RepositoryListener listener = new RepositoryListener(context, Integer.toString(watchedFolder.hashCode()));
		watcher.addListener(listener);
		watcher.poll();

		IArtifactRepository artifactRepository = listener.getArtifactRepository();
		artifactRepository.getModifiableProperties().put("profile.extension", profile.getProfileId());
		IMetadataRepository metadataRepository = listener.getMetadataRepository();
		ProfileSynchronizer synchronizer = new ProfileSynchronizer(profile, metadataRepository);

		removeIUs(context, profile, synchronizer.getIUsToRemove());

		// disable repo cleanup for now until we see how we want to handle support for links folders and eclipse extensions
		//removeUnwatchedRepositories(context, profile, watchedFolder);

		addIUs(context, profile, synchronizer.getIUsToAdd());
		applyConfiguration(context);
	}

	public void stop(BundleContext context) throws Exception {
		// do nothing
	}

	public static File getWatchedDirectory(BundleContext context) {

		String watchedDirectoryProperty = context.getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null) {
			File folder = new File(watchedDirectoryProperty);
			if (folder.isDirectory())
				return folder;

			return null;
		}

		Filter filter = null;
		try {
			filter = context.createFilter(Location.INSTALL_FILTER);
		} catch (InvalidSyntaxException e) {
			// should not happen
		}
		ServiceTracker installLocationTracker = new ServiceTracker(context, filter, null);
		installLocationTracker.open();
		try {
			Location installLocation = (Location) installLocationTracker.getService();
			if (installLocation == null)
				return null;

			URL baseURL = installLocation.getURL();
			if (baseURL == null)
				return null;

			try {
				URL folderURL = new URL(baseURL, DROPINS);
				File folder = new File(folderURL.getPath());
				if (folder.isDirectory())
					return folder;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return null;
		} finally {
			installLocationTracker.close();
		}
	}

	private void addIUs(BundleContext context, Profile profile, IInstallableUnit[] toAdd) {
		ServiceReference reference = context.getServiceReference(IDirector.class.getName());
		IDirector director = (IDirector) context.getService(reference);
		try {
			director.install(toAdd, profile, null);
		} finally {
			context.ungetService(reference);
		}
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

	private void removeIUs(BundleContext context, Profile profile, IInstallableUnit[] toRemove) {
		ServiceReference reference = context.getServiceReference(IDirector.class.getName());
		IDirector director = (IDirector) context.getService(reference);
		try {
			director.uninstall(toRemove, profile, null);
		} finally {
			context.ungetService(reference);
		}
	}

	private void applyConfiguration(BundleContext context) {
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			// unexpected -- log
			e.printStackTrace();
		} finally {
			context.ungetService(reference);
		}
	}

	private Profile getCurrentProfile(BundleContext context) {
		ServiceReference reference = context.getServiceReference(IProfileRegistry.class.getName());
		IProfileRegistry profileRegistry = (IProfileRegistry) context.getService(reference);
		try {
			return profileRegistry.getProfile(IProfileRegistry.SELF);
		} finally {
			context.ungetService(reference);
		}
	}
}
