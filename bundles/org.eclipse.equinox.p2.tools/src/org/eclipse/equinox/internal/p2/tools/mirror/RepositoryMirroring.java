/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 * 	    IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tools.mirror;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.p2.metadata.query.IQuery;

/**
 * A utility class that performs mirroring of metadata and artifacts between repositories.
 */
public class RepositoryMirroring {

	private IMetadataRepository metadataSourceRepository;
	private IMetadataRepository metadataDestinationRepository;
	private IArtifactRepository artifactSourceRepository;
	private IArtifactRepository artifactDestinationRepository;
	private boolean referencedIUs = false;
	private boolean validArtifactRepos = false;
	private boolean validMetadataRepos = false;
	private boolean mirrorArtifactsWithMetadata = false;
	private boolean raw = false;
	private boolean overwrite = false;
	private boolean verbose = false;
	private boolean compressed = false;

	public RepositoryMirroring(URI metadataSourceLocation, URI metadataDestinationLocation, URI artifactSourceLocation, URI artifactDestinationLocation, boolean overwrite, boolean compressed) throws ProvisionException {
		this.overwrite = overwrite;
		this.compressed = compressed;
		if (metadataSourceLocation != null && metadataDestinationLocation != null) {
			MetadataRepositoryManager metadataRepoManager = new MetadataRepositoryManager();
			metadataSourceRepository = metadataRepoManager.loadRepository(metadataSourceLocation, null);
			metadataRepoManager.removeRepository(metadataSourceLocation);
			metadataDestinationRepository = initializeMetadataDestination(metadataRepoManager, metadataDestinationLocation);
			validMetadataRepos = validateMetadataRepositories();
		}
		if (artifactSourceLocation != null && artifactDestinationLocation != null) {
			ArtifactRepositoryManager artifactRepoManager = new ArtifactRepositoryManager();
			artifactSourceRepository = artifactRepoManager.loadRepository(artifactSourceLocation, null);
			artifactRepoManager.removeRepository(artifactSourceLocation);
			artifactDestinationRepository = initializeArtifactDestination(artifactRepoManager, artifactDestinationLocation);
			validArtifactRepos = validateArtifactRepositories();
		}
	}

	public void mirror(String[] iuSpecs, String[] artifactSpecs) throws ProvisionException {
		mirrorArtifactsWithMetadata = validArtifactRepos && artifactSpecs != null && artifactSpecs.length == 0 && iuSpecs != null;
		if (validMetadataRepos && iuSpecs != null)
			mirrorMetadata(iuSpecs);
		if (validArtifactRepos && !mirrorArtifactsWithMetadata && artifactSpecs != null)
			mirrorArtifacts(artifactSpecs, raw);
	}

	public void mirrorMetadata(String[] iuSpecs) throws ProvisionException {
		if (iuSpecs.length == 0)
			mirrorMetadata(InstallableUnitQuery.ANY);
		else {
			VersionRangedName[] iuRanges = new VersionRangedName[iuSpecs.length];
			for (int i = 0; i < iuSpecs.length; i++)
				iuRanges[i] = VersionRangedName.parse(iuSpecs[i]);
			mirrorMetadata(new RangeQuery(iuRanges));
		}
	}

	public void mirrorMetadata(IQuery query) throws ProvisionException {
		Collector result = metadataSourceRepository.query(query, new Collector(), null);
		mirrorMetadata((IInstallableUnit[]) result.toArray(IInstallableUnit.class));
	}

	private void mirrorMetadata(IInstallableUnit[] ius) throws ProvisionException {
		if (referencedIUs)
			ius = addTransitiveIUs(metadataSourceRepository, ius);
		for (int i = 0; i < ius.length; i++) {
			IInstallableUnit iu = ius[i];
			if (verbose)
				System.out.println("Mirroring IU: " + iu);
			if (mirrorArtifactsWithMetadata)
				mirrorArtifacts(iu.getArtifacts(), raw);
		}
		metadataDestinationRepository.addInstallableUnits(ius);
	}

	private void mirrorArtifact(IArtifactDescriptor descriptor) throws ProvisionException {
		IArtifactDescriptor newDescriptor = raw ? descriptor : new ArtifactDescriptor(descriptor);
		if (artifactDestinationRepository.contains(descriptor))
			return;
		OutputStream repositoryStream = null;
		try {
			repositoryStream = artifactDestinationRepository.getOutputStream(newDescriptor);
			if (repositoryStream == null)
				return;
			if (verbose)
				System.out.println("Mirroring artifact: " + descriptor);
			// TODO Is that ok to ignore the result?
			artifactSourceRepository.getRawArtifact(descriptor, repositoryStream, new NullProgressMonitor());
		} finally {
			if (repositoryStream != null)
				try {
					repositoryStream.close();
				} catch (IOException e) {
					// TODO Is that ok to ignore the exception
					e.printStackTrace();
				}
		}
	}

	private void mirrorArtifacts(IArtifactKey[] keys, boolean raw) throws ProvisionException {
		for (int i = 0; i < keys.length; i++) {
			IArtifactKey key = keys[i];
			IArtifactDescriptor[] descriptors = artifactSourceRepository.getArtifactDescriptors(key);
			for (int j = 0; j < descriptors.length; j++)
				mirrorArtifact(descriptors[j]);
		}
	}

	private void mirrorArtifacts(String[] artifactSpecs, boolean raw) throws ProvisionException {
		IArtifactKey[] keys;
		if (artifactSpecs == null)
			return;
		if (artifactSpecs.length == 0)
			keys = artifactSourceRepository.getArtifactKeys();
		else {
			keys = new ArtifactKey[artifactSpecs.length];
			for (int i = 0; i < artifactSpecs.length; i++) {
				keys[i] = ArtifactKey.parse(artifactSpecs[i]);
			}
		}
		mirrorArtifacts(keys, raw);
	}

	protected IInstallableUnit[] addTransitiveIUs(IMetadataRepository source, IInstallableUnit[] ius) {
		// TODO Here we should create a profile from the source repo and discover all the 
		// IUs that are needed to support the given ius.  For now just assume that the 
		// given ius are enough.
		return ius;
	}

	private boolean validateMetadataRepositories() {
		if (metadataSourceRepository == null)
			throw new IllegalStateException("Source metadata repository is null."); //$NON-NLS-1$
		if (metadataDestinationRepository == null)
			throw new IllegalStateException("Destination metadata repository is null."); //$NON-NLS-1$
		if (!metadataDestinationRepository.isModifiable())
			throw new IllegalStateException("Destination metadata repository must be modifiable: " + metadataDestinationRepository.getLocation()); //$NON-NLS-1$
		return true;
	}

	private boolean validateArtifactRepositories() {
		if (artifactSourceRepository == null)
			throw new IllegalStateException("Source artifact repository is null."); //$NON-NLS-1$
		if (artifactDestinationRepository == null)
			throw new IllegalStateException("Destination artifact repository is null."); //$NON-NLS-1$
		if (!artifactDestinationRepository.isModifiable())
			throw new IllegalStateException("Destination artifact repository must be modifiable: " + artifactDestinationRepository.getLocation()); //$NON-NLS-1$
		return true;
	}

	private IMetadataRepository initializeMetadataDestination(MetadataRepositoryManager manager, URI destinationLocation) throws ProvisionException {
		IMetadataRepository repository;
		try {
			String repositoryName = destinationLocation + " - metadata"; //$NON-NLS-1$
			Map properties = null;
			if (compressed) {
				properties = new HashMap(1);
				properties.put(IRepository.PROP_COMPRESSED, String.valueOf(compressed));
			}
			repository = manager.createRepository(destinationLocation, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			manager.removeRepository(destinationLocation);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Metadata repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			return repository;
		} catch (ProvisionException e) {
			//fall through and create repo
		}
		repository = manager.loadRepository(destinationLocation, null);
		if (repository != null)
			manager.removeRepository(destinationLocation);
		if (!repository.isModifiable())
			throw new IllegalArgumentException("Metadata repository not modifiable: " + destinationLocation); //$NON-NLS-1$
		return repository;
	}

	private IArtifactRepository initializeArtifactDestination(ArtifactRepositoryManager repoManager, URI destinationLocation) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		IArtifactRepository repository;
		try {
			String repositoryName = destinationLocation + " - artifacts"; //$NON-NLS-1$
			Map properties = null;
			if (compressed) {
				properties = new HashMap(1);
				properties.put(IRepository.PROP_COMPRESSED, String.valueOf(compressed));
			}
			repository = manager.createRepository(destinationLocation, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);
			if (repository != null)
				manager.removeRepository(destinationLocation);

			if (!repository.isModifiable())
				throw new IllegalArgumentException("Artifact repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			if (overwrite)
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		// 	the given repo location is not an existing repo so we have to create something
		repository = manager.loadRepository(destinationLocation, null);
		manager.removeRepository(destinationLocation);
		return repository;
	}

	public void setVerbose(boolean value) {
		verbose = value;
	}

	public void setReferencedIUs(boolean value) {
		referencedIUs = value;
	}

	public void setRaw(boolean value) {
		raw = value;
	}

	public void setCompressed(boolean value) {
		compressed = value;
	}
}
