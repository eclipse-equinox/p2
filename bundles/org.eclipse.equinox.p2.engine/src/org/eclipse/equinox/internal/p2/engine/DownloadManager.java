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
package org.eclipse.equinox.internal.p2.engine;

import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

public class DownloadManager {

	ArrayList requestsToProcess = new ArrayList();

	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$

	/**
	 * This Comparator sorts the repositories such that ´local´ repositories are first 
	 */
	private static final Comparator LOCAL_FIRST_COMPARATOR = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			Assert.isTrue(arg0 instanceof URL);
			Assert.isTrue(arg1 instanceof URL);

			String protocol0 = ((URL) arg0).getProtocol();
			String protocol1 = ((URL) arg1).getProtocol();

			if (protocol0.equals(FILE_PROTOCOL) && !protocol1.equals(FILE_PROTOCOL))
				return -1;
			if (!protocol0.equals(FILE_PROTOCOL) && protocol1.equals(FILE_PROTOCOL))
				return 1;
			return 0;
		}
	};

	/*
	 * Add the given artifact to the download queue. When it 
	 * is downloaded, put it in the specified location.
	 */
	public void add(IArtifactRequest toAdd) {
		requestsToProcess.add(toAdd);
	}

	public void add(IArtifactRequest[] toAdd) {
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
			IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(EngineActivator.getContext(), IArtifactRepositoryManager.class.getName());
			URL[] repositories = repoMgr.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);
			if (repositories.length == 0)
				return new Status(IStatus.ERROR, EngineActivator.ID, Messages.download_no_repository);
			Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);
			fetch(repoMgr, repositories, subMonitor);
			return overallStatus(monitor);
		} finally {
			subMonitor.done();
		}
	}

	private void fetch(IArtifactRepositoryManager repoMgr, URL[] repositories, SubMonitor monitor) {
		for (int i = 0; i < repositories.length && !requestsToProcess.isEmpty() && !monitor.isCanceled(); i++) {
			try {
				IArtifactRepository current = repoMgr.loadRepository(repositories[i], monitor.newChild(0));
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
		if (monitor.isCanceled())
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
