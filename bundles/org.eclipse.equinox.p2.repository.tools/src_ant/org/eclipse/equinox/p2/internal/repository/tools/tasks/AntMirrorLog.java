/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import org.eclipse.equinox.p2.internal.repository.mirroring.IArtifactMirrorLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;

public class AntMirrorLog implements IArtifactMirrorLog {

	private boolean consoleMessage = false;
	private Method log;
	private Object task;

	public AntMirrorLog(Object task) throws NoSuchMethodException {
		this.task = task;
		try {
			log = task.getClass().getMethod("log", new Class[] {String.class, int.class}); //$NON-NLS-1$
		} catch (SecurityException e) {
			exceptionOccurred(null, e);
		}
	}

	public void log(IArtifactDescriptor descriptor, IStatus status) {
		log(descriptor.toString(), status.getSeverity());
		log(status);
	}

	public void log(IStatus status) {
		int severity = status.getSeverity();
		// Log the status message
		log(status.getMessage(), severity);
		// Log the exception if applicable
		if (status.getException() != null)
			log(status.getException().getMessage(), severity);

		// Log any children of this status
		IStatus[] nestedStatus = status.getChildren();
		if (nestedStatus != null)
			for (int i = 0; i < nestedStatus.length; i++)
				log(nestedStatus[i]);
	}

	public void close() {
		// nothing to do here
	}

	/*
	 * Log a message to the Ant Task
	 */
	private void log(String message, int statusSeverity) {
		try {
			log.invoke(task, new Object[] {message, Integer.valueOf(mapLogLevels(statusSeverity))});
		} catch (IllegalArgumentException e) {
			exceptionOccurred(message, e);
		} catch (IllegalAccessException e) {
			exceptionOccurred(message, e);
		} catch (InvocationTargetException e) {
			exceptionOccurred(message, e);
		}
	}

	/*
	 * Show an error message if this the first time, and print status messages.
	 */
	private void exceptionOccurred(String message, Exception e) {
		if (!consoleMessage) {
			System.err.println(Messages.MirrorLog_Exception_Occurred);
			e.printStackTrace(System.err);
			System.err.println(Messages.MirrorLog_Console_Log);
			consoleMessage = true;
		}
		if (message != null)
			System.out.println(message);
	}

	/**
	 * Copied from AntLogAdapter in pde build.
	 */
	private int mapLogLevels(int iStatusLevel) {
		switch (iStatusLevel) {
			case IStatus.ERROR :
				return 0;
			case IStatus.OK :
				return 2;
			case IStatus.INFO :
				return 2;
			case IStatus.WARNING :
				return 1;
			default :
				return 1;
		}
	}
}
