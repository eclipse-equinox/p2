/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * @since 2.0
 *
 */
public class PreloadMetadataRepositoryJob extends RepositoryJob {

	public static final Object LOAD_FAMILY = new Object();
	public static final QualifiedName SUPPRESS_AUTHENTICATION_JOB_MARKER = new QualifiedName(Activator.ID, "SUPPRESS_AUTHENTICATION_REQUESTS"); //$NON-NLS-1$
	public static final QualifiedName ACCUMULATE_LOAD_ERRORS = new QualifiedName(Activator.ID, "ACCUMULATE_LOAD_ERRORS"); //$NON-NLS-1$

	private List repoCache = new ArrayList();
	private RepositoryTracker tracker;
	private MultiStatus accumulatedStatus;

	public PreloadMetadataRepositoryJob(RepositoryTracker tracker) {
		super(Messages.PreloadRepositoryJob_LoadJobName, tracker.getSession(), tracker.getKnownRepositories());
		this.tracker = tracker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProvisioningJob#runModal(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void runModal(IProgressMonitor monitor) throws ProvisionException {
		SubMonitor sub = SubMonitor.convert(monitor, locations.length * 100);
		if (sub.isCanceled())
			return;
		for (int i = 0; i < locations.length; i++) {
			if (sub.isCanceled())
				return;
			try {
				repoCache.add(getSession().loadMetadataRepository(locations[i], sub.newChild(100)));
			} catch (ProvisionException e) {
				handleLoadFailure(e, locations[i]);
			}
		}
	}

	protected void handleLoadFailure(ProvisionException e, URI location) {
		int code = e.getStatus().getCode();
		// special handling when the repo is bad.  We don't want to continually report it
		if (code == ProvisionException.REPOSITORY_NOT_FOUND || code == ProvisionException.REPOSITORY_INVALID_LOCATION) {
			if (tracker.hasNotFoundStatusBeenReported(location))
				return;
			tracker.addNotFound(location);
		}

		// Some ProvisionExceptions include an empty multi status with a message.  
		// Since empty multi statuses have a severity OK, The platform status handler doesn't handle
		// this well.  We correct this by recreating a status with error severity
		// so that the platform status handler does the right thing.
		IStatus status = e.getStatus();
		if (status instanceof MultiStatus && ((MultiStatus) status).getChildren().length == 0)
			status = new Status(IStatus.ERROR, status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
		if (accumulatedStatus == null) {
			accumulatedStatus = new MultiStatus(Activator.ID, ProvisionException.REPOSITORY_NOT_FOUND, new IStatus[] {status}, Messages.PreloadMetadataRepositoryJob_SomeSitesNotFound, null);
		} else {
			accumulatedStatus.add(status);
		}
		// Always log the complete exception so the detailed stack trace is in the log.  
		LogHelper.log(e);
	}

	public void reportAccumulatedStatus() {
		// If we've discovered not found repos we didn't know about, report them
		if (accumulatedStatus != null) {
			// If there is only missing repo to report, use the specific message rather than the generic.
			if (accumulatedStatus.getChildren().length == 1) {
				tracker.reportLoadFailure(null, accumulatedStatus.getChildren()[0]);
			} else {
				tracker.reportLoadFailure(null, accumulatedStatus);
			}
		}
		// Reset the accumulated status so that next time we only report the newly not found repos.
		accumulatedStatus = null;
	}

	public boolean belongsTo(Object family) {
		return family == LOAD_FAMILY;
	}

	public Object getRepositoryReferenceCache() {
		return repoCache;
	}

}
