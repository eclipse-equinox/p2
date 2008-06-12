/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class Publisher {
	static final public String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$

	private IPublisherInfo info;
	private IPublisherResult results;

	/**
	 * Returns a metadata repository that corresponds to the given settings.  If a repo at the 
	 * given location already exists, it is updated with the settings and returned.  If no repository
	 * is found then a new Simple repository is created, configured and returned
	 * @param location the URL location of the repo
	 * @param name the name of the repo
	 * @param append whether or not the repo should appended or cleared
	 * @param compress whether or not to compress the repository index
	 * @return the discovered or created repository
	 * @throws ProvisionException
	 */
	public static IMetadataRepository createMetadataRepository(String location, String name, boolean append, boolean compress) throws ProvisionException {
		URL url;
		try {
			url = new URL(location);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.exception_metadataRepoLocationURL, location));
		}
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.context, IMetadataRepositoryManager.class.getName());
		try {
			IMetadataRepository result = manager.loadRepository(url, null);
			if (result != null) {
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				if (!result.isModifiable())
					throw new IllegalArgumentException(NLS.bind(Messages.exception_metadataRepoNotWritable, url));
				if (!append)
					result.removeAll();
				return result;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a random repo by default.
		String repositoryName = name == null ? location + " - metadata" : name; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(url, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		manager.addRepository(result.getLocation());
		if (result != null)
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
		return result;
	}

	/**
	 * Returns an artifact repository that corresponds to the given settings.  If a repo at the 
	 * given location already exists, it is updated with the settings and returned.  If no repository
	 * is found then a new Simple repository is created, configured and returned
	 * @param location the URL location of the repo
	 * @param name the name of the repo
	 * @param append whether or not the repo should appended or cleared
	 * @param compress whether or not to compress the repository index
	 * @param reusePackedFiles whether or not to include discovered Pack200 files in the repository
	 * @return the discovered or created repository
	 * @throws ProvisionException
	 */
	public static IArtifactRepository createArtifactRepository(String location, String name, boolean append, boolean compress, boolean reusePackedFiles) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.context, IArtifactRepositoryManager.class.getName());
		URL url;
		try {
			url = new URL(location);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoLocationURL, location));
		}
		try {
			IArtifactRepository result = manager.loadRepository(url, null);
			if (!result.isModifiable())
				throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoNotWritable, url));
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
			if (reusePackedFiles)
				result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
			if (!append)
				result.removeAll();
			return result;
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		// TODO for now create a Simple repo by default.
		String repositoryName = name != null ? name : location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(url, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		manager.addRepository(result.getLocation());
		// TODO there must be something we have to do to set up the mapping rules here...
		//		if (inplace) {
		//		}
		result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
		if (reusePackedFiles)
			result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
		return result;
	}

	public Publisher(IPublisherInfo info) {
		this.info = info;
		results = new PublisherResult();
	}

	public Publisher(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		this.results = results;
	}

	public IStatus publish(IPublishingAction[] actions) {
		// run all the actions
		MultiStatus finalStatus = new MultiStatus("this", 0, "publishing result", null);
		for (int i = 0; i < actions.length; i++) {
			IStatus status = actions[i].perform(info, results);
			finalStatus.merge(status);
		}
		if (!finalStatus.isOK())
			return finalStatus;

		// if there were no errors, publish all the ius.
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection ius = results.getIUs(null, null);
			metadataRepository.addInstallableUnits((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));
		}
		return Status.OK_STATUS;
	}
}
