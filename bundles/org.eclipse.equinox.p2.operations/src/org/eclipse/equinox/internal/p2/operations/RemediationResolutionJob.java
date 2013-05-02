/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.equinox.p2.operations.RemediationOperation;
import org.eclipse.equinox.p2.repository.IRunnableWithProgress;

public class RemediationResolutionJob extends PlannerResolutionJob {

	IRunnableWithProgress computeRemediationRunnable;
	RemediationOperation operation;
	ProfileChangeRequest[] requestHolder;

	public RemediationResolutionJob(String label, ProvisioningSession session, String profileId, ProfileChangeRequest request, ProvisioningContext context, IFailedStatusEvaluator evaluator, MultiStatus additionalStatus, IRunnableWithProgress computeRemediationRunnable, ProfileChangeRequest[] requestFromOperation, RemediationOperation operation) {
		super(label, session, profileId, request, context, evaluator, additionalStatus);
		this.computeRemediationRunnable = computeRemediationRunnable;
		this.operation = operation;
		this.requestHolder = requestFromOperation;
	}

	public IStatus runModal(IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, 2);
		try {
			computeRemediationRunnable.run(sub.newChild(1));
			if (requestHolder.length > 0)
				this.request = requestHolder[0];
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} catch (InvocationTargetException e) {
			// ignore, we don't actually throw this in the supplied runnable
		}
		if (request != null)
			return super.runModal(sub.newChild(1));
		return operation.getResolutionResult();
	}
}
