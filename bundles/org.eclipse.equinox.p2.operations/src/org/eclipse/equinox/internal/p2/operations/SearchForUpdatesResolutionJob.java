/*******************************************************************************
 *  Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.operations;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;

public class SearchForUpdatesResolutionJob extends PlannerResolutionJob {

	IRunnableWithProgress searchForUpdatesRunnable;
	ProfileChangeRequest[] requestHolder;
	UpdateOperation operation;

	public SearchForUpdatesResolutionJob(String label, ProvisioningSession session, String profileId, ProfileChangeRequest request, ProvisioningContext context, IFailedStatusEvaluator evaluator, MultiStatus additionalStatus, IRunnableWithProgress searchForUpdatesRunnable, ProfileChangeRequest[] requestHolder, UpdateOperation operation) {
		super(label, session, profileId, request, context, evaluator, additionalStatus);
		this.searchForUpdatesRunnable = searchForUpdatesRunnable;
		this.requestHolder = requestHolder;
		this.operation = operation;
	}

	@Override
	public IStatus runModal(IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor);
		try {
			searchForUpdatesRunnable.run(sub.newChild(500));
			if (requestHolder.length > 0)
				this.request = requestHolder[0];
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (InvocationTargetException e) {
			// ignore, we don't actually throw this in the supplied runnable
		}
		if (request != null)
			return super.runModal(sub.newChild(500));
		return operation.getResolutionResult();
	}

	// This is made public for the automated tests
	@Override
	public ProfileChangeRequest getProfileChangeRequest() {
		return request;
	}
}
