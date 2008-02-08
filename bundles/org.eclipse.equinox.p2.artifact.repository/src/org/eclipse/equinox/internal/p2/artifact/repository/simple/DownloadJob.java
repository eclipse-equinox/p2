/*******************************************************************************
 * Copyright (c) 2008 Genuitec, LLC and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Genuitec, LLC - initial API and implementation
 * 						IBM Corporation - ongoing maintenance
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.LinkedList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRequest;
import org.eclipse.osgi.util.NLS;

public class DownloadJob extends Job {
	static final Object FAMILY = new Object();

	private LinkedList requestsPending;
	private SimpleArtifactRepository repository;
	private IProgressMonitor masterMonitor;
	private MultiStatus overallStatus;

	DownloadJob(String name) {
		super(name);
		setSystem(true);
	}

	void initialize(SimpleArtifactRepository repository, LinkedList requestsPending, IProgressMonitor masterMonitor, MultiStatus overallStatus) {
		this.repository = repository;
		this.requestsPending = requestsPending;
		this.masterMonitor = masterMonitor;
		this.overallStatus = overallStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	public boolean belongsTo(Object family) {
		return family == FAMILY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor jobMonitor) {
		jobMonitor.beginTask("Downloading artifacts", 1);
		do {
			// get the request we are going to process
			IArtifactRequest request;
			synchronized (requestsPending) {
				if (requestsPending.isEmpty())
					break;
				request = (IArtifactRequest) requestsPending.removeFirst();
			}
			if (masterMonitor.isCanceled())
				return Status.CANCEL_STATUS;

			// prepare a progress monitor that reports to both the master monitor and for the job
			IProgressMonitor monitor = new NullProgressMonitor();
			// progress monitor updating from getArtifact() doesn't seem to be working
			masterMonitor.subTask(NLS.bind("Downloading {0}.", request.getArtifactKey().getId()));

			// process the actual request
			IStatus status = repository.getArtifact((ArtifactRequest) request, monitor);
			if (!status.isOK()) {
				synchronized (overallStatus) {
					overallStatus.add(status);
				}
			}

			// update progress
			masterMonitor.worked(1);
		} while (true);

		jobMonitor.done();
		return Status.OK_STATUS;
	}
}
