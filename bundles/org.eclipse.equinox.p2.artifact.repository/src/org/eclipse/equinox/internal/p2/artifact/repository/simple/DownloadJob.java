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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

public class DownloadJob extends Job {
	static final Object FAMILY = new Object();

	private final LinkedList<IArtifactRequest> requestsPending;
	private final SimpleArtifactRepository repository;
	private final SubMonitor masterMonitor;
	private final MultiStatus overallStatus;

	DownloadJob(String name, SimpleArtifactRepository repository, LinkedList<IArtifactRequest> requestsPending,
			SubMonitor masterMonitor, MultiStatus overallStatus) {
		super(name);
		setSystem(true);
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
		jobMonitor.beginTask("Downloading software", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
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
			IStatus status = repository.getArtifact(request, new ThreadSafeProgressMonitor(1));
			if (!status.isOK()) {
				synchronized (overallStatus) {
					overallStatus.add(status);
				}
			}
		} while (true);

		jobMonitor.done();
		return Status.OK_STATUS;
	}

	/**
	 * Wrapper around the general {@link IProgressMonitor} to make it thread safe.
	 * All methods are wrapped within a {@link ReentrantLock} to ensure that only
	 * one {@link Thread} can notify the {@link #masterMonitor}.
	 */
	private class ThreadSafeProgressMonitor implements IProgressMonitor {
		private static final ReentrantLock LOCK = new ReentrantLock();
		private final IProgressMonitor monitor;

		private ThreadSafeProgressMonitor(int ticks) {
			this.monitor = masterMonitor.newChild(1);
		}

		@Override
		public void worked(int ticks) {
			threadSafe(() -> monitor.worked(ticks));
		}

		@Override
		public void internalWorked(double ticks) {
			threadSafe(() -> monitor.internalWorked(ticks));
		}

		@Override
		public void beginTask(String name, int totalWork) {
			threadSafe(() -> monitor.beginTask(name, totalWork));
		}

		@Override
		public void done() {
			threadSafe(() -> monitor.done());
		}

		@Override
		public void setCanceled(boolean value) {
			threadSafe(() -> monitor.setCanceled(value));
		}

		@Override
		public void setTaskName(String name) {
			threadSafe(() -> monitor.setTaskName(name));
		}

		@Override
		public void subTask(String name) {
			threadSafe(() -> monitor.subTask(name));

		}

		@Override
		public boolean isCanceled() {
			return threadSafe(() -> monitor.isCanceled());
		}

		private void threadSafe(Runnable runnable) {
			threadSafe(() -> {
				runnable.run();
				return null;
			});
		}

		private <T> T threadSafe(Supplier<T> supplier) {
			LOCK.lock();
			try {
				return supplier.get();
			} finally {
				LOCK.unlock();
			}
		}
	}
}
