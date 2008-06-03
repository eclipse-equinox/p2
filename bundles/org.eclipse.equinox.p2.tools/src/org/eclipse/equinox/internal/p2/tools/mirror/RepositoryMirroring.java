/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.tools.mirror;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
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
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * A utility class that performs mirroring of metadata and artifacts between repositories.
 */
public class RepositoryMirroring {

	private IMetadataRepository metadataSourceRepository;
	private IMetadataRepository metadataDestinationRepository;
	private IArtifactRepository artifactSourceRepository;
	private IArtifactRepository artifactDestinationRepository;
	private boolean transitive = false;
	private boolean validArtifactRepos = false;
	private boolean validMetadataRepos = false;
	private boolean mirrorArtifactsWithMetadata = false;
	private boolean raw = false;
	private boolean overwrite = false;
	private boolean verbose = false;

	public RepositoryMirroring(URL metadataSourceLocation, URL metadataDestinationLocation, URL artifactSourceLocation, URL artifactDestinationLocation) throws ProvisionException {
		if (metadataSourceLocation != null && metadataDestinationLocation != null) {
			MetadataRepositoryManager metadataRepoManager = new MetadataRepositoryManager();
			metadataSourceRepository = metadataRepoManager.loadRepository(metadataSourceLocation, null);
			metadataDestinationRepository = initializeMetadataDestination(metadataRepoManager, metadataDestinationLocation);
			validMetadataRepos = validateMetadataRepositories();
		}
		if (artifactSourceLocation != null && artifactDestinationLocation != null) {
			ArtifactRepositoryManager artifactRepoManager = new ArtifactRepositoryManager();
			artifactSourceRepository = artifactRepoManager.loadRepository(artifactSourceLocation, null);
			artifactDestinationRepository = initializeArtifactDestination(artifactRepoManager, artifactDestinationLocation);
			validArtifactRepos = validateArtifactRepositories();
		}
	}

	public void mirror(String[] iuSpecs, String[] artifactSpecs) throws ProvisionException {
		mirrorArtifactsWithMetadata = validArtifactRepos && artifactSpecs != null && artifactSpecs.length == 0 && iuSpecs != null && iuSpecs.length == 0;
		if (validMetadataRepos)
			mirrorMetadata(iuSpecs);
		if (validArtifactRepos && !mirrorArtifactsWithMetadata)
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

	public void mirrorMetadata(Query query) throws ProvisionException {
		Collector result = metadataSourceRepository.query(query, new Collector(), null);
		mirrorMetadata((IInstallableUnit[]) result.toArray(IInstallableUnit.class));
	}

	private void mirrorMetadata(IInstallableUnit[] ius) throws ProvisionException {
		if (transitive)
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
		OutputStream repositoryStream = null;
		try {
			repositoryStream = artifactDestinationRepository.getOutputStream(newDescriptor);
			if (repositoryStream == null)
				return;
			if (verbose)
				System.out.println("Mirroring artifact: " + descriptor);
			// TODO Is that ok to ignore the result?
			artifactSourceRepository.getArtifact(descriptor, repositoryStream, new NullProgressMonitor());
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

	private IMetadataRepository initializeMetadataDestination(MetadataRepositoryManager repoManager, URL destinationLocation) throws ProvisionException {
		try {
			IMetadataRepository repository = repoManager.loadRepository(destinationLocation, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Metadata repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			return repository;
		} catch (ProvisionException e) {
			//fall through and create repo
		}
		String repositoryName = destinationLocation + " - metadata"; //$NON-NLS-1$
		return repoManager.createRepository(destinationLocation, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	private IArtifactRepository initializeArtifactDestination(ArtifactRepositoryManager repoManager, URL destinationLocation) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		try {
			IArtifactRepository repository = manager.loadRepository(destinationLocation, null);
			if (!repository.isModifiable())
				throw new IllegalArgumentException("Artifact repository not modifiable: " + destinationLocation); //$NON-NLS-1$
			if (overwrite)
				repository.removeAll();
			return repository;
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = destinationLocation + " - artifacts"; //$NON-NLS-1$
		return manager.createRepository(destinationLocation, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
	}

	public void setVerbose(boolean value) {
		verbose = value;
	}

	public void setTransitive(boolean value) {
		transitive = value;
	}

	public void setOverwrite(boolean value) {
		overwrite = value;
	}

	public void setRaw(boolean value) {
		raw = value;
	}
}
