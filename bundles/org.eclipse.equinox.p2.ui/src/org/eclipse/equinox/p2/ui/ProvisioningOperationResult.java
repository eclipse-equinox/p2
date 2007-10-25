/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;

/**
 * Reports information about a provisioning operation.
 * 
 * @since 3.4
 */
public class ProvisioningOperationResult {
	private ProvisioningOperation op;
	private IStatus status;
	private Job job;

	public ProvisioningOperationResult(ProvisioningOperation op) {
		this.op = op;
	}

	public IStatus getStatus() {
		return status;
	}

	void setStatus(IStatus status) {
		this.status = status;
	}

	public Job getJob() {
		return job;
	}

	void setJob(Job job) {
		this.job = job;
	}

	public ProvisioningOperation getOperation() {
		return op;
	}

}
