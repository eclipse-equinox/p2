/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype Inc - ongoing development
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

public class RecreateRepositoryApplication extends AbstractApplication {
	static final private String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$
	private URI repoLocation;
	private String repoName = null;
	boolean removeArtifactRepo = true;
	private Map<String, String> repoProperties = null;
	private Map<IArtifactKey, IArtifactDescriptor[]> repoMap = null;

	@Override
	public IStatus run(IProgressMonitor monitor) throws ProvisionException {
		try {
			IArtifactRepository repository = initialize(monitor);
			removeRepository(repository, monitor);
			recreateRepository(monitor);
		} finally {
			if (removeArtifactRepo) {
				IArtifactRepositoryManager repositoryManager = getArtifactRepositoryManager();
				repositoryManager.removeRepository(repoLocation);
			}
		}

		return Status.OK_STATUS;
	}

	public void setArtifactRepository(URI repository) {
		this.repoLocation = repository;
	}

	private IArtifactRepository initialize(IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager repositoryManager = getArtifactRepositoryManager();
		removeArtifactRepo = !repositoryManager.contains(repoLocation);

		IArtifactRepository repository = repositoryManager.loadRepository(repoLocation, IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, monitor);

		if (repository == null || !repository.isModifiable())
			throw new ProvisionException(NLS.bind(Messages.exception_destinationNotModifiable, repository.getLocation()));
		if (!(repository instanceof IFileArtifactRepository))
			throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));

		repoName = repository.getName();
		repoProperties = repository.getProperties();

		repoMap = new HashMap<>();
		IQueryResult<IArtifactKey> keys = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
		for (Iterator<IArtifactKey> iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			repoMap.put(key, descriptors);
		}

		return repository;
	}

	private void removeRepository(IArtifactRepository repository, IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		manager.removeRepository(repository.getLocation());

		boolean compressed = Boolean.parseBoolean(repoProperties.get(IRepository.PROP_COMPRESSED));
		URI realLocation = SimpleArtifactRepository.getActualLocation(repository.getLocation(), compressed);
		File realFile = URIUtil.toFile(realLocation);
		if (!realFile.exists() || !realFile.delete())
			throw new ProvisionException(NLS.bind(Messages.exception_unableToRemoveRepo, realFile.toString()));
	}

	private void recreateRepository(IProgressMonitor monitor) throws ProvisionException {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();

		//add pack200 mappings, the existing repoProperties is not modifiable 
		Map<String, String> newProperties = new HashMap<>(repoProperties);
		newProperties.put(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
		IArtifactRepository repository = manager.createRepository(repoLocation, repoName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, newProperties);
		if (!(repository instanceof IFileArtifactRepository))
			throw new ProvisionException(NLS.bind(Messages.exception_notLocalFileRepo, repository.getLocation()));

		IFileArtifactRepository simple = (IFileArtifactRepository) repository;
		for (IArtifactKey key : repoMap.keySet()) {
			IArtifactDescriptor[] descriptors = repoMap.get(key);

			String unpackedSize = null;
			File packFile = null;
			Set<File> files = new HashSet<>();
			for (IArtifactDescriptor descriptor : descriptors) {
				File artifactFile = simple.getArtifactFile(descriptor);
				files.add(artifactFile);

				String size = Long.toString(artifactFile.length());

				ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
				newDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, size);
				newDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, size);

				Map<String, String> checksums = new HashMap<>();
				List<String> checksumsToSkip = Collections.emptyList();
				IStatus status = ChecksumUtilities.calculateChecksums(artifactFile, checksums, checksumsToSkip);
				if (!status.isOK())
					// TODO handle errors in some way
					LogHelper.log(status);

				Map<String, String> checksumsToProperties = ChecksumUtilities.checksumsToProperties(IArtifactDescriptor.DOWNLOAD_CHECKSUM, checksums);
				newDescriptor.addProperties(checksumsToProperties);

				File temp = new File(artifactFile.getParentFile(), artifactFile.getName() + ".pack.gz"); //$NON-NLS-1$
				if (temp.exists()) {
					packFile = temp;
					unpackedSize = size;
				}

				repository.addDescriptor(newDescriptor, null);
			}
			if (packFile != null && !files.contains(packFile) && packFile.length() > 0) {
				ArtifactDescriptor packDescriptor = createPack200ArtifactDescriptor(key, packFile, unpackedSize);
				repository.addDescriptor(packDescriptor, null);
			}
		}
	}

	private ArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File packFile, String installSize) {

		if (packFile != null && packFile.exists()) {
			ArtifactDescriptor result = new ArtifactDescriptor(key);
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(packFile.length()));
			IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
			result.setProcessingSteps(steps);
			result.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
			return result;
		}
		return null;
	}
}
