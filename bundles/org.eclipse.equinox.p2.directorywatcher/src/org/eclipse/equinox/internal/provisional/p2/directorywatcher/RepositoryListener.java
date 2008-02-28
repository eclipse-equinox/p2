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
	private long lastModifed;

	public RepositoryListener(BundleContext context, String repositoryName) {

		this.repositoryName = repositoryName;
		String stateDirName = "listener_" + repositoryName;
		File stateDir = context.getDataFile(stateDirName);
		stateDir.mkdirs();

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
				IArtifactRepository repository = manager.createRepository(stateDirURL, "artifact listener " + repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
				repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				return repository;
			} catch (ProvisionException e) {
				LogHelper.log(e);
				throw new IllegalStateException("Couldn't create listener artifact repository for: " + stateDirURL);
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
			IMetadataRepository repository = manager.createRepository(stateDirURL, "Metadata listener " + repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
			repository.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
			return repository;
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException("Couldn't create listener metadata repository for: " + stateDirURL);
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

	IArtifactDescriptor generateArtifactDescriptor(File bundle) {
		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundle);
		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IArtifactDescriptor basicDescriptor = MetadataGeneratorHelper.createArtifactDescriptor(key, bundle, true, false);

		ArtifactDescriptor pathDescriptor = new ArtifactDescriptor(basicDescriptor);
		try {
			pathDescriptor.setRepositoryProperty("artifact.reference", bundle.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			// unexpected
			e.printStackTrace();
			return null;
		}
		if (bundle.isDirectory())
			pathDescriptor.setRepositoryProperty("artifact.folder", "true");

		return pathDescriptor;
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
			String parentName = candidate.getParent();
			if (parentName != null && parentName.equals("features")) {
				IInstallableUnit[] featureIUs = generateFeatureIUs(candidate, props);
				if (featureIUs != null)
					ius.add(Arrays.asList(featureIUs));
			} else {
				IInstallableUnit bundleIU = generateBundleIU(candidate, props);
				if (bundleIU != null)
					ius.add(bundleIU);
			}
		}
		return (IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]);
	}

	private IInstallableUnit[] generateFeatureIUs(File featureFile, Properties props) {

		FeatureParser parser = new FeatureParser();
		Feature feature = parser.parse(featureFile);

		IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true);
		IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU);

		if (!Boolean.getBoolean("org.eclipse.p2.update.compatibility")) //$NON-NLS-1$
			return new IInstallableUnit[] {groupIU};

		return new IInstallableUnit[] {featureIU, groupIU};
	}

	private IInstallableUnit generateBundleIU(File bundleFile, Properties props) {

		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(bundleFile);
		if (bundleDescription == null)
			return null;

		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IInstallableUnit iu = MetadataGeneratorHelper.createEclipseIU(bundleDescription, (Map) bundleDescription.getUserObject(), false, key, props);
		return iu;
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
