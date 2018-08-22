/*******************************************************************************
 * Copyright (c) 2008, 2017 Genuitec, LLC and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: Genuitec, LLC - initial API and implementation
 * 						IBM Corporation - ongoing maintenance
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.simple;

import java.util.LinkedList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

public class DownloadJob extends Job {
	static final Object FAMILY = new Object();

	private LinkedList<IArtifactRequest> requestsPending;
	private SimpleArtifactRepository repository;
	private IProgressMonitor masterMonitor;
	private MultiStatus overallStatus;

	DownloadJob(String name) {
		super(name);
		setSystem(true);
	}

	void initialize(SimpleArtifactRepository repository, LinkedList<IArtifactRequest> requestsPending, IProgressMonitor masterMonitor, MultiStatus overallStatus) {
		this.repository = repository;
		this.requestsPending = requestsPending;
		this.masterMonitor = masterMonitor;
		this.overallStatus = overallStatus;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == FAMILY;
	}

	@Override
	protected IStatus run(IProgressMonitor jobMonitor) {
		jobMonitor.beginTask("Downloading software", IProgressMonitor.UNKNOWN);
		do {
			// get the request we are going to process
			IArtifactRequest request;
			synchronized (requestsPending) {
				if (requestsPending.isEmpty())
					break;
				request = requestsPending.removeFirst();
			}
			if (masterMonitor.isCanceled())
				return Status.CANCEL_STATUS;
			// process the actual request
			SubMonitor subMonitor = SubMonitor.convert(masterMonitor, 1);
			subMonitor.beginTask("", 1); //$NON-NLS-1$
			try {
				IStatus status = repository.getArtifact(request, subMonitor);
				if (!status.isOK()) {
					synchronized (overallStatus) {
						overallStatus.add(status);
					}
				}
			} finally {
				subMonitor.done();
			}
		} while (true);

		jobMonitor.done();
		return Status.OK_STATUS;
	}
}
