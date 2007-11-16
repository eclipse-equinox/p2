package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.generator.BundleDescriptionFactory;
import org.eclipse.equinox.p2.metadata.generator.MetadataGeneratorHelper;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class RepositoryListener extends DirectoryChangeListener {

	IMetadataRepository metadataRepository;
	IArtifactRepository artifactRepository;
	BundleDescriptionFactory bundleDescriptionFactory;
	Map currentFiles = new HashMap();

	public RepositoryListener(BundleContext context, File bundleDirectory) {

		String stateDirName = "listener_" + bundleDirectory.getAbsolutePath().hashCode();
		File stateDir = context.getDataFile(stateDirName);
		stateDir.mkdirs();

		URL stateDirURL;
		try {
			stateDirURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e.getMessage());
		}

		initializeMetadataRepository(context, stateDirURL);
		initializeArtifactRepository(context, stateDirURL);
		initializeBundleDescriptionFactory(context);
	}

	private void initializeBundleDescriptionFactory(BundleContext context) {

		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException("PlatformAdmin not registered.");
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException("PlatformAdmin not registered.");

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			bundleDescriptionFactory = new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

	private void initializeArtifactRepository(BundleContext context, URL stateDirURL) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered.");

		try {
			artifactRepository = manager.getRepository(stateDirURL);
			if (artifactRepository == null)
				artifactRepository = manager.createRepository(stateDirURL, "artifact listener", "org.eclipse.equinox.p2.artifact.repository.simpleRepository");
		} finally {
			context.ungetService(reference);
		}

		if (artifactRepository == null)
			throw new IllegalStateException("Couldn't create listener artifact repository for: " + stateDirURL);
	}

	private void initializeMetadataRepository(BundleContext context, URL stateDirURL) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		if (reference == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		try {
			metadataRepository = manager.getRepository(stateDirURL);
			if (metadataRepository == null)
				metadataRepository = manager.createRepository(stateDirURL, "metadata listener", "org.eclipse.equinox.p2.metadata.repository.simpleRepository");
		} finally {
			context.ungetService(reference);
		}

		if (metadataRepository == null)
			throw new IllegalStateException("Couldn't create listener metadata repository for: " + stateDirURL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		if (isInteresting(file))
			currentFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		if (isInteresting(file))
			currentFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
	 */
	public boolean removed(File file) {
		// note that we can't call #isInteresting here because we can't tell if the file handle
		// points to a directory because its already been removed.
		currentFiles.remove(file);
		return true;
	}

	/*
	 * Return a boolean value indicating whether or not we are interested in
	 * processing the given file. Currently we handle JAR files and directories.
	 */
	private boolean isInteresting(File file) {
		return file.isDirectory() || file.getAbsolutePath().endsWith(".jar");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#getExtensions()
	 */
	public String[] getExtensions() {
		// TODO use the empty string here for now to indicate that we are interested in everything
		return new String[] {""};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return (Long) currentFiles.get(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#startPoll()
	 */
	public void startPoll() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
	 */
	public void stopPoll() {
		synchronizeMetadataRepository();
		synchronizeArtifactRepository();
	}

	private void synchronizeMetadataRepository() {
		Map snapshot = new HashMap(currentFiles);
		List toRemove = new ArrayList();

		IInstallableUnit[] ius = metadataRepository.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			File iuFile = new File(iu.getProperty("file.name"));
			Long iuLastModified = new Long(iu.getProperty("file.lastModified"));

			Long snapshotLastModified = (Long) snapshot.get(iuFile);
			if (snapshotLastModified == null || !snapshotLastModified.equals(iuLastModified))
				toRemove.add(iu);
			else
				snapshot.remove(iuFile);
		}

		if (!toRemove.isEmpty()) {
			IInstallableUnit[] iusToRemove = (IInstallableUnit[]) toRemove.toArray(new IInstallableUnit[toRemove.size()]);
			metadataRepository.removeInstallableUnits(iusToRemove);
		}

		if (!snapshot.isEmpty()) {
			IInstallableUnit[] iusToAdd = generateIUs(snapshot.keySet());
			metadataRepository.addInstallableUnits(iusToAdd);
		}
	}

	private void synchronizeArtifactRepository() {
		List snapshot = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));

		IInstallableUnit[] ius = metadataRepository.getInstallableUnits(null);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			IArtifactKey[] artifacts = iu.getArtifacts();
			if (artifacts == null || artifacts.length == 0)
				continue;
			IArtifactKey artifact = artifacts[0];
			if (!snapshot.remove(artifact)) {
				File iuFile = new File(iu.getProperty("file.name"));
				IArtifactDescriptor descriptor = generateArtifactDescriptor(iuFile);
				if (descriptor != null)
					artifactRepository.addDescriptor(descriptor);
			}
		}

		for (Iterator it = snapshot.iterator(); it.hasNext();) {
			IArtifactKey key = (IArtifactKey) it.next();
			artifactRepository.removeDescriptor(key);
		}
	}

	private IArtifactDescriptor generateArtifactDescriptor(File bundle) {
		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundle);
		IArtifactKey key = MetadataGeneratorHelper.createEclipseArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		return MetadataGeneratorHelper.createArtifactDescriptor(key, bundle, true, false);
	}

	private IInstallableUnit[] generateIUs(Collection files) {
		List ius = new ArrayList();
		for (Iterator it = files.iterator(); it.hasNext();) {
			File bundle = (File) it.next();
			IInstallableUnit iu = generateIU(bundle);
			if (iu != null)
				ius.add(iu);
		}
		return (IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]);
	}

	private IInstallableUnit generateIU(File bundle) {
		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundle);
		if (bundleDescription == null)
			return null;
		Properties props = new Properties();
		props.setProperty("file.name", bundle.getAbsolutePath());
		props.setProperty("file.lastModified", Long.toString(bundle.lastModified()));
		IArtifactKey key = MetadataGeneratorHelper.createEclipseArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IInstallableUnit iu = MetadataGeneratorHelper.createEclipseIU(bundleDescription, (Map) bundleDescription.getUserObject(), false, key, props);
		return iu;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}
}
