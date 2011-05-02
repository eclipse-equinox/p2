/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.core.ProvisionException;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.osgi.util.NLS;

/**
 * Abstract class representing provisioning jobs.  Provisioning jobs
 * can be run in the background by scheduling them, or they can
 * be run by a client in a modal context.  An additional progress monitor
 * can be set into the job for progress reporting.
 * 
 * @since 2.0
 */
public abstract class ProvisioningJob extends Job {

	/**
	 * Class for multiplexing progress across multiple progress monitors.
	 */
	private static class DoubleProgressMonitor extends ProgressMonitorWrapper {

		IProgressMonitor additionalMonitor;

		protected DoubleProgressMonitor(IProgressMonitor monitor1, IProgressMonitor monitor2) {
			super(monitor1);
			additionalMonitor = monitor2;
		}

		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			additionalMonitor.beginTask(name, totalWork);
		}

		public void clearBlocked() {
			super.clearBlocked();
			if (additionalMonitor instanceof IProgressMonitorWithBlocking)
				((IProgressMonitorWithBlocking) additionalMonitor).clearBlocked();
		}

		public void done() {
			super.done();
			additionalMonitor.done();
		}

		public void internalWorked(double work) {
			super.internalWorked(work);
			additionalMonitor.internalWorked(work);
		}

		public boolean isCanceled() {
			if (super.isCanceled())
				return true;
			return additionalMonitor.isCanceled();
		}

		public void setBlocked(IStatus reason) {
			super.setBlocked(reason);
			if (additionalMonitor instanceof IProgressMonitorWithBlocking)
				((IProgressMonitorWithBlocking) additionalMonitor).setBlocked(reason);
		}

		public void setCanceled(boolean b) {
			super.setCanceled(b);
			additionalMonitor.setCanceled(b);
		}

		public void setTaskName(String name) {
			super.setTaskName(name);
			additionalMonitor.setTaskName(name);
		}

		public void subTask(String name) {
			super.subTask(name);
			additionalMonitor.subTask(name);
		}

		public void worked(int work) {
			super.worked(work);
			additionalMonitor.worked(work);
		}
	}

	/**
	 * Constant which indicates that the job does not require a restart
	 * upon completion.  This constant is typically used for operations that 
	 * do not modify the running profile.
	 * 
	 * @since 2.0
	 */
	public static final int RESTART_NONE = 1;

	/**
	 * Constant which indicates that the job requires the user to either
	 * restart or apply the configuration changes in order to pick up the
	 * changes performed by the job.  This constant is typically used for
	 * operations that modify the running profile.
	 * 
	 * @since 2.0
	 */
	public static final int RESTART_OR_APPLY = 2;
	/**
	 * Constant which indicates that the job requires the user to restart
	 * in order to pick up the changes performed by the job.  This constant
	 * is typically used for operations that modify the running profile but don't 
	 * handle dynamic changes without restarting the workbench.
	 * 
	 * @since 2.0
	 */
	public static final int RESTART_ONLY = 3;

	private ProvisioningSession session;
	private IProgressMonitor additionalMonitor;

	/**
	 * Create a provisioning job with the given name that uses the
	 * provided provisioning session for retrieving any services
	 * needed.
	 * 
	 * @param name the name of the job
	 * @param session the session providing the services
	 */
	public ProvisioningJob(String name, ProvisioningSession session) {
		super(name);
		this.session = session;
	}

	/**
	 * Return the provisioning session that is used by the receiver
	 * when retrieving necessary provisioning services.
	 * 
	 * @return the session
	 * @see ProvisioningSession
	 */
	protected ProvisioningSession getSession() {
		return session;
	}

	private IProgressMonitor getCombinedProgressMonitor(IProgressMonitor mon1, IProgressMonitor mon2) {
		if (mon1 == null)
			return mon2;
		if (mon2 == null)
			return mon1;
		return new DoubleProgressMonitor(mon1, mon2);
	}

	public void setAdditionalProgressMonitor(IProgressMonitor monitor) {
		additionalMonitor = monitor;
	}

	/**
	 * Executes this job.  Returns the result of the execution.
	 * This method is overridden to look for a wrapped progress monitor for
	 * reporting progress.
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 * 
	 */
	public final IStatus run(IProgressMonitor monitor) {
		IProgressMonitor wrappedMonitor = getCombinedProgressMonitor(monitor, additionalMonitor);
		IStatus status = Status.OK_STATUS;
		try {
			status = runModal(wrappedMonitor);
		} catch (OperationCanceledException e) {
			status = Status.CANCEL_STATUS;
		}
		return status;
	}

	/**
	 * Perform the specific work involved in running this job in
	 * the current thread.  This method can be called directly by
	 * clients, or in the course of running the job in the
	 * background.
	 * 
	 * @param monitor
	 *            the progress monitor to use for the operation
	 *            
	 * @return a status indicating the result of the operation.
	 * 
	 */
	public abstract IStatus runModal(IProgressMonitor monitor);

	/**
	 * Return the restart policy that is appropriate for this job.
	 * 
	 * @return a constant indicating the restart policy
	 * 
	 * @see #RESTART_NONE
	 * @see #RESTART_ONLY
	 * @see #RESTART_OR_APPLY
	 */
	public int getRestartPolicy() {
		return RESTART_NONE;
	}

	/**
	 * Return an error status that can be used to report the specified exception.
	 * 
	 * @param message the message that should be used in the status
	 * @param e the exception to be reported
	 * @return a status that can be used to describe the exception
	 */
	protected IStatus getErrorStatus(String message, ProvisionException e) {
		if (message == null)
			if (e == null)
				message = NLS.bind(Messages.ProvisioningJob_GenericErrorStatusMessage, getName());
			else
				message = e.getLocalizedMessage();
		return new Status(IStatus.ERROR, Activator.ID, message, e);
	}

}
