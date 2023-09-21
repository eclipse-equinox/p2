/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *   IBM Corporation - initial implementation and ideas 
 *   Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.update.Site;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.*;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;

public class RepositoryListener extends DirectoryChangeListener {
	public static final String ARTIFACT_FOLDER = "artifact.folder"; //$NON-NLS-1$
	public static final String ARTIFACT_REFERENCE = "artifact.reference"; //$NON-NLS-1$
	public static final String FILE_LAST_MODIFIED = "file.lastModified"; //$NON-NLS-1$
	public static final String FILE_NAME = "file.name"; //$NON-NLS-1$
	private final IMetadataRepository metadataRepository;
	private final CachingArtifactRepository artifactRepository;
	// at any point in time currentFiles is the list of files/dirs that the watcher has seen and 
	// believes to be on disk.
	private final Map<File, Long> currentFiles = new HashMap<>();
	private final Collection<File> polledSeenFiles = new HashSet<>();

	private EntryAdvice advice = new EntryAdvice();
	private PublisherInfo info;
	private IPublisherResult iusToAdd;
	private IPublisherResult iusToChange;

	/**
	 * Create a repository listener that watches the specified folder and generates repositories
	 * for its content.
	 * @param repositoryName the repository name to use for the repository
	 * @param properties the map of repository properties or <code>null</code>
	 */
	public RepositoryListener(String repositoryName, Map<String, String> properties) {
		URI location = getDefaultRepositoryLocation(this, repositoryName);
		metadataRepository = initializeMetadataRepository(repositoryName, location, properties);
		artifactRepository = initializeArtifactRepository(repositoryName, location, properties);
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
		info.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_NO_MD5);
	}

	protected CachingArtifactRepository initializeArtifactRepository(String name, URI repositoryLocation,
			Map<String, String> properties) {
		IArtifactRepository repository = (IArtifactRepository) initializeRepository(IArtifactRepositoryManager.class,
				name, repositoryLocation, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
		return repository == null ? null : new CachingArtifactRepository(repository);
	}

	protected IMetadataRepository initializeMetadataRepository(String name, URI repositoryLocation,
			Map<String, String> properties) {
		return (IMetadataRepository) initializeRepository(IMetadataRepositoryManager.class, name, repositoryLocation,
				IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
	}

	@Override
	public boolean added(File file) {
		return process(file, true);
	}

	@Override
	public boolean changed(File file) {
		return process(file, false);
	}

	@Override
	public boolean removed(File file) {
		// the IUs and artifacts associated with this file will get removed in stopPoll
		return currentFiles.containsKey(file);
	}

	private boolean process(File file, boolean isAddition) {
		boolean isDirectory = file.isDirectory();
		// is it a feature ?
		if (isDirectory && file.getParentFile() != null && file.getParentFile().getName().equals("features") //$NON-NLS-1$
				&& new File(file, "feature.xml").exists()) //$NON-NLS-1$ )
			return processFeature(file, isAddition);
		// could it be a bundle ?
		if (isDirectory || file.getName().endsWith(".jar")) //$NON-NLS-1$
			return processBundle(file, isDirectory, isAddition);
		return false;
	}

	private boolean processBundle(File file, boolean isDirectory, boolean isAddition) {
		BundleDescription bundleDescription = BundlesAction.createBundleDescriptionIgnoringExceptions(file);
		if (bundleDescription == null)
			return false;

		advice.setProperties(file, file.lastModified(), file.toURI());
		return publish(new BundlesAction(new BundleDescription[] { bundleDescription }), isAddition);
		// TODO see bug 222370
		// we only want to return the bundle IU so must exclude all fragment IUs
		// not sure if this is still relevant but we should investigate.
	}

	private boolean processFeature(File file, boolean isAddition) {
		String link = metadataRepository.getProperties().get(Site.PROP_LINK_FILE);
		advice.setProperties(file, file.lastModified(), file.toURI(), link);
		return publish(new FeaturesAction(new File[] { file }), isAddition);
	}

	private boolean publish(IPublisherAction action, boolean isAddition) {
		IPublisherResult result = isAddition ? iusToAdd : iusToChange;
		return action.perform(info, result, new NullProgressMonitor()).isOK();
	}

	@Override
	public boolean isInterested(File file) {
		return true;
	}

	@Override
	public Long getSeenFile(File file) {
		Long lastSeen = currentFiles.get(file);
		if (lastSeen != null)
			polledSeenFiles.add(file);
		return lastSeen;
	}

	@Override
	public void startPoll() {
		iusToAdd = new PublisherResult();
		iusToChange = new PublisherResult();
		synchronizeCurrentFiles();
	}

	@Override
	public void stopPoll() {
		final Set<File> filesToRemove = new HashSet<>(currentFiles.keySet());
		filesToRemove.removeAll(polledSeenFiles);
		polledSeenFiles.clear();

		synchronizeMetadataRepository(filesToRemove);
		synchronizeArtifactRepository(filesToRemove);
		iusToAdd = null;
		iusToChange = null;
	}

	/**
	 * Flush all the pending changes to the metadata repository.
	 */
	private void synchronizeMetadataRepository(final Collection<File> removedFiles) {
		if (metadataRepository == null)
			return;
		final Collection<IInstallableUnit> changes = iusToChange.getIUs(null, null);
		// first remove any IUs that have changed or that are associated with removed files
		if (!removedFiles.isEmpty() || !changes.isEmpty()) {
			metadataRepository.removeInstallableUnits(changes);

			// create a query that will identify all ius related to removed files.
			// We convert the java.io.File objects to Strings before doing the comparison
			// because when we have large numbers of files, the performance is much better.
			// See bug 324353.
			Collection<String> paths = new HashSet<>(removedFiles.size());
			for (File file : removedFiles)
				paths.add(file.getAbsolutePath());
			IQuery<IInstallableUnit> removeQuery = QueryUtil.createMatchQuery( //
					"$1.exists(x | properties[$0] == x)", FILE_NAME, paths); //$NON-NLS-1$
			IQueryResult<IInstallableUnit> toRemove = metadataRepository.query(removeQuery, null);
			metadataRepository.removeInstallableUnits(toRemove.toUnmodifiableSet());
		}
		// Then add all the new IUs as well as the new copies of the ones that have changed
		Collection<IInstallableUnit> additions = iusToAdd.getIUs(null, null);
		additions.addAll(changes);
		if (!additions.isEmpty())
			metadataRepository.addInstallableUnits(additions);
	}

	/**
	 * Here the artifacts have all been added to the artifact repo.  Remove the
	 * descriptors related to any file that has been removed and flush the repo
	 * to ensure that all the additions and removals have been completed.
	 */
	private void synchronizeArtifactRepository(final Collection<File> removedFiles) {
		if (artifactRepository == null)
			return;
		if (!removedFiles.isEmpty()) {
			IArtifactDescriptor[] descriptors = artifactRepository.descriptorQueryable()
					.query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, null).toArray(IArtifactDescriptor.class);
			for (IArtifactDescriptor d : descriptors) {
				SimpleArtifactDescriptor descriptor = (SimpleArtifactDescriptor) d;
				String filename = descriptor.getRepositoryProperty(FILE_NAME);
				if (filename == null) {
					if (Tracing.DEBUG) {
						String message = NLS.bind(Messages.filename_missing, "artifact", descriptor.getArtifactKey()); //$NON-NLS-1$
						LogHelper.log(new Status(IStatus.ERROR, Constants.BUNDLE_ID, message, null));
					}
				} else {
					File artifactFile = new File(filename);
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
			IQueryResult<IInstallableUnit> ius = metadataRepository.query(QueryUtil.createIUAnyQuery(), null);
			for (IInstallableUnit iu : ius) {
				String filename = iu.getProperty(FILE_NAME);
				if (filename == null) {
					if (Tracing.DEBUG) {
						String message = NLS.bind(Messages.filename_missing, "installable unit", iu.getId()); //$NON-NLS-1$
						LogHelper.log(new Status(IStatus.ERROR, Constants.BUNDLE_ID, message, null));
					}
				} else {
					File iuFile = new File(filename);
					Long iuLastModified = Long.valueOf(iu.getProperty(FILE_LAST_MODIFIED));
					currentFiles.put(iuFile, iuLastModified);
				}
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

	private static <T> IRepository<T> initializeRepository(Class<? extends IRepositoryManager<T>> repositoryManager,
			String name, URI repositoryLocation, String type, Map<String, String> properties) {

		BundleContext bundleContext = FrameworkUtil.getBundle(RepositoryListener.class).getBundleContext();
		IProvisioningAgent agent = ServiceHelper.getService(bundleContext, IProvisioningAgent.class);
		IRepositoryManager<T> manager = agent.getService(repositoryManager);
		if (manager == null) {
			throw new IllegalStateException(
					NLS.bind(Messages.repo_manager_not_registered, repositoryManager.getSimpleName()));
		}
		try {
			return manager.loadRepository(repositoryLocation, null);
		} catch (ProvisionException e) {
			// fall through and create a new repository
		}
		try {
			return manager.createRepository(repositoryLocation, name, type, properties);
		} catch (ProvisionException e) {
			LogHelper.log(e);
			throw new IllegalStateException(
					NLS.bind(Messages.failed_create_repo, repositoryManager.getSimpleName(), repositoryLocation));
		}
	}

	private static URI getDefaultRepositoryLocation(Object object, String repositoryName) {
		Bundle bundle = FrameworkUtil.getBundle(object.getClass());
		BundleContext context = bundle.getBundleContext();
		File base = context.getDataFile(""); //$NON-NLS-1$
		File result = new File(base, "listener_" + repositoryName.hashCode()); //$NON-NLS-1$
		result.mkdirs();
		return result.toURI();
	}

}
