/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import org.eclipse.core.runtime.*;

/**
 * Abstract class for a repository that can signal the start
 * and completion of the job using events.
 * 
 * @since 2.0
 */
public abstract class SignallingRepositoryJob extends RepositoryJob {

	/**
	 * Create a repository job that can be used to manipulate the specified 
	 * repository locations.
	 * @param name the name of the job
	 * @param session the provisioning session to be used
	 * @param locations the locations of the repositories to be manipulated.
	 */
	protected SignallingRepositoryJob(String label, ProvisioningSession session, URI[] locations) {
		super(label, session, locations);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProvisioningJob#runModal(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runModal(IProgressMonitor monitor) {
		if (locations == null || locations.length == 0)
			return Status.OK_STATUS;

		// We batch all the time as a way of distinguishing client-initiated repository 
		// jobs from low level repository manipulation.
		getSession().signalOperationStart();
		try {
			doSignaledWork(monitor);
		} finally {
			Object item = null;
			if (locations.length == 1)
				item = locations[0];
			getSession().signalOperationComplete(item);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Perform the specific work involved in running this job in
	 * the current thread.  This method is called after signalling the
	 * operation start.  The completion of the operation is signalled after
	 * this method is called.  This method may be called from any thread.
	 * 
	 * @param IProgressMonitor the monitor to use in the course of performing
	 * the operation.
	 * 
	 * @return a status indicating the result of the operation
	 */
	protected abstract IStatus doSignaledWork(IProgressMonitor monitor);
}
