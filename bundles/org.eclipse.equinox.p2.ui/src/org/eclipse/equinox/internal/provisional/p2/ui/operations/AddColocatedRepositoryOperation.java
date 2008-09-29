/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.ServiceReference;

/**
 * Operation that adds colocated artifact and metadata repositories
 * given a URL.
 * 
 * @since 3.4
 */
public class AddColocatedRepositoryOperation extends RepositoryOperation {

	boolean added = false;

	public AddColocatedRepositoryOperation(String label, URL url) {
		super(label, new URL[] {url});
	}

	public AddColocatedRepositoryOperation(String label, URL[] urls) {
		super(label, urls);
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// Special case:  if a repo is already known by the manager is disabled,
		// then enable it.  Note that this means the implementation of undo is
		// not correct.  Undoing this operation will remove the repos, but we do not make use
		// of the undo protocol in 3.4.x so we live with this problem.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=236077
		ArrayList disabledMetaRepos = new ArrayList();
		IMetadataRepositoryManager metaMgr = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (metaMgr != null) {
			URL[] disabled = metaMgr.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_DISABLED);
			for (int i = 0; i < disabled.length; i++)
				disabledMetaRepos.add(disabled[i].toExternalForm());
		}
		ArrayList disabledArtifactRepos = new ArrayList();
		IArtifactRepositoryManager artifactMgr = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (artifactMgr != null) {
			URL[] disabled = artifactMgr.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_DISABLED);
			for (int i = 0; i < disabled.length; i++)
				disabledArtifactRepos.add(disabled[i].toExternalForm());
		}
		for (int i = 0; i < urls.length; i++) {
			if (metaMgr != null && disabledMetaRepos.contains(urls[i].toExternalForm())) {
				metaMgr.setEnabled(urls[i], true);
				publishEvent(new RepositoryEvent(urls[i], IRepository.TYPE_METADATA, RepositoryEvent.ADDED, true));
			} else
				ProvisioningUtil.addMetadataRepository(urls[i]);
			if (artifactMgr != null && disabledArtifactRepos.contains(urls[i].toExternalForm())) {
				artifactMgr.setEnabled(urls[i], true);
				publishEvent(new RepositoryEvent(urls[i], IRepository.TYPE_ARTIFACT, RepositoryEvent.ADDED, true));
			} else
				ProvisioningUtil.addArtifactRepository(urls[i]);
		}
		added = true;
		return okStatus();
	}

	private void publishEvent(RepositoryEvent event) {
		ServiceReference busReference = ProvUIActivator.getContext().getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		if (busReference != null)
			((IProvisioningEventBus) ProvUIActivator.getContext().getService(busReference)).publishEvent(event);
	}

	protected IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < urls.length; i++) {
			ProvisioningUtil.removeMetadataRepository(urls[i], monitor);
			ProvisioningUtil.removeArtifactRepository(urls[i], monitor);
		}
		added = false;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return super.canExecute() && !added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return super.canUndo() && added;
	}
}
