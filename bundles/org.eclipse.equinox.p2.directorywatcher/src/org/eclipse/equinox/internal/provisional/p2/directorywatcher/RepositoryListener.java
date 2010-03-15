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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.generator.features.FeatureParser;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class RepositoryListener extends DirectoryChangeListener {
	private static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	private static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	private static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
	private static final String FILE_NAME = "file.name"; //$NON-NLS-1$
	private final IMetadataRepository metadataRepository;
	private final IArtifactRepository artifactRepository;
	private final BundleDescriptionFactory bundleDescriptionFactory;
	private final Map currentFiles = new HashMap();

	private Collection polledSeenFiles = new HashSet();
	private Collection polledIUsToAdd = new ArrayList();
	private Collection polledArtifactsToAdd = new ArrayList();

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
		File stateDir;
		if (repositoryFolder == null) {
			String stateDirName = "listener_" + repositoryName.hashCode(); //$NON-NLS-1$
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

		metadataRepository = initializeMetadataRepository(context, repositoryName, stateDirURL, hidden);
		artifactRepository = initializeArtifactRepository(context, repositoryName, stateDirURL, hidden);
		bundleDescriptionFactory = initializeBundleDescriptionFactory(context);
		synchronizeCurrentFiles();
	}

	public RepositoryListener(BundleContext context, IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
		this.artifactRepository = artifactRepository;
		this.metadataRepository = metadataRepository;
		bundleDescriptionFactory = initializeBundleDescriptionFactory(context);
		synchronizeCurrentFiles();
	}

	/**
	 * Broadcast events for any discovery sites associated with the feature
	 * so the repository managers add them to their list of known repositories.
	 */
	private void publishSites(Feature feature) {
		IProvisioningEventBus bus = (IProvisioningEventBus) ServiceHelper.getService(Activator.getContext(), IProvisioningEventBus.SERVICE_NAME);
		if (bus == null)
			return;
		URLEntry[] discoverySites = feature.getDiscoverySites();
		for (int i = 0; i < discoverySites.length; i++)
			publishSite(feature, bus, discoverySites[i].getURL(), false);
		String updateSite = feature.getUpdateSiteURL();
		if (updateSite != null)
			publishSite(feature, bus, updateSite, true);
	}

	/**
	 * Broadcast a discovery event for the given repository location.
	 */
	private void publishSite(Feature feature, IProvisioningEventBus bus, String locationString, boolean isEnabled) {
		try {
			URL location = new URL(locationString);
			bus.publishEvent(new RepositoryEvent(location, IRepository.TYPE_METADATA, RepositoryEvent.DISCOVERED, isEnabled));
			bus.publishEvent(new RepositoryEvent(location, IRepository.TYPE_ARTIFACT, RepositoryEvent.DISCOVERED, isEnabled));
		} catch (MalformedURLException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Feature references invalid site: " + feature.getId(), e)); //$NON-NLS-1$
		}
	}

	private BundleDescriptionFactory initializeBundleDescriptionFactory(BundleContext context) {
		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			throw new IllegalStateException(Messages.platformadmin_not_registered);
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(reference);
		if (platformAdmin == null)
			throw new IllegalStateException(Messages.platformadmin_not_registered);

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			context.ungetService(reference);
		}
	}

	private IArtifactRepository initializeArtifactRepository(BundleContext context, String repositoryName, URL stateDirURL, boolean hidden) {
		ServiceReference reference = context.getServiceReference(IArtifactRepositoryManager.class.getName());
		IArtifactRepositoryManager manager = null;
		if (reference != null)
			manager = (IArtifactRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_manager_not_registered);

		try {
			try {
				return manager.loadRepository(stateDirURL, null);
			} catch (ProvisionException e) {
				//fall through and create a new repository
			}
			try {
				String name = repositoryName;
				Map properties = new HashMap(1);
				if (hidden) {
					properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
					name = "artifact listener " + repositoryName; //$NON-NLS-1$
				}
				return manager.createRepository(stateDirURL, name, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			} catch (ProvisionException e) {
				LogHelper.log(e);
				throw new IllegalStateException(NLS.bind(Messages.failed_create_artifact_repo, stateDirURL));
			}
		} finally {
			context.ungetService(reference);
		}
	}

	private IMetadataRepository initializeMetadataRepository(BundleContext context, String repositoryName, URL stateDirURL, boolean hidden) {
		ServiceReference reference = context.getServiceReference(IMetadataRepositoryManager.class.getName());
		IMetadataRepositoryManager manager = null;
		if (reference != null)
			manager = (IMetadataRepositoryManager) context.getService(reference);
		if (manager == null)
			throw new IllegalStateException(Messages.metadata_repo_manager_not_registered);

		try {
			try {
				return manager.loadRepository(stateDirURL, null);
			} catch (ProvisionException e) {
				//fall through and create new repository
			}
			String name = repositoryName;
			Map properties = new HashMap(1);
			if (hidden) {
				properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				name = "metadata listener " + repositoryName; //$NON-NLS-1$
			}
			return manager.createRepository(stateDirURL, name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException(NLS.bind(Messages.failed_create_metadata_repo, stateDirURL));
		} finally {
			context.ungetService(reference);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#added(java.io.File)
	 */
	public boolean added(File file) {
		return process(file);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#changed(java.io.File)
	 */
	public boolean changed(File file) {
		// this sequence will trigger removal and then addition during stopPoll
		polledSeenFiles.remove(file);
		return process(file);
	}

	public boolean removed(File file) {
		// this file will get removed in stopPoll
		return currentFiles.containsKey(file);
	}

	private boolean process(File file) {
		boolean isDirectory = file.isDirectory();
		// is it a feature ?
		if (isDirectory && file.getParentFile() != null && file.getParentFile().getName().equals("features") && new File(file, "feature.xml").exists()) //$NON-NLS-1$ //$NON-NLS-2$)
			return processFeature(file);

		// is it a bundle ?
		if (isDirectory || file.getName().endsWith(".jar")) //$NON-NLS-1$
			return processBundle(file, isDirectory);

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.directorywatcher.IDirectoryChangeListener#removed(java.io.File)
	 */

	private boolean processBundle(File file, boolean isDirectory) {
		BundleDescription bundleDescription = bundleDescriptionFactory.getBundleDescription(file);
		if (bundleDescription == null)
			return false;

		String fileName = file.getAbsolutePath();
		String lastModified = Long.toString(file.lastModified());

		// Add Bundle IU
		Properties props = new Properties();
		props.setProperty(FILE_NAME, fileName);
		props.setProperty(FILE_LAST_MODIFIED, lastModified);

		IArtifactKey key = MetadataGeneratorHelper.createBundleArtifactKey(bundleDescription.getSymbolicName(), bundleDescription.getVersion().toString());
		IInstallableUnit[] ius = MetadataGeneratorHelper.createEclipseIU(bundleDescription, (Map) bundleDescription.getUserObject(), isDirectory, key, props);

		// see bug 222370
		// we only want to return the bundle IU so must exclude all fragment IUs
		IInstallableUnit bundleIU = null;
		for (int i = 0; i < ius.length; i++) {
			if (!ius[i].isFragment()) {
				bundleIU = ius[i];
				break;
			}
		}

		if (bundleIU == null) {
			if (ius.length == 0)
				return false;
			throw new IllegalStateException(Messages.multiple_bundle_ius);
		}
		polledIUsToAdd.add(bundleIU);

		// Add Bundle Artifact
		ArtifactDescriptor descriptor = new ArtifactDescriptor(MetadataGeneratorHelper.createArtifactDescriptor(key, file, true, false));
		try {
			descriptor.setRepositoryProperty(ARTIFACT_REFERENCE, file.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			// unexpected
			e.printStackTrace();
			return false;
		}
		if (isDirectory)
			descriptor.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());
		descriptor.setRepositoryProperty(FILE_NAME, fileName);
		descriptor.setRepositoryProperty(FILE_LAST_MODIFIED, lastModified);

		polledArtifactsToAdd.add(descriptor);
		return true;
	}

	private boolean processFeature(File file) {
		FeatureParser parser = new FeatureParser();
		Feature feature = parser.parse(file);
		if (feature == null)
			return false;

		publishSites(feature);

		String fileName = file.getAbsolutePath();
		String lastModified = Long.toString(file.lastModified());

		// Add Feature IUs
		Properties props = new Properties();
		props.setProperty(FILE_NAME, fileName);
		props.setProperty(FILE_LAST_MODIFIED, lastModified);

		IInstallableUnit featureIU = MetadataGeneratorHelper.createFeatureJarIU(feature, true, props);
		IInstallableUnit groupIU = MetadataGeneratorHelper.createGroupIU(feature, featureIU, props);

		polledIUsToAdd.add(featureIU);
		polledIUsToAdd.add(groupIU);

		// Add Feature Artifact
		IArtifactKey featureKey = MetadataGeneratorHelper.createFeatureArtifactKey(feature.getId(), feature.getVersion());
		ArtifactDescriptor descriptor = new ArtifactDescriptor(featureKey);

		try {
			descriptor.setRepositoryProperty(ARTIFACT_REFERENCE, file.toURL().toExternalForm());
		} catch (MalformedURLException e) {
			// unexpected
			e.printStackTrace();
			return false;
		}
		descriptor.setRepositoryProperty(ARTIFACT_FOLDER, Boolean.TRUE.toString());
		descriptor.setRepositoryProperty(FILE_NAME, fileName);
		descriptor.setRepositoryProperty(FILE_LAST_MODIFIED, lastModified);

		polledArtifactsToAdd.add(descriptor);
		return true;
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
		Long lastSeen = (Long) currentFiles.get(file);
		if (lastSeen != null)
			polledSeenFiles.add(file);
		return lastSeen;
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
		final Set removedFiles = new HashSet(currentFiles.keySet());
		removedFiles.removeAll(polledSeenFiles);
		polledSeenFiles.clear();

		if (removedFiles.isEmpty() && polledIUsToAdd.isEmpty() && polledArtifactsToAdd.isEmpty())
			return;

		if (metadataRepository != null)
			synchronizeMetadataRepository(removedFiles);

		if (artifactRepository != null)
			synchronizeArtifactRepository(removedFiles);

		synchronizeCurrentFiles();

		polledIUsToAdd.clear();
		polledArtifactsToAdd.clear();
	}

	private void synchronizeMetadataRepository(final Set removedFiles) {
		Query removeQuery = new Query() {
			public boolean isMatch(Object candidate) {
				if (!(candidate instanceof IInstallableUnit))
					return false;
				IInstallableUnit iu = (IInstallableUnit) candidate;
				String filename = iu.getProperty(FILE_NAME);
				if (filename == null) {
					String message = NLS.bind(Messages.filename_missing, "installable unit", iu.getId()); //$NON-NLS-1$
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, null));
					return false;
				}
				File iuFile = new File(filename);
				return removedFiles.contains(iuFile);
			}
		};
		metadataRepository.removeInstallableUnits(removeQuery, null);

		if (!polledIUsToAdd.isEmpty())
			metadataRepository.addInstallableUnits((IInstallableUnit[]) polledIUsToAdd.toArray(new IInstallableUnit[polledIUsToAdd.size()]));
	}

	private void synchronizeArtifactRepository(final Set removedFiles) {
		final List keys = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
		for (Iterator it = keys.iterator(); it.hasNext();) {
			IArtifactKey key = (IArtifactKey) it.next();
			IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
			for (int i = 0; i < descriptors.length; i++) {
				ArtifactDescriptor descriptor = (ArtifactDescriptor) descriptors[i];
				String filename = descriptor.getRepositoryProperty(FILE_NAME);
				if (filename == null) {
					String message = NLS.bind(Messages.filename_missing, "artifact", descriptor.getArtifactKey()); //$NON-NLS-1$
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, null));
				} else {
					File artifactFile = new File(filename);
					if (removedFiles.contains(artifactFile))
						artifactRepository.removeDescriptor(descriptor);
				}
			}
		}

		if (!polledArtifactsToAdd.isEmpty())
			artifactRepository.addDescriptors((IArtifactDescriptor[]) polledArtifactsToAdd.toArray(new IArtifactDescriptor[polledArtifactsToAdd.size()]));
	}

	private void synchronizeCurrentFiles() {
		currentFiles.clear();
		if (metadataRepository != null) {
			Collector ius = metadataRepository.query(InstallableUnitQuery.ANY, new Collector(), null);
			for (Iterator it = ius.iterator(); it.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) it.next();
				String filename = iu.getProperty(FILE_NAME);
				if (filename == null) {
					String message = NLS.bind(Messages.filename_missing, "installable unit", iu.getId()); //$NON-NLS-1$
					LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, null));
				} else {
					File iuFile = new File(filename);
					Long iuLastModified = new Long(iu.getProperty(FILE_LAST_MODIFIED));
					currentFiles.put(iuFile, iuLastModified);
				}
			}
		}

		if (artifactRepository != null) {
			final List keys = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
			for (Iterator it = keys.iterator(); it.hasNext();) {
				IArtifactKey key = (IArtifactKey) it.next();
				IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
				for (int i = 0; i < descriptors.length; i++) {
					ArtifactDescriptor descriptor = (ArtifactDescriptor) descriptors[i];
					String filename = descriptor.getRepositoryProperty(FILE_NAME);
					if (filename == null) {
						String message = NLS.bind(Messages.filename_missing, "artifact", descriptor.getArtifactKey()); //$NON-NLS-1$
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, message, null));
					} else {
						File artifactFile = new File(filename);
						Long artifactLastModified = new Long(descriptor.getRepositoryProperty(FILE_LAST_MODIFIED));
						currentFiles.put(artifactFile, artifactLastModified);
					}
				}
			}
		}
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}
}
