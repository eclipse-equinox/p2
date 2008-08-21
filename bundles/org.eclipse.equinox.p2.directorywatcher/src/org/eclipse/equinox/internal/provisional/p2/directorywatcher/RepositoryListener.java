/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM Corporation - initial implementation and ideas 
 *   Code 9 - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;

public class RepositoryListener extends DirectoryChangeListener {
	public static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	public static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	public static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
	public static final String FILE_NAME = "file.name"; //$NON-NLS-1$
	private final IMetadataRepository metadataRepository;
	private final CachingArtifactRepository artifactRepository;
	// at any point in time currentFiles is the list of files/dirs that the watcher has seen and 
	// believes to be on disk.
	private Map currentFiles;
	private Collection filesToRemove;
	private EntryAdvice advice = new EntryAdvice();
	private PublisherInfo info;
	private IPublisherResult iusToAdd;
	private IPublisherResult iusToChange;

	/**
	 * Create a repository listener that watches the specified folder and generates repositories
	 * for its content.
	 * @param repositoryName the repository name to use for the repository
	 * @param hidden <code>true</code> if the repository should be hidden, <code>false</code> if not.
	 */
	public RepositoryListener(String repositoryName, boolean hidden) {
		URL location = Activator.getDefaultRepositoryLocation(this, repositoryName);
		metadataRepository = initiailzeMetadataRepository(repositoryName, location, hidden);
		artifactRepository = initializeArtifactRepository(repositoryName, location, hidden);
		initializePublisher();
	}

	public RepositoryListener(IMetadataRepository metadataRepository, IArtifactRepository artifactRepository) {
		this.artifactRepository = new CachingArtifactRepository(artifactRepository);
		this.metadataRepository = metadataRepository;
		initializePublisher();
	}

	private void initializePublisher() {
		info = new PublisherInfo();
		info.setArtifactRepository(artifactRepository);
		info.setMetadataRepository(metadataRepository);
		info.addAdvice(advice);
		info.setArtifactOptions(IPublisherInfo.A_INDEX);
	}

	protected CachingArtifactRepository initializeArtifactRepository(String repositoryName, URL repositoryLocation, boolean hidden) {
		IArtifactRepositoryManager manager = Activator.getArtifactRepositoryManager();
		if (manager == null)
			throw new IllegalStateException(Messages.artifact_repo_manager_not_registered);

		try {
			IArtifactRepository result = manager.loadRepository(repositoryLocation, null);
			return result == null ? null : new CachingArtifactRepository(result);
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
			IArtifactRepository result = manager.createRepository(repositoryLocation, name, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			return result == null ? null : new CachingArtifactRepository(result);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException(NLS.bind(Messages.failed_create_artifact_repo, repositoryLocation));
		}
	}

	protected IMetadataRepository initiailzeMetadataRepository(String repositoryName, URL repositoryLocation, boolean hidden) {
		IMetadataRepositoryManager manager = Activator.getMetadataRepositoryManager();
		if (manager == null)
			throw new IllegalStateException(Messages.metadata_repo_manager_not_registered);

		try {
			return manager.loadRepository(repositoryLocation, null);
		} catch (ProvisionException e) {
			//fall through and create new repository
		}
		try {
			String name = repositoryName;
			Map properties = new HashMap(1);
			if (hidden) {
				properties.put(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());
				name = "metadata listener " + repositoryName; //$NON-NLS-1$
			}
			return manager.createRepository(repositoryLocation, name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException(NLS.bind(Messages.failed_create_metadata_repo, repositoryLocation));
		}
	}

	public boolean added(File file) {
		return process(file, true);
	}

	public boolean changed(File file) {
		return process(file, false);
	}

	public boolean removed(File file) {
		filesToRemove.add(file);
		return true;
	}

	private boolean process(File file, boolean isAddition) {
		boolean isDirectory = file.isDirectory();
		// is it a feature ?
		if (isDirectory && file.getParentFile() != null && file.getParentFile().getName().equals("features") && new File(file, "feature.xml").exists()) //$NON-NLS-1$ //$NON-NLS-2$)
			return processFeature(file, isAddition);
		// could it be a bundle ?
		if (isDirectory || file.getName().endsWith(".jar")) //$NON-NLS-1$
			return processBundle(file, isDirectory, isAddition);
		return false;
	}

	private boolean processBundle(File file, boolean isDirectory, boolean isAddition) {
		BundleDescription bundleDescription = BundlesAction.createBundleDescription(file);
		if (bundleDescription == null)
			return false;
		try {
			advice.setProperties(file, file.lastModified(), file.toURL());
		} catch (MalformedURLException e) {
			// should never happen
		}
		return publish(new BundlesAction(new BundleDescription[] {bundleDescription}), isAddition);
		// TODO see bug 222370
		// we only want to return the bundle IU so must exclude all fragment IUs
		// not sure if this is still relevant but we should investigate.
	}

	private boolean processFeature(File file, boolean isAddition) {
		try {
			advice.setProperties(file, file.lastModified(), file.toURL());
		} catch (MalformedURLException e) {
			// should never happen
		}
		return publish(new FeaturesAction(new File[] {file}), isAddition);
	}

	private boolean publish(IPublisherAction action, boolean isAddition) {
		IPublisherResult result = isAddition ? iusToAdd : iusToChange;
		return action.perform(info, result).isOK();
	}

	public boolean isInterested(File file) {
		return true;
	}

	public Long getSeenFile(File file) {
		return (Long) currentFiles.get(file);
	}

	public void startPoll() {
		filesToRemove = new HashSet();
		iusToAdd = new PublisherResult();
		iusToChange = new PublisherResult();
		// TODO investigate why we do this here?  Suspect it is to clean up the currentFiles collection
		// for removed entries.  This may be a performance opportunity
		currentFiles = new HashMap();
		synchronizeCurrentFiles();
	}

	public void stopPoll() {
		synchronizeMetadataRepository(filesToRemove);
		synchronizeArtifactRepository(filesToRemove);
		filesToRemove.clear();
		iusToAdd = null;
		iusToChange = null;
		currentFiles = null;
	}

	/**
	 * Flush all the pending changes to the metadata repository.
	 */
	private void synchronizeMetadataRepository(final Collection removedFiles) {
		if (metadataRepository == null)
			return;
		final Collection changes = iusToChange.getIUs(null, null);
		// first remove any IUs that have changed or that are associated with removed files
		if (!removedFiles.isEmpty() || !changes.isEmpty()) {
			// create a query that will identify all ius related to removed files or ius that have changed
			Query removeQuery = new Query() {
				public boolean isMatch(Object candidate) {
					if (!(candidate instanceof IInstallableUnit))
						return false;
					IInstallableUnit iu = (IInstallableUnit) candidate;
					if (changes.contains(iu))
						return true;
					File iuFile = new File(iu.getProperty(FILE_NAME));
					return removedFiles.contains(iuFile);
				}
			};
			metadataRepository.removeInstallableUnits(removeQuery, null);
		}
		// Then add all the new IUs as well as the new copies of the ones that have changed
		Collection additions = iusToAdd.getIUs(null, null);
		additions.addAll(changes);
		if (!additions.isEmpty())
			metadataRepository.addInstallableUnits((IInstallableUnit[]) additions.toArray(new IInstallableUnit[additions.size()]));
	}

	/**
	 * Here the artifacts have all been added to the artifact repo.  Remove the
	 * descriptors related to any file that has been removed and flush the repo
	 * to ensure that all the additions and removals have been completed.
	 */
	private void synchronizeArtifactRepository(final Collection removedFiles) {
		if (artifactRepository == null)
			return;
		if (!removedFiles.isEmpty()) {
			final List keys = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
			for (Iterator it = keys.iterator(); it.hasNext();) {
				IArtifactKey key = (IArtifactKey) it.next();
				IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
				for (int i = 0; i < descriptors.length; i++) {
					ArtifactDescriptor descriptor = (ArtifactDescriptor) descriptors[i];
					File artifactFile = new File(descriptor.getRepositoryProperty(FILE_NAME));
					if (removedFiles.contains(artifactFile))
						artifactRepository.removeDescriptor(descriptor);
				}
			}
		}
		artifactRepository.save();
	}

	/**
	 * Prime the list of current files that the listener knows about.  This traverses the 
	 * repos and looks for the related filename and modified timestamp information.
	 */
	private void synchronizeCurrentFiles() {
		currentFiles.clear();
		if (metadataRepository != null) {
			Collector ius = metadataRepository.query(InstallableUnitQuery.ANY, new Collector(), null);
			for (Iterator it = ius.iterator(); it.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) it.next();
				File iuFile = new File(iu.getProperty(FILE_NAME));
				Long iuLastModified = new Long(iu.getProperty(FILE_LAST_MODIFIED));
				currentFiles.put(iuFile, iuLastModified);
			}
		}
		//
		//		// TODO  should we be doing this for the artifact repo?  the metadata repo should
		//		// be the main driver here.
		//		if (artifactRepository != null) {
		//			final List keys = new ArrayList(Arrays.asList(artifactRepository.getArtifactKeys()));
		//			for (Iterator it = keys.iterator(); it.hasNext();) {
		//				IArtifactKey key = (IArtifactKey) it.next();
		//				IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
		//				for (int i = 0; i < descriptors.length; i++) {
		//					ArtifactDescriptor descriptor = (ArtifactDescriptor) descriptors[i];
		//					File artifactFile = new File(descriptor.getRepositoryProperty(FILE_NAME));
		//					Long artifactLastModified = new Long(descriptor.getRepositoryProperty(FILE_LAST_MODIFIED));
		//					currentFiles.put(artifactFile, artifactLastModified);
		//				}
		//			}
		//		}
	}

	public IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	public IArtifactRepository getArtifactRepository() {
		return artifactRepository;
	}
}
