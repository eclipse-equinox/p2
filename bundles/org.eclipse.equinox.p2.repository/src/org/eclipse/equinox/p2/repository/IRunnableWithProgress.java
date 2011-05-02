/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.repository;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * The <code>IRunnableWithProgress</code> interface should be implemented by any
 * class whose instances are intended to be executed as a long-running operation.
 * Long-running operations are typically presented at the UI via a modal dialog
 * showing a progress indicator and a Cancel button.
 * The class must define a <code>run</code> method that takes a progress monitor.
 * 
 * @since 2.0
 */
public interface IRunnableWithProgress {

	/**
	 * Runs this operation.  Progress should be reported to the given progress monitor.
	 * This method is usually invoked by an <code>IRunnableContext</code>'s <code>run</code> method,
	 * which supplies the progress monitor.
	 * A request to cancel the operation should be honored and acknowledged 
	 * by throwing <code>InterruptedException</code>.
	 *
	 * @param monitor the progress monitor to use to display progress and receive
	 *   requests for cancelation
	 * @exception OperationCanceledException if the operation detects a request to cancel, 
	 *  using <code>IProgressMonitor.isCanceled()</code>, it should exit by throwing 
	 *  <code>OperationCanceledException</code>
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, OperationCanceledException;
}
