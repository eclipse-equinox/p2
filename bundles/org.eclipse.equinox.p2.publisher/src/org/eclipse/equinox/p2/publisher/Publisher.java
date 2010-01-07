/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import org.eclipse.equinox.p2.core.ProvisionException;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

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
	public static IMetadataRepository createMetadataRepository(IProvisioningAgent agent, URI location, String name, boolean append, boolean compress) throws ProvisionException {
		try {
			IMetadataRepository result = loadMetadataRepository(agent, location, true, true);
			if (result != null && result.isModifiable()) {
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				if (!append)
					result.removeAll();
				return result;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		// 	the given repo location is not an existing repo so we have to create something
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		String repositoryName = name == null ? location + " - metadata" : name; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		if (result != null) {
			manager.removeRepository(result.getLocation());
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			return result;
		}
		// I don't think we can really get here, but just in case, we better throw a provisioning exception
		String msg = org.eclipse.equinox.internal.p2.metadata.repository.Messages.repoMan_internalError;
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
	}

	/**
	 * Load a metadata repository from the given location.
	 * @param location the URI location of the repo
	 * @param modifiable whether to ask the manager for a modifiable repository
	 * @param removeFromManager remove the loaded repository from the manager if it wasn't already loaded
	 * @return the loaded repository
	 * @throws ProvisionException
	 */
	public static IMetadataRepository loadMetadataRepository(IProvisioningAgent agent, URI location, boolean modifiable, boolean removeFromManager) throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		boolean existing = manager.contains(location);

		IMetadataRepository result = manager.loadRepository(location, modifiable ? IRepositoryManager.REPOSITORY_HINT_MODIFIABLE : 0, null);
		if (!existing && removeFromManager)
			manager.removeRepository(location);
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
	public static IArtifactRepository createArtifactRepository(IProvisioningAgent agent, URI location, String name, boolean append, boolean compress, boolean reusePackedFiles) throws ProvisionException {
		try {
			IArtifactRepository result = loadArtifactRepository(agent, location, true, true);
			if (result != null && result.isModifiable()) {
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				if (reusePackedFiles)
					result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
				if (!append)
					result.removeAll();
				return result;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository
		}

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		String repositoryName = name != null ? name : location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		if (result != null) {
			manager.removeRepository(result.getLocation());
			if (reusePackedFiles)
				result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
			return result;
		}
		// I don't think we can really get here, but just in case, we better throw a provisioning exception
		String msg = org.eclipse.equinox.internal.p2.artifact.repository.Messages.repoMan_internalError;
		throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
	}

	/**
	 * Load an artifact repository from the given location.
	 * @param location the URI location of the repo
	 * @param modifiable whether to ask the manager for a modifiable repository
	 * @param removeFromManager remove the loaded repository from the manager if it wasn't already loaded
	 * @return the loaded repository
	 * @throws ProvisionException
	 */
	public static IArtifactRepository loadArtifactRepository(IProvisioningAgent agent, URI location, boolean modifiable, boolean removeFromManager) throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		boolean existing = manager.contains(location);

		IArtifactRepository result = manager.loadRepository(location, modifiable ? IRepositoryManager.REPOSITORY_HINT_MODIFIABLE : 0, null);
		if (!existing && removeFromManager)
			manager.removeRepository(location);
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

	public IStatus publish(IPublisherAction[] actions, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor sub = SubMonitor.convert(monitor, actions.length);
		if (Tracing.DEBUG_PUBLISHING)
			Tracing.debug("Invoking publisher"); //$NON-NLS-1$
		try {
			// run all the actions
			MultiStatus finalStatus = new MultiStatus("this", 0, "publishing result", null); //$NON-NLS-1$//$NON-NLS-2$
			for (int i = 0; i < actions.length; i++) {
				if (sub.isCanceled())
					return Status.CANCEL_STATUS;
				IStatus status = actions[i].perform(info, results, monitor);
				finalStatus.merge(status);
				sub.worked(1);
			}
			if (Tracing.DEBUG_PUBLISHING)
				Tracing.debug("Publishing complete. Result=" + finalStatus); //$NON-NLS-1$
			if (!finalStatus.isOK())
				return finalStatus;
		} finally {
			sub.done();
		}
		// if there were no errors, publish all the ius.
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection<IInstallableUnit> ius = results.getIUs(null, null);
			metadataRepository.addInstallableUnits(ius.toArray(new IInstallableUnit[ius.size()]));
		}
		return Status.OK_STATUS;
	}
}
