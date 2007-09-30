/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * The provisioning action interface represents a unit of work
 * that can be performed during a provisioning activity.
 * The granularity of the action can vary from a simple,
 * indivisible unit of work to a complex, organized collection
 * of steps.
 * <p>
 * The results of an provisioning activity must be revert-able
 * an error or if a cancellation occurs. A provisioning action
 * that will return an error or cancel status may choose to revert
 * any work performed before returning or may indicate that
 * the caller must explicitly revert.
 */
public interface IProvisioningAction {

	public IStatus perform(AbstractProvisioningTransaction transaction, IProgressMonitor monitor);

	public IStatus revert(AbstractProvisioningTransaction transaction, IProgressMonitor monitor);

	public boolean shouldRevertOnError();

}
