/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.*;

public class DownloadManager {
	private ProvisioningContext provContext = null;
	ArrayList requestsToProcess = new ArrayList();

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

	/**
	 * This Comparator sorts the repositories such that ´local´ repositories are first
	 */
	private static final Comparator LOCAL_FIRST_COMPARATOR = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			Assert.isTrue(arg0 instanceof URI);
			Assert.isTrue(arg1 instanceof URI);

			String protocol0 = ((URI) arg0).getScheme();
			String protocol1 = ((URI) arg1).getScheme();

			if (FILE_PROTOCOL.equals(protocol0) && !FILE_PROTOCOL.equals(protocol1))
				return -1;
			if (!FILE_PROTOCOL.equals(protocol0) && FILE_PROTOCOL.equals(protocol1))
				return 1;
			return 0;
		}
	};
	private final IArtifactRepositoryManager repositoryManager;

	public DownloadManager(ProvisioningContext context, IArtifactRepositoryManager repositoryManager) {
		provContext = context;
		this.repositoryManager = repositoryManager;
	}

	/*
	 * Add the given artifact to the download queue. When it
	 * is downloaded, put it in the specified location.
	 */
	public void add(IArtifactRequest toAdd) {
		Assert.isNotNull(toAdd);
		requestsToProcess.add(toAdd);
	}

	public void add(IArtifactRequest[] toAdd) {
		Assert.isNotNull(toAdd);
		for (int i = 0; i < toAdd.length; i++) {
			add(toAdd[i]);
		}
	}

	private void filterUnfetched() {
		for (Iterator iterator = requestsToProcess.iterator(); iterator.hasNext();) {
			IArtifactRequest request = (IArtifactRequest) iterator.next();
			if (request.getResult() != null && request.getResult().isOK()) {
				iterator.remove();
			}
		}
	}

	/*
	 * Start the downloads. Return a status message indicating success or failure of the overall operation
	 */
	public IStatus start(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.download_artifact, requestsToProcess.size());
		try {
			if (requestsToProcess.isEmpty())
				return Status.OK_STATUS;

			URI[] repositories = null;
			if (provContext == null || provContext.getArtifactRepositories() == null)
				repositories = repositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
			else
				repositories = provContext.getArtifactRepositories();
			if (repositories.length == 0)
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.download_no_repository, new Exception());
			Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);
			fetch(repositories, subMonitor);
			return overallStatus(monitor);
		} finally {
			subMonitor.done();
		}
	}

	private void fetch(URI[] repositories, SubMonitor monitor) {
		for (int i = 0; i < repositories.length && !requestsToProcess.isEmpty() && !monitor.isCanceled(); i++) {
			try {
				IArtifactRepository current = repositoryManager.loadRepository(repositories[i], monitor.newChild(0));
				IArtifactRequest[] requests = getRequestsForRepository(current);
				IStatus dlStatus = current.getArtifacts(requests, monitor.newChild(requests.length));
				if (dlStatus.getSeverity() == IStatus.CANCEL)
					return;
				filterUnfetched();
				monitor.setWorkRemaining(requestsToProcess.size());
			} catch (ProvisionException e) {
				//skip unreachable repositories
			}
		}
	}

	private IArtifactRequest[] getRequestsForRepository(IArtifactRepository repository) {
		ArrayList applicable = new ArrayList();
		for (Iterator it = requestsToProcess.iterator(); it.hasNext();) {
			IArtifactRequest request = (IArtifactRequest) it.next();
			if (repository.contains(request.getArtifactKey()))
				applicable.add(request);
		}
		return (IArtifactRequest[]) applicable.toArray(new IArtifactRequest[applicable.size()]);
	}

	//	private void notifyFetched() {
	//		ProvisioningEventBus bus = (ProvisioningEventBus) ServiceHelper.getService(DownloadActivator.context, ProvisioningEventBus.class);
	//		bus.publishEvent();
	//	}

	private IStatus overallStatus(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			return Status.CANCEL_STATUS;

		if (requestsToProcess.size() == 0)
			return Status.OK_STATUS;

		MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Iterator iterator = requestsToProcess.iterator(); iterator.hasNext();) {
			IStatus failed = ((IArtifactRequest) iterator.next()).getResult();
			if (failed != null && !failed.isOK())
				result.add(failed);
		}
		return result;
	}
}
