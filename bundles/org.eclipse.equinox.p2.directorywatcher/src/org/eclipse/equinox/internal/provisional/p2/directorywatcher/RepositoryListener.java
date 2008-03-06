/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class RepositoryListener extends DirectoryChangeListener {

	private final IMetadataRepository metadataRepository;
	private final IArtifactRepository artifactRepository;
	private final BundleDescriptionFactory bundleDescriptionFactory;
	private final Map currentFiles = new HashMap();
	private final String repositoryName;
	private final boolean hidden;
	private long lastModifed;

	/**
	 * Create a repository listener that watches the specified folder and generates repositories
	 * for its content.
	 * @param context the bundle context
	 * @param repositoryName the repository name to use for the repository
	 * @param repositoryFolder the target folder for the repository, or <code>null</code> if a folder based on the
	 * bundle's data location should be used.
	 * @param hidden <code>true</code> if the repository should be hidden, <code>false</code> if not.
	 */
	public RepositoryListener(BundleContext context, String repositoryName, File repositoryFolder, boolean hidden) {

		this.repositoryName = repositoryName;
		this.hidden = hidden;
		File stateDir;
		if (repositoryFolder == null) {
			String stateDirName = "listener_" + repositoryName;
			stateDir = context.getDataFile(stateDirName);
			stateDir.mkdirs();
		} else {
			stateDir = repositoryFolder;
		}

		URL stateDirURL;
		try {
			stateDirURL = stateDir.toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e.getMessage());
		}

		metadataRepository = initializeMetadataRepository(context, stateDirURL);
		artifactRepository = initializeArtifactRepository(context, stateDirURL);
		bundleDescriptionFactory = initializeBundleDescriptionFactory(context);
	}

	public RepositoryListener(BundleContext context, String string) {
		this(context, string, null, false);
	}

	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {

		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException("PlatformAdmin not registered.");
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException("PlatformAdmin not registered.");

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

	private IArtifactRepository initializeArtifactRepository(BundleContext context, URL stateDirURL) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		IArtifactRepositoryManager manager = null;
		if (reference != null)
			manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("ArtifactRepositoryManager not registered."); //$NON-NLS-1$

		try {
			try {
				return manager.loadRepository(stateDirURL, null);
			} catch (ProvisionException e) {
				//fall through and create a new repository
			}
			try {
				IArtifactRepository repository;
				if (hidden) {
					repository = manager.createRepository(stateDirURL, "artifact listener " + repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
					manager.addRepository(repository.getLocation());
					repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				} else {
					repository = manager.createRepository(stateDirURL, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
					manager.addRepository(repository.getLocation());
				}
				return repository;
			} catch (ProvisionException e) {
				LogHelper.log(e);
				throw new IllegalStateException("Couldn't create artifact repository for: " + stateDirURL);
			}
		} finally {
			context.ungetService(reference);
		}
	}

	private IMetadataRepository initializeMetadataRepository(BundleContext context, URL stateDirURL) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException("MetadataRepositoryManager not registered.");

		try {
			try {
				return manager.loadRepository(stateDirURL, null);
			} catch (ProvisionException e) {
				//fall through and create new repository
			}
			IMetadataRepository repository;
			if (hidden) {
				repository = manager.createRepository(stateDirURL, "Metadata listener " + repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			} else {
				repository = manager.createRepository(stateDirURL, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
			}
			return repository;
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException("Couldn't create metadata repository for: " + stateDirURL);
		} finally {
			context.ungetService(reference);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		if (isInteresting(file))
			currentFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		if (isInteresting(file))
			currentFiles.put(file, new Long(file.lastModified()));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
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
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener#isInterested(java.io.File)
	 */
	public boolean isInterested(File file) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#getSeenFile(java.io.File)
	 */
	public Long getSeenFile(File file) {
		return (Long) currentFiles.get(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#startPoll()
	 */
	public void startPoll() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#stopPoll()
	 */
	public void stopPoll() {
		synchronizeMetadataRepository();
		synchronizeArtifactRepository();
	}

	private void synchronizeMetadataRepository() {
		boolean modified = false;
		final Map snapshot = new HashMap(currentFiles);
		Query removeQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (!(candidate instanceof IInstallableUnit))
					return false;
				IInstallableUnit iu = (IInstallableUnit) candidate;
				File iuFile = new File(iu.getProperty("file.name")); //$NON-NLS-1$
				Long iuLastModified = new Long(iu.getProperty("file.lastModified")); //$NON-NLS-1$
				Long snapshotLastModified = (Long) snapshot.get(iuFile);
				if (snapshotLastModified == null || !snapshotLastModified.equals(iuLastModified))
					return true;
				snapshot.remove(iuFile);
				return false;
			}
		};
		if (metadataRepository.removeInstallableUnits(removeQuery, null))
			modified = true;

		if (!snapshot.isEmpty()) {
			modified = true;
			IInstallableUnit[] iusToAdd = generateIUs(snapshot.keySet(), metadataRepository.getLocation().toExternalForm());
			metadataRepository.addInstallableUnits(iusToAdd);
		}
		if (modified)
			lastModifed = System.currentTimeMillis();
	}

	private void synchronizeArtifactRepository() {
		final boolean[] modified = {false};
		final List snapshot = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
		Collector collector = new Collector() {
			public boolean accept(Object object) {
				IInstallableUnit iu = (IInstallableUnit) object;
				IArtifactKey[] artifacts = iu.getArtifacts();
				if (artifacts == null || artifacts.length == 0)
					return true;
				IArtifactKey artifact = artifacts[0];
				if (!snapshot.remove(artifact)) {
					File iuFile = new File(iu.getProperty("file.name"));
					IArtifactDescriptor descriptor = generateArtifactDescriptor(iuFile);
					if (descriptor != null) {
						artifactRepository.addDescriptor(descriptor);
						modified[0] = true;
					}
				}
				return true;
			}
		};
		metadataRepository.query(InstallableUnitQuery.ANY, collector, null);

		for (Iterator it = snapshot.iterator(); it.hasNext();) {
			IArtifactKey key = (IArtifactKey) it.next();
			artifactRepository.removeDescriptor(key);
			modified[0] = true;
		}

		if (modified[0])
			lastModifed = System.currentTimeMillis();
	}

	IArtifactDescriptor generateArtifactDescriptor(File candidate) {

		IArtifactDescriptor basicDescriptor = generateBasicDescriptor(candidate);
		ArtifactDescriptor pathDescriptor = new ArtifactDescriptor(basicDescriptor);
		try {
			pathDescriptor.setRepositoryProperty("artifact.reference", candidate.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			// unexpected
			e.printStackTrace();
			return null;
		}
		if (candidate.isDirectory())
			pathDescriptor.setRepositoryProperty("artifact.folder", "true");

		return pathDescriptor;
	}

	private IArtifactDescriptor generateBasicDescriptor(File candidate) {
		// feature check
		File parent = candidate.getParentFile();
		if (parent != null && parent.getName().equals("features")) {
			FeatureParser parser = new FeatureParser();
			Feature feature = parser.parse(candidate);
			IArtifactKey featureKey = MetadataGeneratorHelper.createFeatureArtifactKey(feature.getId(), feature.getVersion());
			return new ArtifactDescriptor(featureKey);
		}

		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(candidate);
		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		return MetadataGeneratorHelper.createArtifactDescriptor(key, candidate, true, false);
	}

	private IInstallableUnit[] generateIUs(Collection files, String repositoryId) {
		List ius = new ArrayList();
		for (Iterator it = files.iterator(); it.hasNext();) {
			File candidate = (File) it.next();

			Properties props = new Properties();
			props.setProperty("repository.id", repositoryId);
			props.setProperty("file.name", candidate.getAbsolutePath());
			props.setProperty("file.lastModified", Long.toString(candidate.lastModified()));

			if (candidate.isDirectory() && candidate.getName().equals("eclipse"))
				continue;

			// feature check
			File parent = candidate.getParentFile();
			if (parent != null && parent.getName().equals("features")) {
				IInstallableUnit[] featureIUs = generateFeatureIUs(candidate, props);
				if (featureIUs != null)
					ius.addAll(Arrays.asList(featureIUs));
			} else {
				IInstallableUnit[] bundleIUs = generateBundleIU(candidate, props);
				if (bundleIUs != null) {
					for (int i = 0; i < bundleIUs.length; i++) {
						ius.add(bundleIUs[i]);
					}
				}
			}
		}
		return (IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]);
	}

	private IInstallableUnit[] generateFeatureIUs(File featureFile, Properties props) {

		FeatureParser parser = new FeatureParser();
		Feature feature = parser.parse(featureFile);

		IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true, props);
		IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU, props);

		return new IInstallableUnit[] {featureIU, groupIU};
	}

	private IInstallableUnit[] generateBundleIU(File bundleFile, Properties props) {

		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundleFile);
		if (bundleDescription == null)
			return null;

		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IInstallableUnit[] ius = MetadataGeneratorHelper.createEclipseIU(bundleDescription, (Map) bundleDescription.getUserObject(), false, key, props);
		return ius;
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}

	public long getLastModified() {
		return lastModifed;
	}
}
