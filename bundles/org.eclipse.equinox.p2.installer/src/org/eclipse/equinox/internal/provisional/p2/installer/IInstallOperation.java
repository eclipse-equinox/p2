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
package org.eclipse.equinox.internal.provisional.p2.installer;

import org.eclipse.core.runtime.*;

/**
 * The operation that will perform the install. This interface exists to allow
 * the install advisor to act as the runnable context for the install, handling
 * progress monitoring and cancelation.
 * <p>
 * This interface is not intended to be implemented by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IInstallOperation {
	/**
	 * Performs the install.
	 * <p>
	 * The provided monitor can be used to report progress and respond to 
	 * cancellation.  If the progress monitor has been canceled, the job
	 * should finish its execution at the earliest convenience and return a result
	 * status of severity {@link IStatus#CANCEL}.  The singleton
	 * cancel status {@link Status#CANCEL_STATUS} can be used for
	 * this purpose.  The monitor is only valid for the duration of the invocation
	 * of this method.
	 * 
	 * @param monitor the monitor to be used for reporting progress and
	 * responding to cancelation, or <code>null</code> if progress reporting
	 * and cancelation are not desired.
	 * @return resulting status of the run. The result must not be <code>null</code>
	 */
	public IStatus install(IProgressMonitor monitor);
}
