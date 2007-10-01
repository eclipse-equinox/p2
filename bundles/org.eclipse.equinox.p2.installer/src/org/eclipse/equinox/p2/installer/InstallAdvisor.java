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
package org.eclipse.equinox.p2.installer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * The install advisor helps to make decisions during install, and is the conduit
 * for reporting progress and results back to an end user or log.
 */
public abstract class InstallAdvisor {
	/**
	 * Updates a local file system location in which to install. Returns <code>null</code>
	 * if no location could be determined
	 * @return The install location.
	 */
	public abstract String getInstallLocation(IInstallDescription description);

	public abstract IRunnableContext getRunnableContext();

	/**
	 * Reports some result information to the context.  The status may be
	 * information, warning, or an error.
	 */
	public abstract void reportStatus(IStatus status);

	/**
	 * Initializes the install advisor.  This method must be called before calling any 
	 * other methods on the advisor are called.  Subsequent invocations of this
	 * method are ignored.
	 */
	public abstract void start();

	/**
	 * Stops the install advisor. The advisor becomes invalid after it has been
	 * stopped; a stopped advisor cannot be restarted.
	 */
	public abstract void stop();
}
