/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

public class Publisher {
	static final public String PUBLISH_PACK_FILES_AS_SIBLINGS = "publishPackFilesAsSiblings"; //$NON-NLS-1$
	private static final long SERVICE_TIMEOUT = 5000;

	private IPublisherInfo info;
	private IPublisherResult results;

	/**
	 * Returns a metadata repository that corresponds to the given settings. If a
	 * repository at the given location already exists, it is updated with the
	 * settings and returned. If no repository is found then a new Simple repository
	 * is created, configured and returned
	 *
	 * @param agent    the provisioning agent to use when creating the repository
	 * @param location the URL location of the repository
	 * @param name     the name of the repository
	 * @param append   whether or not the repository should appended or cleared
	 * @param compress whether or not to compress the repository index
	 * @return the discovered or created repository
	 * @throws ProvisionException
	 */
	public static IMetadataRepository createMetadataRepository(IProvisioningAgent agent, URI location, String name,
			boolean append, boolean compress) throws ProvisionException {
		try {
			IMetadataRepository result = loadMetadataRepository(agent, location, true, true);
			if (result != null && result.isModifiable()) {
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				if (!append)
					result.removeAll();
				return result;
			}
		} catch (ProvisionException e) {
			// fall through and create a new repository
		}

		// the given repo location is not an existing repo so we have to create
		// something
		IMetadataRepositoryManager manager = getService(agent, IMetadataRepositoryManager.SERVICE_NAME);
		String repositoryName = name == null ? location + " - metadata" : name; //$NON-NLS-1$
		IMetadataRepository result = manager.createRepository(location, repositoryName,
				IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		if (result != null) {
			manager.removeRepository(result.getLocation());
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
			return result;
		}
		// I don't think we can really get here, but just in case, we better throw a
		// provisioning exception
		String msg = org.eclipse.equinox.internal.p2.metadata.repository.Messages.repoMan_internalError;
		throw new ProvisionException(
				new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
	}

	/**
	 * Load a metadata repository from the given location.
	 *
	 * @param location          the URI location of the repository
	 * @param modifiable        whether to ask the manager for a modifiable
	 *                          repository
	 * @param removeFromManager remove the loaded repository from the manager if it
	 *                          wasn't already loaded
	 * @return the loaded repository
	 * @throws ProvisionException
	 */
	public static IMetadataRepository loadMetadataRepository(IProvisioningAgent agent, URI location, boolean modifiable,
			boolean removeFromManager) throws ProvisionException {
		return loadRepository(agent, IMetadataRepositoryManager.SERVICE_NAME, location, modifiable, removeFromManager);
	}

	/**
	 * Returns an artifact repository that corresponds to the given settings. If a
	 * repository at the given location already exists, it is updated with the
	 * settings and returned. If no repository is found then a new Simple repository
	 * is created, configured and returned
	 *
	 * @param agent    the provisioning agent to use when creating the repository
	 * @param location the URL location of the repository
	 * @param name     the name of the repository
	 * @param compress whether or not to compress the repository index
	 * @return the discovered or created repository
	 * @throws ProvisionException
	 */
	@Deprecated(forRemoval = true, since = "2.3.0")
	public static IArtifactRepository createArtifactRepository(IProvisioningAgent agent, URI location, String name,
			boolean compress) throws ProvisionException {
		try {
			IArtifactRepository result = loadArtifactRepository(agent, location, true, true);
			if (result != null && result.isModifiable()) {
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				return result;
			}
		} catch (ProvisionException e) {
			// fall through and create a new repository
		}

		IArtifactRepositoryManager manager = getService(agent, IArtifactRepositoryManager.SERVICE_NAME);
		String repositoryName = name != null ? name : location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepository result = manager.createRepository(location, repositoryName,
				IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		if (result != null) {
			manager.removeRepository(result.getLocation());
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
			return result;
		}
		// I don't think we can really get here, but just in case, we better throw a
		// provisioning exception
		String msg = org.eclipse.equinox.internal.p2.artifact.repository.Messages.repoMan_internalError;
		throw new ProvisionException(
				new Status(IStatus.ERROR, Activator.ID, ProvisionException.INTERNAL_ERROR, msg, null));
	}

	/**
	 * Load an artifact repository from the given location.
	 *
	 * @param location          the URI location of the repository
	 * @param modifiable        whether to ask the manager for a modifiable
	 *                          repository
	 * @param removeFromManager remove the loaded repository from the manager if it
	 *                          wasn't already loaded
	 * @return the loaded repository
	 * @throws ProvisionException
	 */
	public static IArtifactRepository loadArtifactRepository(IProvisioningAgent agent, URI location, boolean modifiable,
			boolean removeFromManager) throws ProvisionException {
		return loadRepository(agent, IArtifactRepositoryManager.SERVICE_NAME, location, modifiable, removeFromManager);
	}

	private static <T, R extends IRepository<T>> R loadRepository(IProvisioningAgent agent, String serviceName,
			URI location, boolean modifiable, boolean removeFromManager) throws ProvisionException {
		IRepositoryManager<T> manager = getService(agent, serviceName);
		boolean existing = manager.contains(location);
		int flags = modifiable ? IRepositoryManager.REPOSITORY_HINT_MODIFIABLE : 0;
		@SuppressWarnings("unchecked")
		R result = (R) manager.loadRepository(location, flags, null);
		if (!existing && removeFromManager) {
			manager.removeRepository(location);
		}
		return result;
	}

	public Publisher(IPublisherInfo info) {
		this.info = info;
		results = new PublisherResult();
	}

	/**
	 * Obtains a service from the agent, waiting for a reasonable timeout period if
	 * the service is not yet available. This method never returns
	 * <code>null</code>; an exception is thrown if the service could not be
	 * obtained.
	 *
	 * @param <T>         The type of the service to return
	 * @param agent       The agent to obtain the service from
	 * @param serviceName The name of the service to obtain
	 * @return The service instance
	 */
	@SuppressWarnings("unchecked")
	protected static <T> T getService(IProvisioningAgent agent, String serviceName) {
		T service = (T) agent.getService(serviceName);
		if (service != null)
			return service;
		long start = System.currentTimeMillis();
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore and keep waiting
			}
			service = (T) agent.getService(serviceName);
			if (service != null)
				return service;
		} while ((System.currentTimeMillis() - start) < SERVICE_TIMEOUT);
		// could not obtain the service
		throw new IllegalStateException("Unable to obtain required service: " + serviceName); //$NON-NLS-1$
	}

	public Publisher(IPublisherInfo info, IPublisherResult results) {
		this.info = info;
		this.results = results;
	}

	class ArtifactProcess implements IRunnableWithProgress {

		private final IPublisherAction[] actions;
		private final IPublisherInfo publisherInfo;
		private IStatus result = null;

		public ArtifactProcess(IPublisherAction[] actions, IPublisherInfo info) {
			this.publisherInfo = info;
			this.actions = actions;
		}

		@Override
		public void run(IProgressMonitor monitor) {
			MultiStatus finalStatus = new MultiStatus("this", 0, "publishing result", null); //$NON-NLS-1$//$NON-NLS-2$
			for (IPublisherAction action : actions) {
				if (monitor.isCanceled()) {
					result = Status.CANCEL_STATUS;
					return;
				}
				IStatus status = action.perform(publisherInfo, results, monitor);
				finalStatus.merge(status);
				monitor.worked(1);
			}
			result = finalStatus;
		}

		public IStatus getStatus() {
			return result;
		}

	}

	public IStatus publish(IPublisherAction[] actions, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor sub = SubMonitor.convert(monitor, actions.length);
		if (Tracing.DEBUG_PUBLISHING)
			Tracing.debug("Invoking publisher"); //$NON-NLS-1$
		try {
			ArtifactProcess artifactProcess = new ArtifactProcess(actions, info);

			IStatus finalStatus = null;
			if (info.getArtifactRepository() != null) {
				finalStatus = info.getArtifactRepository().executeBatch(artifactProcess, sub);
				if (!finalStatus.matches(IStatus.ERROR | IStatus.CANCEL))
					// If the batch process didn't report any errors, then
					// Use the status from our actions
					finalStatus = artifactProcess.getStatus();
			} else {
				artifactProcess.run(sub);
				finalStatus = artifactProcess.getStatus();
			}

			if (Tracing.DEBUG_PUBLISHING)
				Tracing.debug("Publishing complete. Result=" + finalStatus); //$NON-NLS-1$

			savePublishedIUs();

			if (!finalStatus.isOK())
				return finalStatus;
			return Status.OK_STATUS;
		} finally {
			sub.done();
		}
	}

	protected void savePublishedIUs() {
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection<IInstallableUnit> ius = results.getIUs(null, null);
			metadataRepository.addInstallableUnits(ius);
		}
	}
}
