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
import java.util.function.Consumer;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;

public class DownloadJob extends Job {
	private static final int SUB_TICKS = 1000;

	static final Object FAMILY = new Object();

	private final LinkedList<IArtifactRequest> requestsPending;
	private final SimpleArtifactRepository repository;

	private final Consumer<IStatus> resultConsumer;

	private final Consumer<String> messageConsumer;

	DownloadJob(String name, SimpleArtifactRepository repository, LinkedList<IArtifactRequest> requestsPending,
			Consumer<IStatus> resultConsumer, Consumer<String> messageConsumer) {
		super(name);
		this.resultConsumer = resultConsumer;
		this.messageConsumer = messageConsumer;
		setSystem(true);
		this.repository = repository;
		this.requestsPending = requestsPending;
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == FAMILY;
	}

	@Override
	protected IStatus run(IProgressMonitor jobMonitor) {
		SubMonitor monitor = SubMonitor.convert(jobMonitor, Messages.DownloadJob_initial, 1_000_000);
		do {
			IArtifactRequest request;
			synchronized (requestsPending) {
				int totalDownloadWork = requestsPending.size();
				if (totalDownloadWork == 0) {
					return Status.OK_STATUS;
				}
				monitor.setWorkRemaining(totalDownloadWork * SUB_TICKS);
				request = requestsPending.removeFirst();
			}
			IArtifactKey key = request.getArtifactKey();
			String currentArtifact = String.format("%s %s", key.getId(), key.getVersion()); //$NON-NLS-1$
			monitor.setTaskName(currentArtifact);
			SubMonitor split = monitor.split(SUB_TICKS, SubMonitor.SUPPRESS_NONE);
			IStatus status = repository.getArtifact(request, new IProgressMonitor() {

				private volatile boolean canceled;
				private String taskName;
				private String subTaskName;
				private String lastMessage;

				@Override
				public void worked(int work) {
					split.worked(work);
				}

				@Override
				public void subTask(String name) {
					this.subTaskName = name;
					if (messageConsumer != null) {
						messageConsumer.accept(name);
					}
					updateTaskName();
				}

				@Override
				public void setTaskName(String name) {
					this.taskName = name;
					updateTaskName();
				}

				@Override
				public void setCanceled(boolean canceled) {
					this.canceled = canceled;
				}

				@Override
				public boolean isCanceled() {
					return canceled;
				}

				@Override
				public void internalWorked(double work) {
					split.internalWorked(work);
				}

				@Override
				public void done() {
					split.done();

				}

				@Override
				public void beginTask(String name, int totalWork) {
					monitor.beginTask(name, totalWork);
					this.taskName = name;
					updateTaskName();
				}

				private void updateTaskName() {
					StringBuilder sb = new StringBuilder();
					if (taskName != null && !taskName.isBlank()) {
						sb.append(taskName);
					}
					if (subTaskName != null && !subTaskName.isBlank()) {
						if (sb.length() > 0) {
							sb.append(" - "); //$NON-NLS-1$
						}
						sb.append(subTaskName);
					}
					String message = sb.toString();
					if (message.length() > 0 && !message.equals(lastMessage)) {
						lastMessage = message;
						monitor.subTask(message);
					}
				}
			});
			resultConsumer.accept(status);
		} while (!monitor.isCanceled());
		return Status.CANCEL_STATUS;
	}

}
