/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   IBM - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.p2.publisher.Messages;
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
	public static IMetadataRepository createMetadataRepository(URI location, String name, boolean append, boolean compress) throws ProvisionException {
		String repositoryName = name == null ? location + " - metadata" : name; //$NON-NLS-1$
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.context, IMetadataRepositoryManager.class.getName());
		IMetadataRepository result = null;

		try {
			result = manager.createRepository(location, repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			if (result != null) {
				manager.addRepository(result.getLocation());
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
				return result;
			}
		} catch (ProvisionException e) {
			//repo exists, fall through to load
		}

		result = manager.loadRepository(location, null);
		if (result != null) {
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
			if (!result.isModifiable())
				throw new IllegalArgumentException(NLS.bind(Messages.exception_metadataRepoNotWritable, location));
			if (!append)
				result.removeAll();
			return result;
		}
		return null;
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
	public static IArtifactRepository createArtifactRepository(URI location, String name, boolean append, boolean compress, boolean reusePackedFiles) throws ProvisionException {
		String repositoryName = name != null ? name : location + " - artifacts"; //$NON-NLS-1$
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.context, IArtifactRepositoryManager.class.getName());
		IArtifactRepository result = null;

		try {
			result = manager.createRepository(location, repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			if (result != null) {
				manager.addRepository(result.getLocation());
				if (reusePackedFiles)
					result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
				result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
				return result;
			}
		} catch (ProvisionException e) {
			//repo exists, fall through to load
		}

		result = manager.loadRepository(location, null);
		if (result != null) {
			if (!result.isModifiable())
				throw new IllegalArgumentException(NLS.bind(Messages.exception_artifactRepoNotWritable, location));
			result.setProperty(IRepository.PROP_COMPRESSED, compress ? "true" : "false"); //$NON-NLS-1$//$NON-NLS-2$
			if (reusePackedFiles)
				result.setProperty(PUBLISH_PACK_FILES_AS_SIBLINGS, "true"); //$NON-NLS-1$
			if (!append)
				result.removeAll();
			return result;
		}

		return null;
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
			if (!finalStatus.isOK())
				return finalStatus;
		} finally {
			sub.done();
		}
		// if there were no errors, publish all the ius.
		IMetadataRepository metadataRepository = info.getMetadataRepository();
		if (metadataRepository != null) {
			Collection ius = results.getIUs(null, null);
			metadataRepository.addInstallableUnits((IInstallableUnit[]) ius.toArray(new IInstallableUnit[ius.size()]));
		}
		return Status.OK_STATUS;
	}
}
